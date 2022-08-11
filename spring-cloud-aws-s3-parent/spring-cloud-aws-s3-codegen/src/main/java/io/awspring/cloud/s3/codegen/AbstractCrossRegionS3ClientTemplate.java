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
package io.awspring.cloud.s3.codegen;

import java.util.function.Function;
import software.amazon.awssdk.services.s3.S3Client;

abstract class AbstractCrossRegionS3ClientTemplate implements S3Client {

	abstract <R> R executeInBucketRegion(String bucket, Function<S3Client, R> fn);

	abstract <R> R executeInDefaultRegion(Function<S3Client, R> fn);

	@Override
	public String serviceName() {
		return S3Client.SERVICE_NAME;
	}
}
