/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Unit tests for {@link S3Template}. Tests edge cases not tested by {@link S3TemplateIntegrationTests}.
 *
 * @author Maciej Walkowiak
 */
class S3TemplateTests {

	private final S3Client client = mock(S3Client.class);
	private final S3OutputStreamProvider s3OutputStreamProvider = mock(S3OutputStreamProvider.class,
			Answers.RETURNS_DEEP_STUBS);
	private final S3ObjectConverter s3ObjectConverter = mock(S3ObjectConverter.class);

	private final S3Presigner s3Presigner = mock(S3Presigner.class);

	private final S3Template s3Template = new S3Template(client, s3OutputStreamProvider, s3ObjectConverter,
			s3Presigner);

	@Test
	void throwsExceptionWhenUploadFails() throws IOException {
		when(s3OutputStreamProvider.create(any(), any(), any())).thenThrow(IOException.class);

		assertThatExceptionOfType(S3Exception.class).isThrownBy(() -> {
			try (InputStream is = new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8))) {
				s3Template.upload("bucket-name", "key-name", is);
			}
		}).satisfies(ex -> {
			assertThat(ex.getMessage())
					.isEqualTo("Failed to upload object with a key 'key-name' to bucket 'bucket-name'");
		});
	}

	@Test
	void throwsExceptionWhenReadFails() {
		when(client.getObject(ArgumentMatchers.<Consumer<GetObjectRequest.Builder>> any()))
				.thenThrow(RuntimeException.class);

		assertThatExceptionOfType(S3Exception.class).isThrownBy(() -> {
			s3Template.read("bucket-name", "key-name", String.class);
		}).satisfies(ex -> {
			assertThat(ex.getMessage())
					.isEqualTo("Failed to read object with a key 'key-name' from bucket 'bucket-name'");
		});
	}

	@Test
	void createsS3Resource() throws IOException {
		S3OutputStream outputStream = mock(S3OutputStream.class);
		when(s3OutputStreamProvider.create(eq("bucket"), eq("key"), any())).thenReturn(outputStream);

		S3Resource resource = s3Template.createResource("bucket", "key");

		assertThat(resource).isNotNull();
		assertThat(resource.getOutputStream()).isEqualTo(outputStream);
		assertThat(resource.getLocation().getBucket()).isEqualTo("bucket");
		assertThat(resource.getLocation().getObject()).isEqualTo("key");
	}

}
