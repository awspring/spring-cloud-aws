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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

/**
 * Unit tests for {@link TransferManagerS3OutputStream}.
 *
 * @author Anton Perez
 */
class TransferManagerS3OutputStreamTests {

	@Test
	void setsMd5hash() throws IOException {
		S3TransferManager s3TransferManager = mock(S3TransferManager.class, Answers.RETURNS_DEEP_STUBS);

		try (TransferManagerS3OutputStream transferManagerS3OutputStream = new TransferManagerS3OutputStream(
				new Location("bucket", "key"), s3TransferManager, null)) {
			transferManagerS3OutputStream.write("hello".getBytes(StandardCharsets.UTF_8));
		}

		ArgumentCaptor<UploadFileRequest> captor = ArgumentCaptor.forClass(UploadFileRequest.class);
		verify(s3TransferManager).uploadFile(captor.capture());

		assertThat(captor.getValue().putObjectRequest().contentMD5()).isNotNull();
	}

	@Test
	void throwsExceptionWhenUploadFails() throws IOException {
		S3TransferManager s3TransferManager = mock(S3TransferManager.class);
		when(s3TransferManager.uploadFile(any(UploadFileRequest.class))).thenThrow(S3Exception.class);

		try {
			try (TransferManagerS3OutputStream transferManagerS3OutputStream = new TransferManagerS3OutputStream(
					new Location("bucket", "key"), s3TransferManager, null)) {
				transferManagerS3OutputStream.write("hello".getBytes(StandardCharsets.UTF_8));
			}
			fail("UploadFailedException should be thrown");
		}
		catch (UploadFailedException e) {
			assertThat(e.getPath()).isNotNull();
		}
	}

}
