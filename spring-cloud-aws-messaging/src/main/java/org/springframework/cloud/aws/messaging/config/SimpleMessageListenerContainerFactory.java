/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.messaging.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.util.Assert;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleMessageListenerContainerFactory {

	private TaskExecutor taskExecutor;

	private Integer maxNumberOfMessages;

	private Integer visibilityTimeout;

	private Integer waitTimeOut;

	private boolean autoStartup = true;

	private AmazonSQSAsync amazonSqs;

	private QueueMessageHandler queueMessageHandler;

	private ResourceIdResolver resourceIdResolver;

	private DestinationResolvingMessageSendingOperations<?> sendToMessageTemplate;

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setMaxNumberOfMessages(Integer maxNumberOfMessages) {
		this.maxNumberOfMessages = maxNumberOfMessages;
	}

	public void setVisibilityTimeout(Integer visibilityTimeout) {
		this.visibilityTimeout = visibilityTimeout;
	}

	public void setWaitTimeOut(Integer waitTimeOut) {
		this.waitTimeOut = waitTimeOut;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setAmazonSqs(AmazonSQSAsync amazonSqs) {
		Assert.notNull(amazonSqs, "amazonSqs must not be null");
		this.amazonSqs = amazonSqs;
	}

	public AmazonSQS getAmazonSqs() {
		return this.amazonSqs;
	}

	public void setQueueMessageHandler(QueueMessageHandler messageHandler) {
		this.queueMessageHandler = messageHandler;
	}

	public QueueMessageHandler getQueueMessageHandler() {
		return this.queueMessageHandler;
	}

	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	public ResourceIdResolver getResourceIdResolver() {
		return this.resourceIdResolver;
	}

	public void setSendToMessageTemplate(DestinationResolvingMessageSendingOperations<?> sendToMessageTemplate) {
		this.sendToMessageTemplate = sendToMessageTemplate;
	}

	public DestinationResolvingMessageSendingOperations<?> getSendToMessageTemplate() {
		return this.sendToMessageTemplate;
	}

	public SimpleMessageListenerContainer createSimpleMessageListenerContainer() {
		Assert.notNull(this.amazonSqs, "amazonSqs must not be null");

		SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
		simpleMessageListenerContainer.setAmazonSqs(this.amazonSqs);
		simpleMessageListenerContainer.setAutoStartup(this.autoStartup);

		if (this.taskExecutor != null) {
			simpleMessageListenerContainer.setTaskExecutor(this.taskExecutor);
		}
		if (this.maxNumberOfMessages != null) {
			simpleMessageListenerContainer.setMaxNumberOfMessages(this.maxNumberOfMessages);
		}
		if (this.visibilityTimeout != null) {
			simpleMessageListenerContainer.setVisibilityTimeout(this.visibilityTimeout);
		}
		if (this.waitTimeOut != null) {
			simpleMessageListenerContainer.setWaitTimeOut(this.waitTimeOut);
		}
		if (this.resourceIdResolver != null) {
			simpleMessageListenerContainer.setResourceIdResolver(this.resourceIdResolver);
		}

		return simpleMessageListenerContainer;
	}
}
