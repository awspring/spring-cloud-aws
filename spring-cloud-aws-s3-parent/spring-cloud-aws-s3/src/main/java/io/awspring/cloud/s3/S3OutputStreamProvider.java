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
import java.io.OutputStream;
import org.springframework.lang.Nullable;

/**
 * Creates an {@link OutputStream} that writes data to S3.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public interface S3OutputStreamProvider {

	/**
	 * Creates an {@link OutputStream} that writes data to S3.
	 *
	 * @param bucket - the bucket name
	 * @param key - the object key
	 * @param metadata - object metadata, can be {@code null}
	 * @return the S3 output stream
	 * @throws IOException - when IO operation fails
	 */
	S3OutputStream create(String bucket, String key, @Nullable ObjectMetadata metadata) throws IOException;

	/**
	 * Creates an {@link OutputStream} that writes data to S3.
	 *
	 * @param location - the bucket location
	 * @param metadata - object metadata, can be {@code null}
	 * @return the S3 output stream
	 * @throws IOException - when IO operation fails
	 */
	default S3OutputStream create(Location location, @Nullable ObjectMetadata metadata) throws IOException {
		return create(location.getBucket(), location.getObject(), metadata);
	}

}
