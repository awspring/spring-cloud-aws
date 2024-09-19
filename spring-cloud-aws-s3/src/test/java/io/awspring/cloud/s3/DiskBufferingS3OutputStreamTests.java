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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Unit tests for {@link DiskBufferingS3OutputStream}.
 *
 * @author Maciej Walkowiak
 */
class DiskBufferingS3OutputStreamTests {

	@Test
	void setsMd5hash() throws IOException {
		S3Client s3Client = mock(S3Client.class);

		try (DiskBufferingS3OutputStream diskBufferingS3OutputStream = new DiskBufferingS3OutputStream(
				new Location("bucket", "key"), s3Client, null)) {
			diskBufferingS3OutputStream.write("hello".getBytes(StandardCharsets.UTF_8));
		}

		ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

		assertThat(captor.getValue().contentMD5()).isNotNull();
	}

	@Test
	void throwsExceptionWhenUploadFails() throws IOException {
		S3Client s3Client = mock(S3Client.class);
		when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(S3Exception.class);

		try {
			try (DiskBufferingS3OutputStream diskBufferingS3OutputStream = new DiskBufferingS3OutputStream(
					new Location("bucket", "key"), s3Client, null)) {
				diskBufferingS3OutputStream.write("hello".getBytes(StandardCharsets.UTF_8));
			}
			fail("UploadFailedException should be thrown");
		}
		catch (UploadFailedException e) {
			assertThat(e.getPath()).isNotNull();
		}
	}

	@Test
	void abortsWhenExplicitlyInvoked() throws IOException {
		S3Client s3Client = mock(S3Client.class);

		try (DiskBufferingS3OutputStream diskBufferingS3OutputStream = new DiskBufferingS3OutputStream(
			new Location("bucket", "key"), s3Client, null)) {
			diskBufferingS3OutputStream.write("hello".getBytes(StandardCharsets.UTF_8));
			diskBufferingS3OutputStream.abort();
		}

		verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
	}
}
