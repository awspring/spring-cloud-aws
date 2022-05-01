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

import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.UploadFileRequest;

import java.io.IOException;

/**
 * {@link S3OutputStream} implementation, that uses the TransferManager from AWS (in preview).
 *
 * Transfer manager has been announced here
 * https://aws.amazon.com/blogs/developer/introducing-amazon-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/
 *
 * @author Anton Perez
 * @since 3.0
 */
class TransferManagerS3OutputStream extends BaseTempFileS3OutputStream {

	private final S3TransferManager s3TransferManager;

	TransferManagerS3OutputStream(Location location, S3TransferManager s3TransferManager,
			@Nullable ObjectMetadata objectMetadata) throws IOException {
		this(location, s3TransferManager, objectMetadata, null);
	}

	TransferManagerS3OutputStream(Location location, S3TransferManager s3TransferManager,
			@Nullable ObjectMetadata objectMetadata, @Nullable S3ObjectContentTypeResolver contentTypeResolver)
			throws IOException {
		super(location, objectMetadata, contentTypeResolver);
		this.s3TransferManager = s3TransferManager;
	}

	@Override
	protected void upload(PutObjectRequest putObjectRequest) {
		s3TransferManager.uploadFile(UploadFileRequest.builder()
			.putObjectRequest(putObjectRequest)
			.source(file)
			.build()
		);
	}
}
