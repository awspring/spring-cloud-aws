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
import org.springframework.lang.Nullable;
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
class DiskBufferingS3OutputStream extends AbstractTempFileS3OutputStream {

	private final S3Client s3Client;

	DiskBufferingS3OutputStream(Location location, S3Client s3Client, @Nullable ObjectMetadata objectMetadata)
			throws IOException {
		this(location, s3Client, objectMetadata, null);
	}

	DiskBufferingS3OutputStream(Location location, S3Client client, @Nullable ObjectMetadata objectMetadata,
			@Nullable S3ObjectContentTypeResolver contentTypeResolver) throws IOException {
		super(location, objectMetadata, contentTypeResolver);
		this.s3Client = client;
	}

	@Override
	protected void upload(PutObjectRequest putObjectRequest) {
		s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
	}
}
