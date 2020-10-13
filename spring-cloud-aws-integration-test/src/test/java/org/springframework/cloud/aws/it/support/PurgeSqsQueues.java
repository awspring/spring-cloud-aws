/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.it.support;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;

/**
 * Helper class to use when running integration tests during development.
 *
 * Purges all SQS queus used during integration tests.
 *
 * @author Maciej Walkowiak
 */
public class PurgeSqsQueues {

	public static void main(String[] args) {
		AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

		sqs.listQueues().getQueueUrls().stream().filter(url -> url.contains("IntegrationTestStack"))
				.forEach(url -> sqs.purgeQueue(new PurgeQueueRequest(url)));
	}

}
