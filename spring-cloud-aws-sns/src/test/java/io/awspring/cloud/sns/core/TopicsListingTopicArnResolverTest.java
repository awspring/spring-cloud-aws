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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.Topic;

class TopicsListingTopicArnResolverTest {
	private final SnsClient snsClient = mock(SnsClient.class);

	private final TopicsListingTopicArnResolver resolver = new TopicsListingTopicArnResolver(snsClient);

	/**
	 * SNS topic ARN should be resolved by full topic name rather by just a substring. i.e. "topic1" should not be
	 * resolved to arn:aws:sns:eu-west-1:123456789012:topic11 but rather to arn:aws:sns:eu-west-1:123456789012:topic1
	 */
	@Test
	void shouldResolveArnBasedOnTopicName() {
		given(snsClient.listTopics()).willReturn(aResponseContainingArnsForTopicNames("topic11", "topic1"));

		Arn arn = resolver.resolveTopicArn("topic1");

		assertThat(arn.toString()).isEqualTo("arn:aws:sns:eu-west-1:123456789012:topic1");
	}

	private ListTopicsResponse aResponseContainingArnsForTopicNames(String... topicNames) {
		return ListTopicsResponse.builder().topics(stubTopics(topicNames)).build();
	}

	private List<Topic> stubTopics(String... topicNames) {
		return Arrays.stream(topicNames).map(this::createTopic).toList();
	}

	private Topic createTopic(String name) {
		return Topic.builder().topicArn("arn:aws:sns:eu-west-1:123456789012:" + name).build();
	}
}
