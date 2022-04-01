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
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Creates {@link DiskBufferingS3OutputStream}.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class DiskBufferingS3OutputStreamProvider implements S3OutputStreamProvider {

	private final S3Client s3Client;

	public DiskBufferingS3OutputStreamProvider(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	@Override
	public S3OutputStream create(String bucket, String key, @Nullable ObjectMetadata metadata) throws IOException {
		return new DiskBufferingS3OutputStream(bucket, key, s3Client, metadata);
	}

}
