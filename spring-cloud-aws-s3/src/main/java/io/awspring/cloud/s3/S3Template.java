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
import java.net.URL;
import java.time.Duration;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Higher level abstraction over {@link S3Client} providing methods for the most common use cases.
 *
 * @author Maciej Walkowiak
 * @author Ziemowit Stolarczyk
 * @since 3.0
 */
public class S3Template implements S3Operations {

	private final S3Client s3Client;

	private final S3OutputStreamProvider s3OutputStreamProvider;

	private final S3ObjectConverter s3ObjectConverter;

	private final S3Presigner s3Presigner;

	public S3Template(S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider,
			S3ObjectConverter s3ObjectConverter, S3Presigner s3Presigner) {
		Assert.notNull(s3Client, "s3Client is required");
		Assert.notNull(s3OutputStreamProvider, "s3OutputStreamProvider is required");
		Assert.notNull(s3ObjectConverter, "s3ObjectConverter is required");
		Assert.notNull(s3Presigner, "s3Presigner is required");
		this.s3Client = s3Client;
		this.s3OutputStreamProvider = s3OutputStreamProvider;
		this.s3ObjectConverter = s3ObjectConverter;
		this.s3Presigner = s3Presigner;
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
	public boolean bucketExists(String bucketName) {
		Assert.notNull(bucketName, "bucketName is required");
		try {
			s3Client.headBucket(request -> request.bucket(bucketName));
		}
		catch (NoSuchBucketException e) {
			return false;
		}
		return true;
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
	public boolean objectExists(String bucketName, String key) {
		Assert.notNull(bucketName, "bucketName is required");
		Assert.notNull(key, "key is required");
		try {
			s3Client.headObject(request -> request.bucket(bucketName).key(key));
		}
		catch (NoSuchBucketException | NoSuchKeyException e) {
			return false;
		}
		return true;
	}

	@Override
	public List<S3Resource> listAllObjects(String bucketName) {
		return listObjects(bucketName, "");
	}

	@Override
	public List<S3Resource> listObjects(String bucketName, String prefix) {
		Assert.notNull(bucketName, "bucketName is required");
		Assert.notNull(prefix, "prefix is required");

		final ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build();
		final ListObjectsV2Response response = s3Client.listObjectsV2(request);

		return response.contents().stream()
				.map(s3Object -> new S3Resource(bucketName, s3Object.key(), s3Client, s3OutputStreamProvider)).toList();
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
	public S3Resource createResource(String bucketName, String key) {
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

	@Override
	public URL createSignedGetURL(String bucketName, String key, Duration duration) {
		Assert.notNull(bucketName, "bucketName is required");
		Assert.notNull(key, "key is required");
		Assert.notNull(duration, "duration is required");

		GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(key).build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().getObjectRequest(getObjectRequest)
				.signatureDuration(duration).build();

		PresignedGetObjectRequest signedRequest = s3Presigner.presignGetObject(presignRequest);
		return signedRequest.url();
	}

	@Override
	public URL createSignedPutURL(String bucketName, String key, Duration duration, @Nullable ObjectMetadata metadata,
			@Nullable String contentType) {
		Assert.notNull(bucketName, "bucketName is required");
		Assert.notNull(key, "key is required");
		Assert.notNull(duration, "duration is required");

		PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder().bucket(bucketName).key(key);
		if (metadata != null) {
			metadata.apply(putObjectRequestBuilder);
		}
		if (contentType != null) {
			putObjectRequestBuilder.contentType(contentType);
		}
		PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder().putObjectRequest(putObjectRequest)
				.signatureDuration(duration).build();

		PresignedPutObjectRequest signedRequest = s3Presigner.presignPutObject(presignRequest);
		return signedRequest.url();
	}

}
