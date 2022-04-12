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
package io.awspring.cloud.samples.s3.infrastructure;

import java.util.Arrays;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;

public class InfrastructureStack extends Stack {

	public InfrastructureStack(final Construct scope, final String id) {
		this(scope, id, null);
	}

	public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
		super(scope, id, props);

		Bucket bucket1 = Bucket.Builder.create(this, "bucket1").bucketName("spring-cloud-aws-sample-bucket1").build();

		BucketDeployment.Builder.create(this, "bucketDeployment").destinationBucket(bucket1)
				.sources(Arrays.asList(Source.data("my-file.txt", "my file content"),
						Source.data("test-file.txt", "test file content")))
				.build();
	}

}
