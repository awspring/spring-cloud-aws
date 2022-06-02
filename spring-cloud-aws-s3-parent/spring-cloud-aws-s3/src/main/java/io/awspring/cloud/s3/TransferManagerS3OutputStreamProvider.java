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
import org.springframework.util.Assert;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Creates {@link TransferManagerS3OutputStream}.
 *
 * @author Anton Perez
 * @since 3.0
 */
public class TransferManagerS3OutputStreamProvider implements S3OutputStreamProvider {

	private final S3TransferManager s3TransferManager;
	@Nullable
	private final S3ObjectContentTypeResolver contentTypeResolver;

	public TransferManagerS3OutputStreamProvider(S3TransferManager s3TransferManager,
			@Nullable S3ObjectContentTypeResolver contentTypeResolver) {
		Assert.notNull(s3TransferManager, "s3TransferManager is required");
		this.s3TransferManager = s3TransferManager;
		this.contentTypeResolver = contentTypeResolver;
	}

	@Override
	public S3OutputStream create(String bucket, String key, @Nullable ObjectMetadata metadata) throws IOException {
		return new TransferManagerS3OutputStream(new Location(bucket, key, null), s3TransferManager, metadata,
				contentTypeResolver);
	}

}
