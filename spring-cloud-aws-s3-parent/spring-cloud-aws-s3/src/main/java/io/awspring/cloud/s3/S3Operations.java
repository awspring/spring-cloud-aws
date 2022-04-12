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
import org.springframework.core.io.Resource;

public interface S3Operations {

	String createBucket(String bucketName);

	void deleteBucket(String bucketName);

	void deleteObject(String bucketName, String key);

	void deleteObject(String s3Url);

	void store(String bucketName, String key, Object object);

	<T> T read(String bucketName, String key, Class<T> object);

	void upload(String bucketName, String key, InputStream inputStream) throws IOException;

	Resource download(String bucketName, String key) throws IOException;
}
