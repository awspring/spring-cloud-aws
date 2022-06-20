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

import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Higher level abstraction over {@link S3Client} providing methods for the most common use cases.
 *
 * @author Maciej Walkowiak
 */
public class S3Template implements S3Operations {

	private final S3Client s3Client;

	private final S3OutputStreamProvider s3OutputStreamProvider;

	private final S3ObjectConverter s3ObjectConverter;

	public S3Template(S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider,
			S3ObjectConverter s3ObjectConverter) {
		Assert.notNull(s3Client, "s3Client is required");
		Assert.notNull(s3OutputStreamProvider, "s3OutputStreamProvider is required");
		Assert.notNull(s3ObjectConverter, "s3ObjectConverter is required");
		this.s3Client = s3Client;
		this.s3OutputStreamProvider = s3OutputStreamProvider;
		this.s3ObjectConverter = s3ObjectConverter;
	}

	@Override
	public String createBucket(String bucketName) {
		Assert.notNull(bucketName, "bucketName is required");
		return s3Client.createBucket(request -> request.bucket(bucketName)).location();
	}

	@Override
	public void deleteBucket(String bucketName) {
		Assert.notNull(bucketName, "bucketName is required");
		s3Client.deleteBucket(request -> request.bucket(bucketName));
	}

	@Override
	public void deleteObject(String bucketName, String key) {
		Assert.notNull(bucketName, "bucketName is required");
		Assert.notNull(key, "key is required");
		s3Client.deleteObject(request -> request.bucket(bucketName).key(key));
	}

	@Override
	public void deleteObject(String s3Url) {
		Assert.notNull(s3Url, "s3Url is required");
		Location location = Location.of(s3Url);
		this.deleteObject(location.getBucket(), location.getObject());
	}

	@Override
	public S3Resource store(String bucketName, String key, Object object) {
		Assert.notNull(bucketName, "bucketName is required");
		Assert.notNull(key, "key is required");
		Assert.notNull(object, "object is required");

		PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder().bucket(bucketName).key(key)
				.contentType(s3ObjectConverter.contentType());
		s3Client.putObject(requestBuilder.build(), s3ObjectConverter.write(object));
		return new S3Resource(bucketName, key, s3Client, s3OutputStreamProvider);
	}

	@Override
	public <T> T read(String bucketName, String key, Class<T> clazz) {
		Assert.notNull(bucketName, "bucketName is required");
		Assert.notNull(key, "key is required");
		Assert.notNull(clazz, "clazz is required");

		try (InputStream is = s3Client.getObject(r -> r.bucket(bucketName).key(key))) {
			return s3ObjectConverter.read(is, clazz);
		}
		catch (Exception e) {
			throw new S3Exception(
					String.format("Failed to read object with a key '%s' from bucket '%s'", key, bucketName), e);
		}
	}

	@Override
	public S3Resource upload(String bucketName, String key, InputStream inputStream,
			@Nullable ObjectMetadata objectMetadata) {
		Assert.notNull(bucketName, "bucketName is required");
		Assert.notNull(key, "key is required");
		Assert.notNull(inputStream, "inputStream is required");

		S3Resource s3Resource = new S3Resource(bucketName, key, s3Client, s3OutputStreamProvider);
		if (objectMetadata != null) {
			s3Resource.setObjectMetadata(objectMetadata);
		}
		try (OutputStream os = s3Resource.getOutputStream()) {
			StreamUtils.copy(inputStream, os);
			return s3Resource;
		}
		catch (Exception e) {
			throw new S3Exception(
					String.format("Failed to upload object with a key '%s' to bucket '%s'", key, bucketName), e);
		}
	}

	@Override
	public S3Resource download(String bucketName, String key) {
		Assert.notNull(bucketName, "bucketName is required");
		Assert.notNull(key, "key is required");

		return new S3Resource(bucketName, key, s3Client, s3OutputStreamProvider);
	}

}
