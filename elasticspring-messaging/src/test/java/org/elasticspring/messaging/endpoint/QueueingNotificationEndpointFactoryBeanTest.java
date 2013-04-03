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

package org.elasticspring.messaging.endpoint;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import org.elasticspring.messaging.config.annotation.TopicListener;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.Collections;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueingNotificationEndpointFactoryBeanTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testCreateEndpoint() throws Exception {
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);
		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);

		String topicArn = "arn:aws:sns:eu-west:123456789012:test";
		Mockito.when(sns.listTopics(new ListTopicsRequest(null))).thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn(topicArn)));

		Mockito.when(sns.listSubscriptionsByTopic(new ListSubscriptionsByTopicRequest(topicArn))).thenReturn(
				new ListSubscriptionsByTopicResult().withSubscriptions(new Subscription().withProtocol("sqs").
						withTopicArn(topicArn).withEndpoint("arn:aws:sqs:eu-west:123456789012:myQueue")));

		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("myQueue"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://myQueue.aws.amazon.com"));
		Mockito.when(sqs.getQueueAttributes(new GetQueueAttributesRequest().withQueueUrl("http://myQueue.aws.amazon.com").withAttributeNames("QueueArn"))).
				thenReturn(new GetQueueAttributesResult().withAttributes(Collections.singletonMap("QueueArn", "arn:aws:sqs:eu-west:123456789012:myQueue")));

		QueueingNotificationEndpointFactoryBean factoryBean = new QueueingNotificationEndpointFactoryBean(sns, sqs, "test",
				TopicListener.NotificationProtocol.SQS, "myQueue", new Object(), "listenerMethod");

		factoryBean.afterPropertiesSet();
		Assert.assertNotNull(factoryBean.getObject());
	}

	@Test
	public void testWrongProtocol() throws Exception {
		this.expectedException.expectMessage("This endpoint only support sqs endpoints");
		this.expectedException.expect(IllegalArgumentException.class);
		AmazonSNS sns = Mockito.mock(AmazonSNS.class);
		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);
		//noinspection ResultOfObjectAllocationIgnored
		new QueueingNotificationEndpointFactoryBean(sns, sqs, "test",
				TopicListener.NotificationProtocol.E_MAIL_JSON, "myQueue", new Object(), "listenerMethod");

	}
}
