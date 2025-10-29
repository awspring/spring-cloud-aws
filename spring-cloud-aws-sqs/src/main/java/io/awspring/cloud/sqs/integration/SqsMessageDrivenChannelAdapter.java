/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.integration;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import java.util.Arrays;
import java.util.Collection;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * The {@link MessageProducerSupport} implementation for the Amazon SQS {@code receiveMessage}. Works in 'listener'
 * manner and delegates hard work to the {@link SqsMessageListenerContainer}.
 *
 * @author Artem Bilan
 * @author Patrick Fitzsimons
 *
 * @since 4.0
 *
 * @see SqsMessageListenerContainerFactory
 * @see MessageListener
 */
@ManagedResource
@IntegrationManagedResource
public class SqsMessageDrivenChannelAdapter extends MessageProducerSupport {

	private final SqsMessageListenerContainerFactory.Builder<Object> sqsMessageListenerContainerFactory = SqsMessageListenerContainerFactory
			.builder();

	private final String[] queues;

	private SqsContainerOptions sqsContainerOptions;

	private SqsMessageListenerContainer<?> listenerContainer;

	public SqsMessageDrivenChannelAdapter(SqsAsyncClient amazonSqs, String... queues) {
		Assert.noNullElements(queues, "'queues' must not be empty");
		this.sqsMessageListenerContainerFactory.sqsAsyncClient(amazonSqs);
		this.queues = Arrays.copyOf(queues, queues.length);
	}

	public void setSqsContainerOptions(SqsContainerOptions sqsContainerOptions) {
		this.sqsContainerOptions = sqsContainerOptions;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.sqsContainerOptions != null) {
			this.sqsMessageListenerContainerFactory.configure(sqsContainerOptionsBuilder -> sqsContainerOptionsBuilder
					.fromBuilder(this.sqsContainerOptions.toBuilder()));
		}
		this.sqsMessageListenerContainerFactory.messageListener(new IntegrationMessageListener());
		this.listenerContainer = this.sqsMessageListenerContainerFactory.build().createContainer(this.queues);
	}

	@Override
	public String getComponentType() {
		return "aws:sqs-message-driven-channel-adapter";
	}

	@Override
	protected void doStart() {
		this.listenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.listenerContainer.stop();
	}

	@ManagedAttribute
	public String[] getQueues() {
		return Arrays.copyOf(this.queues, this.queues.length);
	}

	private class IntegrationMessageListener implements MessageListener<Object> {

		IntegrationMessageListener() {
		}

		@Override
		public void onMessage(Message<Object> message) {
			sendMessage(message);
		}

		@Override
		public void onMessage(Collection<Message<Object>> messages) {
			onMessage(new GenericMessage<>(messages));
		}

	}

}
