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
package io.awspring.cloud.samples.infrastructure;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SqsQueue;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.amazon.awscdk.services.sqs.Queue;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.constructs.Construct;

public class InfrastructureStack extends Stack {

	public InfrastructureStack(final Construct scope, final String id) {
		this(scope, id, null);
	}

	public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
		super(scope, id, props);

		// S3
		Bucket.Builder.create(this, "bucket1").bucketName("spring-cloud-aws-sample-bucket1").build();

		// Parameter Store
		StringParameter.Builder.create(this, "Parameter").parameterName("/config/spring/message")
				.stringValue("Spring-cloud-aws value!").build();

		// Secrets Manager
		SecretStringGenerator secretStringGenerator = SecretStringGenerator.builder().generateStringKey("password")
				.secretStringTemplate("{}").build();

		Secret.Builder.create(this, "Secret").secretName("/secrets/spring-cloud-aws-sample-app")
				.generateSecretString(secretStringGenerator).build();

		SqsQueue.Builder.create(Queue.Builder.create(this, "test-queue").build()).build();
	}

}
