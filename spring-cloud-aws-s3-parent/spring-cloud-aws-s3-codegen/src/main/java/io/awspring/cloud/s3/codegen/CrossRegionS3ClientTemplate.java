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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseRequest;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseResponse;

public abstract class CrossRegionS3ClientTemplate implements S3Client {

	@Override
	public abstract ListBucketsResponse listBuckets(ListBucketsRequest request);

	@Override
	public abstract WriteGetObjectResponseResponse writeGetObjectResponse(WriteGetObjectResponseRequest request,
			RequestBody requestBody);

	abstract <R> R executeInBucketRegion(String bucket, Function<S3Client, R> fn);

	@Override
	public String serviceName() {
		return S3Client.SERVICE_NAME;
	}
}
