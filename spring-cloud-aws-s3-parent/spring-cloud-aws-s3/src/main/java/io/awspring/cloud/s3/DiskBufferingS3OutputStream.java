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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * {@link S3OutputStream} implementation, that first uploads to a local file in tmp folder and then flushes file to S3.
 *
 * Originally developed in
 * https://github.com/Alluxio/alluxio/blob/master/underfs/s3a/src/main/java/alluxio/underfs/s3a/S3AOutputStream.java and
 * adopted to Spring Cloud AWS needs.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
class DiskBufferingS3OutputStream extends S3OutputStream {

	private static final Logger LOG = LoggerFactory.getLogger(DiskBufferingS3OutputStream.class);

	/**
	 * Bucket name of the S3 bucket.
	 */
	private final String bucket;

	/**
	 * Key of the file when it is uploaded to S3.
	 */
	private final String key;

	/**
	 * The local file that will be uploaded when the stream is closed.
	 */
	private final File file;

	private final S3Client s3Client;

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

	DiskBufferingS3OutputStream(@NonNull String bucket, @NonNull String key, @NonNull S3Client client,
			@Nullable ObjectMetadata objectMetadata) throws IOException {
		Assert.notNull(bucket, "Bucket name must not be null.");
		this.bucket = bucket;
		this.key = key;
		this.s3Client = client;
		this.objectMetadata = objectMetadata;
		this.file = File.createTempFile("DiskBufferingS3OutputStream", UUID.randomUUID().toString());
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
			PutObjectRequest.Builder builder = PutObjectRequest.builder().bucket(bucket).key(key)
					.contentLength(file.length());
			if (objectMetadata != null) {
				objectMetadata.apply(builder);
			}
			if (hash != null) {
				String contentMD5 = new String(Base64.getEncoder().encode(hash.digest()));
				builder = builder.contentMD5(contentMD5);
			}
			s3Client.putObject(builder.build(), RequestBody.fromFile(file));
			file.delete();
		}
		catch (Exception se) {
			LOG.error("Failed to upload " + key + ". Temporary file @ " + file.getPath());
			throw new UploadFailedException(file.getPath(), se);
		}
	}

}
