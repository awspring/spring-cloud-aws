/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.v3.messaging.config;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.v3.core.env.ResourceIdResolver;
import io.awspring.cloud.v3.messaging.core.QueueMessagingTemplate;
import io.awspring.cloud.v3.messaging.listener.QueueMessageHandler;
import io.awspring.cloud.v3.messaging.listener.SendToHandlerMethodReturnValueHandler;
import io.awspring.cloud.v3.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @author Matej Nedic
 * @author Luis Duarte
 * @since 1.0
 */
public class QueueMessageHandlerFactory {

	private List<HandlerMethodArgumentResolver> argumentResolvers;

	private List<HandlerMethodReturnValueHandler> returnValueHandlers;

	private DestinationResolvingMessageSendingOperations<?> sendToMessagingTemplate;

	private SqsClient amazonSqs;

	private ResourceIdResolver resourceIdResolver;

	private SqsMessageDeletionPolicy sqsMessageDeletionPolicy;

	private BeanFactory beanFactory;

	private List<MessageConverter> messageConverters;

	private ObjectMapper objectMapper;

	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.argumentResolvers = argumentResolvers;
	}

	public void setReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.returnValueHandlers = returnValueHandlers;
	}

	/**
	 * Configures the {@link DestinationResolvingMessageSendingOperations} template used
	 * by the {@link SendToHandlerMethodReturnValueHandler} to send return values of
	 * handler methods.
	 * @param sendToMessagingTemplate A
	 * {@link DestinationResolvingMessageSendingOperations} template for sending return
	 * values of handler methods.
	 */
	public void setSendToMessagingTemplate(DestinationResolvingMessageSendingOperations<?> sendToMessagingTemplate) {
		this.sendToMessagingTemplate = sendToMessagingTemplate;
	}

	public SqsClient getAmazonSqs() {
		return this.amazonSqs;
	}

	/**
	 * <p>
	 * Sets the {@link SqsClient} client that is going to be used to create a new
	 * {@link QueueMessagingTemplate} if {@code sendToMessagingTemplate} is {@code null}.
	 * This template is used by the {@link SendToHandlerMethodReturnValueHandler} to send
	 * the return values of handler methods annotated with
	 * {@link org.springframework.messaging.handler.annotation.SendTo}.
	 * </p>
	 * <p>
	 * An {@link SqsClient} client is only needed if {@code sendToMessagingTemplate} is
	 * {@code null}.
	 * </p>
	 * @param amazonSqs The {@link SqsClient} client that is going to be used by the
	 * {@link SendToHandlerMethodReturnValueHandler} to send messages.
	 */
	public void setAmazonSqs(SqsClient amazonSqs) {
		this.amazonSqs = amazonSqs;
	}

	/**
	 * Configures global deletion Policy.
	 * @param sqsMessageDeletionPolicy if set it will use SqsMessageDeletionPolicy param
	 * as global default value only if SqsMessageDeletionPolicy is omitted
	 * from @SqsListener annotation. Should not be null.
	 */
	public void setSqsMessageDeletionPolicy(final SqsMessageDeletionPolicy sqsMessageDeletionPolicy) {
		this.sqsMessageDeletionPolicy = sqsMessageDeletionPolicy;
	}

	/**
	 * This value is only used if no {@code sendToMessagingTemplate} has been set.
	 * @param resourceIdResolver the resourceIdResolver to use for resolving logical to
	 * physical ids in a CloudFormation environment. This resolver will be used by the
	 * {@link QueueMessagingTemplate} created for the
	 * {@link SendToHandlerMethodReturnValueHandler}.
	 */
	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	/**
	 * Configures a {@link BeanFactory} that should be used to resolve expressions and
	 * placeholder for {@link org.springframework.messaging.handler.annotation.SendTo}
	 * annotations. If not set, then no expressions or place holders will be resolved.
	 * @param beanFactory - the bean factory used to resolve expressions and / or place
	 * holders
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * Configures an {@link ObjectMapper} that is used by default
	 * {@link MappingJackson2MessageConverter} created if no {@link #messageConverters}
	 * are set.
	 * @param objectMapper - object mapper, can be null
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public QueueMessageHandler createQueueMessageHandler() {
		QueueMessageHandler queueMessageHandler = new QueueMessageHandler(
				CollectionUtils.isEmpty(this.messageConverters)
						? Arrays.asList(getDefaultMappingJackson2MessageConverter(this.objectMapper))
						: this.messageConverters,
				this.sqsMessageDeletionPolicy);

		if (!CollectionUtils.isEmpty(this.argumentResolvers)) {
			queueMessageHandler.getCustomArgumentResolvers().addAll(this.argumentResolvers);
		}
		if (!CollectionUtils.isEmpty(this.returnValueHandlers)) {
			queueMessageHandler.getCustomReturnValueHandlers().addAll(this.returnValueHandlers);
		}

		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		if (this.sendToMessagingTemplate != null) {
			sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
					this.sendToMessagingTemplate);
		}
		else {
			sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
					getDefaultSendToQueueMessagingTemplate(this.amazonSqs, this.resourceIdResolver));

		}
		sendToHandlerMethodReturnValueHandler.setBeanFactory(this.beanFactory);
		queueMessageHandler.getCustomReturnValueHandlers().add(sendToHandlerMethodReturnValueHandler);

		return queueMessageHandler;
	}

	private QueueMessagingTemplate getDefaultSendToQueueMessagingTemplate(SqsClient amazonSqs,
																		  ResourceIdResolver resourceIdResolver) {
		return new QueueMessagingTemplate(amazonSqs, resourceIdResolver,
				getDefaultMappingJackson2MessageConverter(this.objectMapper));
	}

	public List<MessageConverter> getMessageConverters() {
		return this.messageConverters;
	}

	/**
	 * Configures a {@link MessageConverter}s that should be used to deserialize incoming
	 * message payloads and serialize messages in {@link QueueMessagingTemplate}. If not
	 * set, default {@link MappingJackson2MessageConverter} is used.
	 * @param messageConverters - the converters used for message conversion
	 */
	public void setMessageConverters(List<MessageConverter> messageConverters) {
		this.messageConverters = messageConverters;
	}

	private MappingJackson2MessageConverter getDefaultMappingJackson2MessageConverter(ObjectMapper objectMapper) {
		MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
		jacksonMessageConverter.setSerializedPayloadClass(String.class);
		jacksonMessageConverter.setStrictContentTypeMatch(true);

		if (objectMapper != null) {
			jacksonMessageConverter.setObjectMapper(objectMapper);
		}

		return jacksonMessageConverter;
	}

}
