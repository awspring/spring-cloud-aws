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

import static io.awspring.cloud.s3.InMemoryBufferingS3OutputStream.DEFAULT_BUFFER_CAPACITY;
import static io.awspring.cloud.s3.InMemoryBufferingS3OutputStream.DEFAULT_BUFFER_CAPACITY_IN_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * Tests for {@link InMemoryBufferingS3OutputStream}
 *
 * @author Sam Garfinkel
 */
class InMemoryBufferingS3OutputStreamTests {

	private final S3Client s3Client = mock(S3Client.class);

	@Test
	void writesWithPutObjectWhenBufferIsNotFull() throws IOException {
		final byte[] content = new byte[DEFAULT_BUFFER_CAPACITY_IN_BYTES - 1];

		try (InMemoryBufferingS3OutputStream outputStream = new InMemoryBufferingS3OutputStream(
				new Location("bucket", "key", null), s3Client, null, null, DEFAULT_BUFFER_CAPACITY)) {
			new Random().nextBytes(content);
			outputStream.write(content);
		}

		verify(s3Client, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
		verify(s3Client, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));

		final ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		final ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

		verify(s3Client, times(1)).putObject(requestCaptor.capture(), bodyCaptor.capture());

		assertThat(requestCaptor.getValue().bucket()).isEqualTo("bucket");
		assertThat(requestCaptor.getValue().key()).isEqualTo("key");
		assertThat(requestCaptor.getValue().contentLength()).isEqualTo(content.length);
		assertThat(requestCaptor.getValue().contentMD5()).isNotNull();

		assertThat(bodyCaptor.getValue().contentStreamProvider().newStream()).hasBinaryContent(content);
	}

	@Test
	void writesWithMultipartUploadWhenBufferIsFull() throws IOException {
		when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
				.thenReturn(CreateMultipartUploadResponse.builder().uploadId("uploadId").build());

		final String[] etags = new String[] { "etag-1", "etag-2" };
		when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class))).thenReturn(
				UploadPartResponse.builder().eTag(etags[0]).build(),
				UploadPartResponse.builder().eTag(etags[1]).build());

		final byte[] content = new byte[DEFAULT_BUFFER_CAPACITY_IN_BYTES + 1];

		try (InMemoryBufferingS3OutputStream outputStream = new InMemoryBufferingS3OutputStream(
				new Location("bucket", "key", null), s3Client, null, null, DEFAULT_BUFFER_CAPACITY)) {
			new Random().nextBytes(content);
			outputStream.write(content);
		}

		verify(s3Client, times(1)).createMultipartUpload(any(CreateMultipartUploadRequest.class));

		final ArgumentCaptor<UploadPartRequest> requestCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
		final ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

		// Check parts
		verify(s3Client, times(2)).uploadPart(requestCaptor.capture(), bodyCaptor.capture());

		assertThat(requestCaptor.getAllValues()).hasSize(2);
		assertThat(bodyCaptor.getAllValues()).hasSize(2);

		final UploadPartRequest firstRequest = requestCaptor.getAllValues().get(0);
		final RequestBody firstBody = bodyCaptor.getAllValues().get(0);

		assertThat(firstRequest.bucket()).isEqualTo("bucket");
		assertThat(firstRequest.key()).isEqualTo("key");
		assertThat(firstRequest.contentLength()).isEqualTo(DEFAULT_BUFFER_CAPACITY_IN_BYTES);
		assertThat(firstRequest.contentMD5()).isNotNull();
		assertThat(firstRequest.partNumber()).isEqualTo(1);

		assertThat(firstBody.contentStreamProvider().newStream())
				.hasBinaryContent(Arrays.copyOfRange(content, 0, DEFAULT_BUFFER_CAPACITY_IN_BYTES));

		final UploadPartRequest secondRequest = requestCaptor.getAllValues().get(1);
		final RequestBody secondBody = bodyCaptor.getAllValues().get(1);

		assertThat(secondRequest.bucket()).isEqualTo("bucket");
		assertThat(secondRequest.key()).isEqualTo("key");
		assertThat(secondRequest.contentLength()).isEqualTo(1);
		assertThat(secondRequest.contentMD5()).isNotNull();
		assertThat(secondRequest.partNumber()).isEqualTo(2);

		assertThat(secondBody.contentStreamProvider().newStream()).hasBinaryContent(
				Arrays.copyOfRange(content, DEFAULT_BUFFER_CAPACITY_IN_BYTES, DEFAULT_BUFFER_CAPACITY_IN_BYTES + 1));

		// Check complete
		final ArgumentCaptor<CompleteMultipartUploadRequest> completeUploadRequestCaptor = ArgumentCaptor
				.forClass(CompleteMultipartUploadRequest.class);

		verify(s3Client, times(1)).completeMultipartUpload(completeUploadRequestCaptor.capture());

		assertThat(completeUploadRequestCaptor.getValue().uploadId()).isEqualTo("uploadId");
		assertThat(completeUploadRequestCaptor.getValue().multipartUpload().parts()).satisfies(completedParts -> {
			assertThat(completedParts.get(0).partNumber()).isEqualTo(1);
			assertThat(completedParts.get(0).eTag()).isEqualTo(etags[0]);
			assertThat(completedParts.get(1).partNumber()).isEqualTo(2);
			assertThat(completedParts.get(1).eTag()).isEqualTo(etags[1]);
		});

		verify(s3Client, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
	}

	@Test
	void abortsWhenCompletingMultipartUploadFails() throws IOException {
		when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
				.thenReturn(CreateMultipartUploadResponse.builder().uploadId("uploadId").build());

		when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
				.thenReturn(UploadPartResponse.builder().build());

		when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
				.thenThrow(SdkException.builder().build());

		final byte[] content = new byte[DEFAULT_BUFFER_CAPACITY_IN_BYTES + 1];

		try {
			try (InMemoryBufferingS3OutputStream outputStream = new InMemoryBufferingS3OutputStream(
					new Location("bucket", "key", null), s3Client, null, null, DEFAULT_BUFFER_CAPACITY)) {
				new Random().nextBytes(content);
				outputStream.write(content);
			}
			fail("S3Exception should be thrown.");
		}
		catch (S3Exception e) {
			final ArgumentCaptor<AbortMultipartUploadRequest> requestCaptor = ArgumentCaptor
					.forClass(AbortMultipartUploadRequest.class);

			verify(s3Client, times(1)).abortMultipartUpload(requestCaptor.capture());
			assertThat(requestCaptor.getValue().bucket()).isEqualTo("bucket");
			assertThat(requestCaptor.getValue().key()).isEqualTo("key");
			assertThat(requestCaptor.getValue().uploadId()).isEqualTo("uploadId");
		}
	}

	@Test
	void abortsWhenExplicitlyInvoked() throws IOException {
		when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
				.thenReturn(CreateMultipartUploadResponse.builder().uploadId("uploadId").build());

		when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
				.thenReturn(UploadPartResponse.builder().build());

		when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
				.thenThrow(SdkException.builder().build());

		final byte[] content = new byte[DEFAULT_BUFFER_CAPACITY_IN_BYTES + 1];

		try (InMemoryBufferingS3OutputStream outputStream = new InMemoryBufferingS3OutputStream(
			new Location("bucket", "key", null), s3Client, null, null, DEFAULT_BUFFER_CAPACITY)) {
			new Random().nextBytes(content);
			outputStream.write(content);
			outputStream.abort();
		}
		final ArgumentCaptor<AbortMultipartUploadRequest> requestCaptor = ArgumentCaptor
				.forClass(AbortMultipartUploadRequest.class);

		verify(s3Client, times(1)).abortMultipartUpload(requestCaptor.capture());
		assertThat(requestCaptor.getValue().bucket()).isEqualTo("bucket");
		assertThat(requestCaptor.getValue().key()).isEqualTo("key");
		assertThat(requestCaptor.getValue().uploadId()).isEqualTo("uploadId");
	}

	@Test
	void abortsWhenInvokedBeforeWriting() {
		try (InMemoryBufferingS3OutputStream outputStream = new InMemoryBufferingS3OutputStream(
				new Location("bucket", "key", null), s3Client, null, null, DEFAULT_BUFFER_CAPACITY)) {
			outputStream.abort();
		}

		verify(s3Client, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
		verify(s3Client, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
	}

	@Test
	void failsWhenAbortingAfterClosing() {
		InMemoryBufferingS3OutputStream outputStream = null;
		try {
			outputStream = new InMemoryBufferingS3OutputStream(new Location("bucket", "key", null), s3Client, null,
					null, DEFAULT_BUFFER_CAPACITY);
		}
		finally {
			assertThat(outputStream).isNotNull();
			outputStream.close();
			try {
				outputStream.abort();
				fail("IllegalStateException should be thrown.");
			}
			catch (IllegalStateException e) {
				final ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
				final ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

				verify(s3Client, times(1)).putObject(requestCaptor.capture(), bodyCaptor.capture());

				assertThat(requestCaptor.getValue().bucket()).isEqualTo("bucket");
				assertThat(requestCaptor.getValue().key()).isEqualTo("key");
				assertThat(requestCaptor.getValue().contentLength()).isEqualTo(0);
				assertThat(requestCaptor.getValue().contentMD5()).isNotNull();

				verify(s3Client, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
				verify(s3Client, never()).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
			}
		}
	}
}
