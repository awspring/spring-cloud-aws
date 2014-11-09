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

package org.springframework.cloud.aws.messaging.config.annotation;

import com.amazonaws.services.sqs.AmazonSQS;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler;
import org.springframework.context.annotation.Bean;
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
 */
public class QueueMessageHandlerConfigurationSupport implements InitializingBean {

	private static final boolean JACKSON_2_PRESENT =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", QueueMessageHandlerConfigurationSupport.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", QueueMessageHandlerConfigurationSupport.class.getClassLoader());

	private AmazonSQS amazonSqs;

	private ResourceIdResolver resourceIdResolver;

	public void setAmazonSqs(AmazonSQS amazonSqs) {
		this.amazonSqs = amazonSqs;
	}

	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	@Bean
	public QueueMessageHandler queueMessageHandler() {
		List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();
		addArgumentResolvers(argumentResolvers);

		List<HandlerMethodReturnValueHandler> returnValueHandlers = new ArrayList<>();
		addReturnValueHandlers(returnValueHandlers);
		returnValueHandlers.add(getSendToHandlerMethodReturnValueHandler());

		QueueMessageHandler queueMessageHandler = new QueueMessageHandler();
		queueMessageHandler.setCustomArgumentResolvers(argumentResolvers);
		queueMessageHandler.setCustomReturnValueHandlers(returnValueHandlers);

		return queueMessageHandler;
	}

	private HandlerMethodReturnValueHandler getSendToHandlerMethodReturnValueHandler() {
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;

		if (getSendToQueueMessagingTemplate() != null) {
			sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(getSendToQueueMessagingTemplate());
		} else {
			sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(getDefaultSendToQueueMessagingTemplate());
		}

		return sendToHandlerMethodReturnValueHandler;
	}

	private QueueMessagingTemplate getDefaultSendToQueueMessagingTemplate() {
		QueueMessagingTemplate sendToQueueMessagingTemplate = new QueueMessagingTemplate(this.amazonSqs, this.resourceIdResolver);
		List<MessageConverter> messageConverters = new ArrayList<>(3);

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
		configureSendToQueueMessagingTemplate(sendToQueueMessagingTemplate);

		return sendToQueueMessagingTemplate;
	}

	/**
	 * Override this method to provide a custom {@link QueueMessagingTemplate}.
	 *
	 * @return a {@link QueueMessagingTemplate} that is used by the {@link SendToHandlerMethodReturnValueHandler}.
	 */
	protected QueueMessagingTemplate getSendToQueueMessagingTemplate() {
		return null;
	}

	/**
	 * Override this method to configure the default {@link QueueMessagingTemplate} that is used by the
	 * {@link SendToHandlerMethodReturnValueHandler}. This method is typically used e.g. when the {@link MessageConverter}s
	 * must be extended or modified.
	 *
	 * @param sendToQueueMessagingTemplate
	 * 		the pre-configured {@link QueueMessagingTemplate}.
	 */
	protected void configureSendToQueueMessagingTemplate(QueueMessagingTemplate sendToQueueMessagingTemplate) {
	}

	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
	}

	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.amazonSqs, "amazonSqs must not be null");
	}
}
