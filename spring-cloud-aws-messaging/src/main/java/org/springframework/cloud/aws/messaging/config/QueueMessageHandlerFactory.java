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
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author Alain Sahli
 * @since 1.0
 */
public class QueueMessageHandlerFactory {

	private List<HandlerMethodArgumentResolver> argumentResolvers;

	private List<HandlerMethodReturnValueHandler> returnValueHandlers;

	private DestinationResolvingMessageSendingOperations<?> sendToMessagingTemplate;

	private AmazonSQS amazonSqs;

	private ResourceIdResolver resourceIdResolver;

	public void setArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		this.argumentResolvers = argumentResolvers;
	}

	public void setReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		this.returnValueHandlers = returnValueHandlers;
	}

	/**
	 * Configures the {@link org.springframework.messaging.core.DestinationResolvingMessageSendingOperations} template
	 * used by the {@link org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler} to
	 * send return values of handler methods.
	 *
	 * @param sendToMessagingTemplate
	 * 		A {@link org.springframework.messaging.core.DestinationResolvingMessageSendingOperations} template for
	 * 		sending return values of handler methods.
	 */
	public void setSendToMessagingTemplate(DestinationResolvingMessageSendingOperations<?> sendToMessagingTemplate) {
		this.sendToMessagingTemplate = sendToMessagingTemplate;
	}

	/**
	 * <p>Sets the {@link com.amazonaws.services.sqs.AmazonSQS} client that is going to be used to create a new
	 * {@link org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate} if {@code sendToMessagingTemplate} is
	 * {@code null}. This template is used by the
	 * {@link org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler} to send the return
	 * values of handler methods annotated with {@link org.springframework.messaging.handler.annotation.SendTo}.</p>
	 * <p>An {@link com.amazonaws.services.sqs.AmazonSQS} client is only needed if {@code sendToMessagingTemplate} is
	 * {@code null}.</p>
	 *
	 * @param amazonSqs
	 * 		The {@link com.amazonaws.services.sqs.AmazonSQS} client that is going to be used by the
	 * 		{@link org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler} to send
	 * 		messages.
	 */
	public void setAmazonSqs(AmazonSQS amazonSqs) {
		this.amazonSqs = amazonSqs;
	}

	public AmazonSQS getAmazonSqs() {
		return this.amazonSqs;
	}

	/**
	 * This value is only used if no {@code sendToMessagingTemplate} has been set.
	 *
	 * @param resourceIdResolver
	 * 		the resourceIdResolver to use for resolving logical to physical ids in a CloudFormation environment. This
	 * 		resolver will be used by the {@link org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate}
	 * 		created for the {@link org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler}.
	 */
	public void setResourceIdResolver(ResourceIdResolver resourceIdResolver) {
		this.resourceIdResolver = resourceIdResolver;
	}

	public QueueMessageHandler createQueueMessageHandler() {
		QueueMessageHandler queueMessageHandler = new QueueMessageHandler();

		if (!CollectionUtils.isEmpty(this.argumentResolvers)) {
			queueMessageHandler.getCustomArgumentResolvers().addAll(this.argumentResolvers);
		}
		if (!CollectionUtils.isEmpty(this.returnValueHandlers)) {
			queueMessageHandler.getCustomReturnValueHandlers().addAll(this.returnValueHandlers);
		}
		if (this.sendToMessagingTemplate != null) {
			queueMessageHandler.getCustomReturnValueHandlers().add(new SendToHandlerMethodReturnValueHandler(this.sendToMessagingTemplate));
		} else {
			queueMessageHandler.getCustomReturnValueHandlers().add(new SendToHandlerMethodReturnValueHandler(
					getDefaultSendToQueueMessagingTemplate(this.amazonSqs, this.resourceIdResolver)));
		}

		return queueMessageHandler;
	}

	private QueueMessagingTemplate getDefaultSendToQueueMessagingTemplate(AmazonSQS amazonSqs, ResourceIdResolver resourceIdResolver) {
		return new QueueMessagingTemplate(amazonSqs, resourceIdResolver);
	}
}
