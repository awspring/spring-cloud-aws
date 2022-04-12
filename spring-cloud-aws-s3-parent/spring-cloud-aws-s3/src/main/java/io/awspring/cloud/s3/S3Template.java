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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3Template implements S3Operations {

	private final S3Client s3Client;

	private final S3OutputStreamProvider s3OutputStreamProvider;

	private final S3ObjectConverter s3ObjectConverter;

	public S3Template(S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider,
			S3ObjectConverter s3ObjectConverter) {
		this.s3Client = s3Client;
		this.s3OutputStreamProvider = s3OutputStreamProvider;
		this.s3ObjectConverter = s3ObjectConverter;
	}

	@Override
	public String createBucket(String bucketName) {
		return s3Client.createBucket(request -> request.bucket(bucketName)).location();
	}

	@Override
	public void deleteBucket(String bucketName) {
		s3Client.deleteBucket(request -> request.bucket(bucketName));
	}

	@Override
	public void deleteObject(String bucketName, String key) {
		s3Client.deleteObject(request -> request.bucket(bucketName).key(key));
	}

	@Override
	public void deleteObject(String s3Url) {
		Location location = Location.of(s3Url);
		this.deleteObject(location.getBucket(), location.getObject());
	}

	@Override
	public void store(String bucketName, String key, Object object) {
		s3Client.putObject(r -> r.bucket(bucketName).key(key),
				s3ObjectConverter.write(object));
	}

	@Override
	public <T> T read(String bucketName, String key, Class<T> clazz) {
		try (InputStream is = s3Client.getObject(r -> r.bucket(bucketName).key(key))) {
			return s3ObjectConverter.read(is, clazz);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void upload(String bucketName, String key, InputStream inputStream) throws IOException {
		S3Resource s3Resource = new S3Resource(bucketName, key, s3Client, s3OutputStreamProvider);
		try (OutputStream os = s3Resource.getOutputStream()) {
			StreamUtils.copy(inputStream, os);
		}

	}

	@Override
	public Resource download(String bucketName, String key) {
		return new S3Resource(bucketName, key, s3Client, s3OutputStreamProvider);
	}

}
