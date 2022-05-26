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

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

/**
 * {@link S3OutputStream} implementation that buffers content to an internal {@link ByteArrayOutputStream} and streams
 * the content as a MultiPartUpload as the buffer fills up. If the buffer's capacity is never exceeded, then an ordinary
 * {@link PutObjectRequest} is used for efficiency. This implementation is thread-safe, but makes no guarantees to
 * prevent data from interleaving. <br>
 * <br>
 * This stream <b>must</b> be closed to complete the upload.
 *
 * @author Sam Garfinkel
 * @since 3.0
 */
public class InMemoryBufferingS3OutputStream extends S3OutputStream {

	private static final Logger logger = LoggerFactory.getLogger(InMemoryBufferingS3OutputStream.class);

	public static final int DEFAULT_BUFFER_CAPACITY = 1024 * 1024 * 5;

	private final Location location;

	private final S3Client s3Client;

	@Nullable
	private final ObjectMetadata objectMetadata;

	@Nullable
	private final S3ObjectContentTypeResolver contentTypeResolver;

	private final int bufferSize;

	@Nullable
	private ByteArrayOutputStream outputStream;

	private final Object monitor = new Object();

	@Nullable
	private CreateMultipartUploadResponse multipartUploadResponse;

	private int partCounter = 1;

	private final List<CompletedPart> completedParts = new LinkedList<>();

	InMemoryBufferingS3OutputStream(Location location, S3Client s3Client, @Nullable ObjectMetadata objectMetadata,
			@Nullable S3ObjectContentTypeResolver contentTypeResolver, @Nullable Integer bufferSize) {
		this.location = location;
		this.s3Client = s3Client;
		this.objectMetadata = objectMetadata;
		this.contentTypeResolver = contentTypeResolver;
		this.bufferSize = computeBufferSize(bufferSize);
		this.outputStream = new ByteArrayOutputStream(this.bufferSize);
	}

	private static int computeBufferSize(@Nullable Integer bufferSize) {
		if (bufferSize != null) {
			if (bufferSize >= DEFAULT_BUFFER_CAPACITY) {
				return bufferSize;
			}
			else {
				logger.warn("Buffer size {} is less than the minimum {}. Using minimum instead.", bufferSize,
						DEFAULT_BUFFER_CAPACITY);
			}
		}
		return DEFAULT_BUFFER_CAPACITY;
	}

	@Override
	public void write(int b) {
		synchronized (this.monitor) {
			if (isClosed()) {
				return;
			}
			// Flush before writing to ensure that the MultiPartUpload is only initiated when the user is writing
			// at least bufferSize + 1 bytes.
			if (outputStream.size() == bufferSize) {
				// Create multipart upload if one isn't already in progress
				if (!isMultiPartUpload()) {
					createMultiPartUpload();
				}
				completedParts.add(uploadPart(outputStream.toByteArray()));
				outputStream.reset();
			}
			outputStream.write(b);
		}
	}

	@Override
	public void close() {
		synchronized (this.monitor) {
			if (isClosed()) {
				return;
			}
			if (isMultiPartUpload()) {
				completedParts.add(uploadPart(outputStream.toByteArray()));
				completeMultiPartUpload(multipartUploadResponse);
			}
			else {
				putObject(outputStream.toByteArray());
			}
			outputStream = null;
		}
	}

	private boolean isClosed() {
		return outputStream == null;
	}

	private boolean isMultiPartUpload() {
		return this.multipartUploadResponse != null;
	}

	private Optional<String> getHash(byte[] content) {
		try {
			return Optional.of(Base64.getEncoder().encodeToString(MessageDigest.getInstance("md5").digest(content)));
		}
		catch (NoSuchAlgorithmException e) {
			logger.warn("Algorithm not available for MD5 hash.", e);
			return Optional.empty();
		}
	}

	private void createMultiPartUpload() {
		try {
			this.multipartUploadResponse = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
					.bucket(location.getBucket()).key(location.getObject()).applyMutation(builder -> {
						if (objectMetadata != null) {
							objectMetadata.apply(builder);
						}
						if (contentTypeResolver != null
								&& (objectMetadata == null || objectMetadata.getContentType() == null)) {
							builder.contentType(contentTypeResolver.resolveContentType(location.getObject()));
						}
					}).build());
		}
		catch (SdkException e) {
			throw new S3Exception("Failed to create multipart upload.", e);
		}
	}

	private CompletedPart uploadPart(byte[] content) {
		final UploadPartResponse response = s3Client
				.uploadPart(UploadPartRequest.builder().bucket(location.getBucket()).key(location.getObject())
						.contentLength((long) content.length).partNumber(partCounter).applyMutation(builder -> {
							getHash(content).ifPresent(builder::contentMD5);
							if (objectMetadata != null) {
								objectMetadata.apply(builder);
							}
						}).build(), RequestBody.fromBytes(content));
		return CompletedPart.builder().partNumber(partCounter++).eTag(response.eTag()).build();
	}

	private void completeMultiPartUpload(CreateMultipartUploadResponse response) {
		try {
			s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder().bucket(location.getBucket())
					.key(location.getObject()).uploadId(response.uploadId())
					.multipartUpload(multipartUpload -> multipartUpload.parts(completedParts))
					.applyMutation(builder -> {
						if (objectMetadata != null) {
							objectMetadata.apply(builder);
						}
					}).build());
		}
		catch (SdkException e) {
			abortMultiPartUpload(response);
			throw new S3Exception("Multipart upload failed.", e);
		}
	}

	private void abortMultiPartUpload(CreateMultipartUploadResponse response) {
		try {
			s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket(location.getBucket())
					.key(location.getObject()).uploadId(response.uploadId()).build());
		}
		catch (SdkException e) {
			logger.error(
					"Failed to abort the upload with ID {}. The incomplete upload should be removed to avoid additional S3 charges.",
					response.uploadId());
			throw new S3Exception("Failed to abort the upload.", e);
		}
	}

	private void putObject(byte[] content) {
		try {
			s3Client.putObject(PutObjectRequest.builder().bucket(location.getBucket()).key(location.getObject())
					.contentLength((long) content.length).applyMutation(builder -> {
						getHash(content).ifPresent(builder::contentMD5);
						if (objectMetadata != null) {
							objectMetadata.apply(builder);
						}
					}).build(), RequestBody.fromBytes(content));
		}
		catch (SdkException e) {
			throw new S3Exception("Simple upload failed.", e);
		}
	}
}
