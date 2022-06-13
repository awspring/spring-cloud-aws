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
package io.awspring.cloud.sns.core;

import org.springframework.util.Assert;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;

/**
 * Default implementation of {@link TopicArnResolver} used to determine topic ARN by name.
 *
 * @author Matej Nedic
 */
public class DefaultTopicArnResolver implements TopicArnResolver {

	private final SnsClient snsClient;

	public DefaultTopicArnResolver(SnsClient snsClient) {
		this.snsClient = snsClient;
	}

	/**
	 * Resolves topic ARN by topic name. If topicName is already an ARN, it returns {@link Arn}. If topicName is just a
	 * string with a topic name, it attempts to create a topic or if topic already exists, just returns its ARN.
	 */
	@Override
	public Arn resolveTopicArn(String topicName) {
		Assert.notNull(topicName, "topicName must not be null");
		if (topicName.toLowerCase().startsWith("arn:")) {
			return Arn.fromString(topicName);
		}
		else {
			// if topic exists, createTopic returns successful response with topic arn
			return Arn.fromString(
					this.snsClient.createTopic(CreateTopicRequest.builder().name(topicName).build()).topicArn());
		}
	}

}
