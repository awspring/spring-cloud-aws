/*
 * Copyright 2013-2019 the original author or authors.
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

package io.awspring.cloud.v3.messaging.support.destination;

import io.awspring.cloud.v3.core.env.ResourceIdResolver;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.Topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @since 1.0
 */
class DynamicTopicDestinationResolverTest {

	// @checkstyle:off
	@Test
	void resolveDestination_withNonExistentTopicAndWithoutMarkerReturnedOnListTopics_shouldThrowIllegalArgumentException()
			throws Exception {
		// @checkstyle:on
		// Arrange
		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().build())).thenReturn(ListTopicsResponse.builder().build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);

		// Assert
		assertThatThrownBy(() -> resolver.resolveDestination("test")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("No topic found for name :'test'");
	}

	// @checkstyle:off
	@Test
	void resolveDestination_withNonExistentTopicAndWithMarkerReturnedOnListTopics_shouldCallListMultipleTimeWithMarkerAndThrowIllegalArgumentException()
			// @checkstyle:on
			throws Exception {
		// Arrange
		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().build()))
			.thenReturn(ListTopicsResponse.builder().nextToken("foo").build());
		when(sns.listTopics(ListTopicsRequest.builder().nextToken("foo").build()))
			.thenReturn(ListTopicsResponse.builder().build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);

		// Assert
		assertThatThrownBy(() -> resolver.resolveDestination("test")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("No topic found for name :'test'");
	}

	@Test
	void resolveDestination_withExistentTopic_returnsTopicArnFoundWhileListingTopic() throws Exception {
		// Arrange
		String topicArn = "arn:aws:sns:eu-west:123456789012:test";

		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().build()))
				.thenReturn(ListTopicsResponse.builder()
					.topics(Topic.builder()
						.topicArn(topicArn)
						.build())
					.build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	void resolveDestination_withExistentTopicAndMarker_returnsTopicArnFoundWhileListingTopic() throws Exception {
		// Arrange

		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().build()))
			.thenReturn(ListTopicsResponse.builder().nextToken("mark").build());

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		when(sns.listTopics(ListTopicsRequest.builder().nextToken("mark").build()))
				.thenReturn(ListTopicsResponse.builder().topics(Topic.builder().topicArn(topicArn).build()).build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	void resolveDestination_withAlreadyExistingArn_returnsArnWithoutValidatingIt() throws Exception {
		// Arrange
		String topicArn = "arn:aws:sns:eu-west:123456789012:test";

		SnsClient sns = mock(SnsClient.class);
		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);

		// Act
		String resolvedDestinationName = resolver.resolveDestination(topicArn);

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	void resolveDestination_withAutoCreateEnabled_shouldCreateTopicDirectly() throws Exception {
		// Arrange
		String topicArn = "arn:aws:sns:eu-west:123456789012:test";

		SnsClient sns = mock(SnsClient.class);
		when(sns.createTopic(CreateTopicRequest.builder().name("test").build()))
				.thenReturn(CreateTopicResponse.builder().topicArn(topicArn).build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);
		resolver.setAutoCreate(true);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	void resolveDestination_withResourceIdResolver_shouldCallIt() throws Exception {
		// Arrange
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:myTopic";
		String logicalTopicName = "myTopic";

		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(logicalTopicName)).thenReturn(physicalTopicName);

		SnsClient sns = mock(SnsClient.class);
		when(sns.listTopics(ListTopicsRequest.builder().build()))
				.thenReturn(ListTopicsResponse.builder()
					.topics(Topic.builder()
						.topicArn(physicalTopicName)
						.build())
					.build());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns, resourceIdResolver);

		// Assert
		String resolvedDestinationName = resolver.resolveDestination(logicalTopicName);

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(physicalTopicName);
	}

}
