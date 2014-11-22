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
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.annotation.SqsConfigurationSupport;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleMessageListenerContainerFactory {

	private static final boolean JACKSON_2_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", SqsConfigurationSupport.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", SqsConfigurationSupport.class.getClassLoader());

	private TaskExecutor taskExecutor;

	private Integer maxNumberOfMessages;

	private Integer visibilityTimeout;

	private Integer waitTimeOut;

	private boolean autoStartup = true;

	private AmazonSQS amazonSqs;

	private QueueMessageHandler messageHandler;

	private ResourceIdResolver resourceIdResolver;

	private final List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<>();

	private final List<HandlerMethodReturnValueHandler> customReturnValueHandlers = new ArrayList<>();

	public SimpleMessageListenerContainerFactory(AmazonSQS amazonSqs) {
		Assert.notNull(amazonSqs, "amazonSqs must not be null");
		this.amazonSqs = amazonSqs;
	}

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

	public void setAmazonSqs(AmazonSQS amazonSqs) {
		Assert.notNull(amazonSqs, "amazonSqs must not be null");
		this.amazonSqs = amazonSqs;
	}

	public AmazonSQS getAmazonSqs() {
		return this.amazonSqs;
	}

	public void setMessageHandler(QueueMessageHandler messageHandler) {
		this.messageHandler = messageHandler;
	}

	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	public ResourceIdResolver getResourceIdResolver() {
		return this.resourceIdResolver;
	}

	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
		return this.customReturnValueHandlers;
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
		if (this.messageHandler != null) {
			this.messageHandler.setCustomArgumentResolvers(this.customArgumentResolvers);
			this.messageHandler.setCustomReturnValueHandlers(this.customReturnValueHandlers);

			simpleMessageListenerContainer.setMessageHandler(this.messageHandler);
		} else {
			simpleMessageListenerContainer.setMessageHandler(getDefaultMessageHandler());
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

	private QueueMessageHandler getDefaultMessageHandler() {
		this.customReturnValueHandlers.add(new SendToHandlerMethodReturnValueHandler(getDefaultSendToQueueMessagingTemplate(this.amazonSqs, this.resourceIdResolver)));

		QueueMessageHandler queueMessageHandler = new QueueMessageHandler();
		queueMessageHandler.setCustomArgumentResolvers(this.customArgumentResolvers);
		queueMessageHandler.setCustomReturnValueHandlers(this.customReturnValueHandlers);

		return queueMessageHandler;
	}

	protected QueueMessagingTemplate getDefaultSendToQueueMessagingTemplate(AmazonSQS amazonSqs, ResourceIdResolver resourceIdResolver) {
		QueueMessagingTemplate sendToQueueMessagingTemplate = new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
		List<MessageConverter> messageConverters = new ArrayList<>(2);

		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);
		messageConverters.add(stringMessageConverter);

		if (JACKSON_2_PRESENT) {
			MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();
			mappingJackson2MessageConverter.setSerializedPayloadClass(String.class);
			messageConverters.add(mappingJackson2MessageConverter);
		}

		CompositeMessageConverter compositeMessageConverter = new CompositeMessageConverter(messageConverters);
		sendToQueueMessagingTemplate.setMessageConverter(compositeMessageConverter);

		return sendToQueueMessagingTemplate;
	}
}
