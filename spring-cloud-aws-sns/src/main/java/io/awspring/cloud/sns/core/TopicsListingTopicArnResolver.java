/*
 * Copyright 2013-2023 the original author or authors.
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

import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.Topic;

/**
 * Basic implementation for resolving ARN from topicName. It is listing all topics using {@link SnsClient#listTopics()}
 * to determine topicArn for topicName.
 *
 * @author Matej Nedić
 * @since 3.0.0
 */
public class TopicsListingTopicArnResolver implements TopicArnResolver {

	private final SnsClient snsClient;

	public TopicsListingTopicArnResolver(SnsClient snsClient) {
		Assert.notNull(snsClient, "SnsClient cannot be null!");
		this.snsClient = snsClient;
	}

	/**
	 * Resolves topic ARN by topic name. If topicName is already an ARN, it returns {@link Arn}. If topicName has value
	 * topic name of type String it will List all topics inside
	 */
	@Override
	public Arn resolveTopicArn(String topicName) {
		Assert.notNull(topicName, "topicName must not be null");
		if (topicName.toLowerCase().startsWith("arn:")) {
			return Arn.fromString(topicName);
		}
		return resolveTopicArnBySnsCall(topicName);
	}

	private Arn resolveTopicArnBySnsCall(String topicName) {
		ListTopicsResponse listTopicsResponse = snsClient.listTopics();
		return checkIfArnIsInList(topicName, listTopicsResponse);
	}

	private Arn doRecursiveCall(@Nullable String token, String topicName) {
		if (token != null) {
			ListTopicsResponse topicsResponse = snsClient
					.listTopics(ListTopicsRequest.builder().nextToken(token).build());
			return checkIfArnIsInList(topicName, topicsResponse);
		}
		else {
			throw new TopicNotFoundException("Topic does not exist for given topic name!");
		}
	}

	private Arn checkIfArnIsInList(String topicName, ListTopicsResponse listTopicsResponse) {
		Optional<String> arn = listTopicsResponse.topics().stream().map(Topic::topicArn)
				.filter(ta -> ta.endsWith(":" + topicName)).findFirst();
		if (arn.isPresent()) {
			return Arn.fromString(arn.get());
		}
		else {
			return doRecursiveCall(listTopicsResponse.nextToken(), topicName);
		}
	}

}
