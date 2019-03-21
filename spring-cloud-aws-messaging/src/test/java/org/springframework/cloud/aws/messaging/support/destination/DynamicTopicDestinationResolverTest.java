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

package org.springframework.cloud.aws.messaging.support.destination;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicTopicDestinationResolverTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	// @checkstyle:off
	@Test
	public void resolveDestination_withNonExistentTopicAndWithoutMarkerReturnedOnListTopics_shouldThrowIllegalArgumentException()
			throws Exception {
		// @checkstyle:on
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No topic found for name :'test'");

		AmazonSNS sns = mock(AmazonSNS.class);
		when(sns.listTopics(new ListTopicsRequest(null)))
				.thenReturn(new ListTopicsResult());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		resolver.resolveDestination("test");
	}

	// @checkstyle:off
	@Test
	public void resolveDestination_withNonExistentTopicAndWithMarkerReturnedOnListTopics_shouldCallListMultipleTimeWithMarkerAndThrowIllegalArgumentException()
			// @checkstyle:on
			throws Exception {
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No topic found for name :'test'");

		AmazonSNS sns = mock(AmazonSNS.class);
		when(sns.listTopics(new ListTopicsRequest(null)))
				.thenReturn(new ListTopicsResult().withNextToken("foo"));
		when(sns.listTopics(new ListTopicsRequest("foo")))
				.thenReturn(new ListTopicsResult());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		resolver.resolveDestination("test");
	}

	@Test
	public void resolveDestination_withExistentTopic_returnsTopicArnFoundWhileListingTopic()
			throws Exception {
		// Arrange
		String topicArn = "arn:aws:sns:eu-west:123456789012:test";

		AmazonSNS sns = mock(AmazonSNS.class);
		when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(
				new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	public void resolveDestination_withExistentTopicAndMarker_returnsTopicArnFoundWhileListingTopic()
			throws Exception {
		// Arrange

		AmazonSNS sns = mock(AmazonSNS.class);
		when(sns.listTopics(new ListTopicsRequest(null)))
				.thenReturn(new ListTopicsResult().withNextToken("mark"));

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		when(sns.listTopics(new ListTopicsRequest("mark"))).thenReturn(
				new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	public void resolveDestination_withAlreadyExistingArn_returnsArnWithoutValidatingIt()
			throws Exception {
		// Arrange
		String topicArn = "arn:aws:sns:eu-west:123456789012:test";

		AmazonSNS sns = mock(AmazonSNS.class);
		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);

		// Act
		String resolvedDestinationName = resolver.resolveDestination(topicArn);

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	public void resolveDestination_withAutoCreateEnabled_shouldCreateTopicDirectly()
			throws Exception {
		// Arrange
		String topicArn = "arn:aws:sns:eu-west:123456789012:test";

		AmazonSNS sns = mock(AmazonSNS.class);
		when(sns.createTopic(new CreateTopicRequest("test")))
				.thenReturn(new CreateTopicResult().withTopicArn(topicArn));

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns);
		resolver.setAutoCreate(true);

		// Act
		String resolvedDestinationName = resolver.resolveDestination("test");

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(topicArn);
	}

	@Test
	public void resolveDestination_withResourceIdResolver_shouldCallIt()
			throws Exception {
		// Arrange
		String physicalTopicName = "arn:aws:sns:eu-west:123456789012:myTopic";
		String logicalTopicName = "myTopic";

		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId(logicalTopicName))
				.thenReturn(physicalTopicName);

		AmazonSNS sns = mock(AmazonSNS.class);
		when(sns.listTopics(new ListTopicsRequest(null)))
				.thenReturn(new ListTopicsResult()
						.withTopics(new Topic().withTopicArn(physicalTopicName)));

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(
				sns, resourceIdResolver);

		// Assert
		String resolvedDestinationName = resolver.resolveDestination(logicalTopicName);

		// Assert
		assertThat(resolvedDestinationName).isEqualTo(physicalTopicName);
	}

}
