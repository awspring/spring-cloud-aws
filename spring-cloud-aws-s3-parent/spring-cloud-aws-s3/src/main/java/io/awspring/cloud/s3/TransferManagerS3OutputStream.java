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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.UploadFileRequest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * {@link S3OutputStream} implementation, that uses the TransferManager from AWS (in preview).
 *
 * Transfer manager has been announced here
 * https://aws.amazon.com/blogs/developer/introducing-amazon-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/
 *
 * @author Anton Perez
 * @since 3.0
 */
class TransferManagerS3OutputStream extends S3OutputStream {

	private static final Logger LOG = LoggerFactory.getLogger(TransferManagerS3OutputStream.class);

	private final Location location;

	/**
	 * The local file that will be uploaded when the stream is closed.
	 */
	private final File file;

	private final S3TransferManager s3TransferManager;

	/**
	 * The outputstream to a local file where the file will be buffered until closed.
	 */
	private OutputStream localOutputStream;

	/**
	 * The MD5 hash of the file.
	 */
	@Nullable
	private MessageDigest hash;

	@Nullable
	private final ObjectMetadata objectMetadata;

	/**
	 * Flag to indicate this stream has been closed, to ensure close is only done once.
	 */
	private boolean closed;

	@Nullable
	private final S3ObjectContentTypeResolver contentTypeResolver;

	TransferManagerS3OutputStream(Location location, S3TransferManager s3TransferManager,
			@Nullable ObjectMetadata objectMetadata) throws IOException {
		this(location, s3TransferManager, objectMetadata, null);
	}

	TransferManagerS3OutputStream(Location location, S3TransferManager s3TransferManager,
			@Nullable ObjectMetadata objectMetadata, @Nullable S3ObjectContentTypeResolver contentTypeResolver)
			throws IOException {
		Assert.notNull(location, "Location must not be null.");
		this.location = location;
		this.s3TransferManager = s3TransferManager;
		this.objectMetadata = objectMetadata;
		this.contentTypeResolver = contentTypeResolver;
		this.file = File.createTempFile("TransferManagerS3OutputStream", UUID.randomUUID().toString());
		try {
			hash = MessageDigest.getInstance("MD5");
			localOutputStream = new BufferedOutputStream(new DigestOutputStream(new FileOutputStream(file), hash));
		}
		catch (NoSuchAlgorithmException e) {
			LOG.warn("Algorithm not available for MD5 hash.", e);
			hash = null;
			localOutputStream = new BufferedOutputStream(new FileOutputStream(file));
		}
		closed = false;
	}

	@Override
	public void write(int b) throws IOException {
		localOutputStream.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		localOutputStream.write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		localOutputStream.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		localOutputStream.flush();
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		localOutputStream.close();
		closed = true;
		try {
			PutObjectRequest.Builder builder = PutObjectRequest.builder().bucket(location.getBucket())
					.key(location.getObject()).contentLength(file.length());
			if (objectMetadata != null) {
				objectMetadata.apply(builder);
			}
			if (hash != null) {
				String contentMD5 = new String(Base64.getEncoder().encode(hash.digest()));
				builder = builder.contentMD5(contentMD5);
			}
			if (contentTypeResolver != null && (objectMetadata == null || objectMetadata.getContentType() == null)) {
				String contentType = contentTypeResolver.resolveContentType(location.getObject());
				if (contentType != null) {
					builder.contentType(contentType);
				}
			}
			PutObjectRequest putObjectRequest = builder.build();
			s3TransferManager.uploadFile(UploadFileRequest.builder().putObjectRequest(putObjectRequest).build());
			file.delete();
		}
		catch (Exception se) {
			LOG.error(String.format("Failed to upload %s. Temporary file @%s", location.getObject(), file.getPath()));
			throw new UploadFailedException(file.getPath(), se);
		}
	}

}
