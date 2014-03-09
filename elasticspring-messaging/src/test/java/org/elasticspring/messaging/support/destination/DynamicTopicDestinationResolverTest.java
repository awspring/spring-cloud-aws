/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.support.destination;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Topic;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class DynamicTopicDestinationResolverTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testTopicDoesNotExist() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No topic found for name :'test'");
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);
		resolver.resolveDestination("test");
	}

	@Test
	public void testTopicDoesNotExistWithMarker() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("No topic found for name :'test'");
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withNextToken("foo"));
		Mockito.when(sns.listTopics(new ListTopicsRequest("foo"))).thenReturn(new ListTopicsResult());

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);
		resolver.resolveDestination("test");
	}

	@Test
	public void testTopicNameFoundInFirstRun() throws Exception {
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);
		Assert.assertEquals(topicArn, resolver.resolveDestination("test"));
	}

	@Test
	public void testTopicNameFoundInSecondRun() throws Exception {
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withNextToken("mark"));

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		Mockito.when(sns.listTopics(new ListTopicsRequest("mark"))).thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);
		Assert.assertEquals(topicArn, resolver.resolveDestination(topicArn));
	}

	@Test
	public void testWithAlreadyExistingArn() throws Exception {
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);
		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		Assert.assertEquals(topicArn, resolver.resolveDestination(topicArn));
	}

	@Test
	public void testWithAutoCreate() throws Exception {
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		Mockito.when(sns.createTopic(new CreateTopicRequest("test"))).thenReturn(new CreateTopicResult().withTopicArn(topicArn));

		DynamicTopicDestinationResolver resolver = new DynamicTopicDestinationResolver(sns);
		resolver.setAutoCreate(true);
		Assert.assertEquals(topicArn, resolver.resolveDestination("test"));
	}
}
