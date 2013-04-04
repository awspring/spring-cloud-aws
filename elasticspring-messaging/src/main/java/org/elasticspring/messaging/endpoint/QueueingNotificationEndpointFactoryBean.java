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
import org.elasticspring.core.naming.AmazonResourceName;
import org.elasticspring.messaging.config.annotation.TopicListener;
import org.elasticspring.messaging.listener.MessageListenerAdapter;
import org.elasticspring.messaging.listener.SimpleMessageListenerContainer;
import org.elasticspring.messaging.support.converter.NotificationMessageConverter;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicQueueDestinationResolver;
import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueingNotificationEndpointFactoryBean extends AbstractNotificationEndpointFactoryBean<SimpleMessageListenerContainer> implements SmartLifecycle {

	private final AmazonSQSAsync amazonSqs;
	private final NotificationMessageConverter messageConverter = new NotificationMessageConverter();
	private final DestinationResolver destinationResolver;
	private SimpleMessageListenerContainer container;

	public QueueingNotificationEndpointFactoryBean(AmazonSNS amazonSns, AmazonSQSAsync amazonSqs, String topicName,
												   TopicListener.NotificationProtocol protocol, String endpoint, Object target, String method) {
		super(amazonSns, topicName, protocol, endpoint, target, method);
		Assert.notNull(amazonSqs, "amazonSqs must not be null");
		Assert.isTrue(protocol == TopicListener.NotificationProtocol.SQS, "This endpoint only support sqs endpoints");
		this.amazonSqs = amazonSqs;
		this.destinationResolver = new CachingDestinationResolver(new DynamicQueueDestinationResolver(amazonSqs));
	}

	@Override
	protected SimpleMessageListenerContainer doCreateEndpointInstance(Subscription subscription) {
		this.container = new SimpleMessageListenerContainer();
		this.container.setAmazonSqs(this.amazonSqs);

		MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(this.messageConverter, getTarget(), getMethod());
		this.container.setMessageListener(messageListenerAdapter);
		this.container.setDestinationName(AmazonResourceName.fromString(subscription.getEndpoint()).getResourceType());
		return this.container;
	}

	@Override
	public Class<SimpleMessageListenerContainer> getObjectType() {
		return SimpleMessageListenerContainer.class;
	}

	@Override
	public String getEndpoint() {
		String queueUrl = this.destinationResolver.resolveDestinationName(super.getEndpoint());
		return this.amazonSqs.getQueueAttributes(new GetQueueAttributesRequest(queueUrl).withAttributeNames("QueueArn")).getAttributes().get("QueueArn");
	}

	@Override
	public boolean isAutoStartup() {
		return this.container.isAutoStartup();
	}

	@Override
	public void stop(Runnable callback) {
		this.container.stop(callback);
	}

	@Override
	public void start() {
		this.container.start();
	}

	@Override
	public void stop() {
		this.container.stop();
	}

	@Override
	public boolean isRunning() {
		return this.container.isRunning();
	}

	@Override
	public int getPhase() {
		return this.container.getPhase();
	}
}