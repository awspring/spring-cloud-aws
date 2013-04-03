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
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import org.elasticspring.core.naming.AmazonResourceName;
import org.elasticspring.messaging.config.annotation.TopicListener;
import org.elasticspring.messaging.listener.MessageListenerAdapter;
import org.elasticspring.messaging.listener.SimpleMessageListenerContainer;
import org.elasticspring.messaging.support.converter.NotificationMessageConverter;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueingNotificationEndpointFactoryBean extends AbstractNotificationEndpointFactoryBean<SimpleMessageListenerContainer> {

	private final AmazonSQSAsync amazonSqs;
	private final NotificationMessageConverter messageConverter = new NotificationMessageConverter();

	public QueueingNotificationEndpointFactoryBean(AmazonSNS amazonSns, AmazonSQSAsync amazonSqs, String topicName,
												   TopicListener.NotificationProtocol protocol, String endpoint, Object target, String method) {
		super(amazonSns, topicName, protocol, endpoint, target, method);
		this.amazonSqs = amazonSqs;
		Assert.notNull(amazonSqs, "amazonSqs must not be null");
		Assert.isTrue(protocol == TopicListener.NotificationProtocol.SQS, "This endpoint only support sqs endpoints");
	}

	@Override
	protected SimpleMessageListenerContainer doCreateEndpointInstance(Subscription subscription) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setAmazonSqs(this.amazonSqs);

		MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(this.messageConverter, getTarget(), getMethod());
		container.setMessageListener(messageListenerAdapter);
		container.setDestinationName(AmazonResourceName.fromString(subscription.getEndpoint()).getResourceType());
		return container;
	}

	@Override
	public Class<SimpleMessageListenerContainer> getObjectType() {
		return SimpleMessageListenerContainer.class;
	}

	@Override
	public String getEndpoint() {
		String queueUrl = this.amazonSqs.getQueueUrl(new GetQueueUrlRequest(super.getEndpoint())).getQueueUrl();
		return this.amazonSqs.getQueueAttributes(new GetQueueAttributesRequest(queueUrl).withAttributeNames("QueueArn")).getAttributes().get("QueueArn");
	}
}