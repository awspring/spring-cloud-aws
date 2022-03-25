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

import io.awspring.cloud.core.resource.AmazonResourceName;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class DynamicTopicDestinationResolver implements DestinationResolver<String> {

	private final SnsClient snsClient;

	private boolean autoCreate;

	public DynamicTopicDestinationResolver(SnsClient snsClient) {
		this.snsClient = snsClient;
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	@Override
	public String resolveDestination(String name) throws DestinationResolutionException {
		Assert.notNull(name, "Name must not be null");
		if (this.autoCreate) {
			return this.snsClient.createTopic(CreateTopicRequest.builder().name(name).build()).topicArn();
		}
		else {
			if (AmazonResourceName.isValidAmazonResourceName(name)) {
				return name;
			}
			String topicArn = getTopicResourceName(null, name);
			if (topicArn == null) {
				throw new IllegalArgumentException("No Topic with name: '" + name + "' found. Please use "
						+ "the right topic name or enable auto creation of topics for this DestinationResolver");
			}
			return topicArn;
		}
	}

	private String getTopicResourceName(String nextToken, String topicName) {
		ListTopicsResponse listTopicsResult = this.snsClient
				.listTopics(ListTopicsRequest.builder().nextToken(nextToken).build());
		for (Topic topic : listTopicsResult.topics()) {
			AmazonResourceName resourceName = AmazonResourceName.fromString(topic.topicArn());
			if (resourceName.getResourceType().equals(topicName)) {
				return topic.topicArn();
			}
		}

		if (StringUtils.hasText(listTopicsResult.nextToken())) {
			return getTopicResourceName(listTopicsResult.nextToken(), topicName);
		}
		else {
			throw new IllegalArgumentException("No topic found for name :'" + topicName + "'");
		}
	}

}
