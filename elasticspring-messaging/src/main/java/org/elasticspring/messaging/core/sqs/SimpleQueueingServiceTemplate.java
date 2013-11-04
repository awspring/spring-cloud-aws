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

package org.elasticspring.messaging.core.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.elasticspring.messaging.core.QueueingOperations;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicQueueDestinationResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.converter.MessageConverter;
import org.springframework.messaging.support.converter.SimpleMessageConverter;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link QueueingOperations} interface using the {@link org.springframework.messaging.support.converter.MessageConverter} and {@link
 * DestinationResolver} as collaborators to send and receive the messages. This class uses the {@link AmazonSQS}
 * instance to actually interact with the Amazon SQS service.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class SimpleQueueingServiceTemplate implements QueueingOperations {

	/**
	 * {@link AmazonSQS} client used by the instance
	 */
	private final AmazonSQS amazonSqs;

	/**
	 * {@link DestinationResolver} used by the instance. By default a {@link DynamicQueueDestinationResolver}
	 */
	private DestinationResolver destinationResolver;

	/**
	 * {@link org.springframework.messaging.support.converter.MessageConverter} used by the instance. By default a {@link SimpleMessageConverter}
	 */
	private MessageConverter messageConverter = new SimpleMessageConverter();

	/**
	 * The default destination name for operations that don't provide a destination name
	 */
	private String defaultDestinationName;

	/**
	 * Constructs an instance of this class with the mandatory {@link AmazonSQS} instance. This
	 * constructor will also
	 * create {@link DynamicQueueDestinationResolver} which is wrapped with
	 * a {@link CachingDestinationResolver} to
	 * improve
	 * the performance of the destination resolving process.
	 *
	 * @param amazonSqs
	 * 		- the Amazon SQS client used by the instance, must not be null.
	 */
	public SimpleQueueingServiceTemplate(AmazonSQS amazonSqs) {
		this.amazonSqs = amazonSqs;
		this.destinationResolver = new CachingDestinationResolver(new DynamicQueueDestinationResolver(this.amazonSqs));
	}

	/**
	 * Configures a {@link DestinationResolver} overriding the default {@link DynamicQueueDestinationResolver}
	 * for this instance. The destination resolver could add additional logic to retrieve physical destination names.
	 *
	 * @param destinationResolver
	 * 		- the destination resolver. Must not be null
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Configures the {@link org.springframework.messaging.support.converter.MessageConverter} used by this instance to convert the payload into notification messages.
	 * Overrides the default {@link org.springframework.messaging.support.converter.SimpleMessageConverter} that convert String objects into notification messages.
	 *
	 * @param messageConverter
	 * 		- the message converter to be used. Must not be null
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Configures the default destination name for this instance. This name will be used by operations that do not receive
	 * a destination name for the client (e.g. {@link #convertAndSend(Object)}. The value which might be a logical or
	 * physical name (queue url) will be resolved during the method called to the physical name.
	 *
	 * @param defaultDestinationName
	 * 		- the default destination name, either a topic arn or logical topic name.
	 */
	public void setDefaultDestinationName(String defaultDestinationName) {
		this.defaultDestinationName = defaultDestinationName;
	}

	/**
	 * Converts and sends the payload.
	 *
	 * @param payload
	 * 		- the payload that will be converted and sent (e.g. a String in combination with a {@link org.springframework.messaging.support.converter.SimpleMessageConverter}
	 * @throws java.lang.IllegalStateException
	 * 		if the default destination name is not set
	 */
	@Override
	public void convertAndSend(Object payload) {
		Assert.state(this.defaultDestinationName != null, "No default destination name configured for this template.");
		this.convertAndSend(this.defaultDestinationName, payload);
	}

	/**
	 * Converts and sends the payload.
	 *
	 * @param destinationName
	 * 		- the logical or physical destination name, must not be null
	 * @param payload
	 * 		- the payload to be sent
	 */
	@Override
	public void convertAndSend(String destinationName, Object payload) {
		Assert.notNull(destinationName, "destinationName must not be null.");
		String destinationUrl = this.destinationResolver.resolveDestinationName(destinationName);
		org.springframework.messaging.Message<?> message = this.messageConverter.toMessage(payload, null);
		SendMessageRequest request = new SendMessageRequest(destinationUrl, message.getPayload().toString());
		this.amazonSqs.sendMessage(request);
	}

	/**
	 * Receives and converts the payload.
	 *
	 * @throws IllegalStateException
	 * 		if the default destination name is not set
	 */
	@Override
	public Object receiveAndConvert() {
		Assert.state(this.defaultDestinationName != null, "No default destination name configured for this template.");
		return this.receiveAndConvert(this.defaultDestinationName);
	}

	/**
	 * Receives and converts and casts the payload.
	 *
	 * @throws IllegalStateException
	 * 		if the default destination name is not set
	 * @throws IllegalArgumentException
	 * 		if the expected type is null
	 */
	@Override
	public <T> T receiveAndConvert(Class<T> expectedType) {
		Assert.state(this.defaultDestinationName != null, "No default destination name configured for this template.");
		return receiveAndConvert(this.defaultDestinationName, expectedType);
	}

	/**
	 * Receives and converts a message with the provided destination name
	 *
	 * @param destinationName
	 * 		- the logical or physical destination name which will pe polled for a new message
	 * @return the converted object
	 */
	@Override
	public Object receiveAndConvert(String destinationName) {
		Assert.notNull(destinationName, "destinationName must not be null.");
		String destinationUrl = this.destinationResolver.resolveDestinationName(destinationName);
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(destinationUrl).withMaxNumberOfMessages(1);
		ReceiveMessageResult receiveMessageResult = this.amazonSqs.receiveMessage(receiveMessageRequest);
		if (receiveMessageResult.getMessages().isEmpty()) {
			return null;
		}

		Message message = receiveMessageResult.getMessages().get(0);

		String payload = message.getBody();
		org.springframework.messaging.Message<String> msg = MessageBuilder.withPayload(payload).build();
		Object result = this.messageConverter.fromMessage(msg,null);

		this.amazonSqs.deleteMessage(new DeleteMessageRequest(destinationUrl, message.getReceiptHandle()));

		return result;
	}

	/**
	 * Receives and converts the object
	 *
	 * @param destinationName
	 * 		- the logical or physical destination name which will pe polled for a new message
	 * @param expectedType
	 * 		the class of the expected type to which the message should be cast
	 * @return the object of type as an instance of expectedType
	 */
	@Override
	public <T> T receiveAndConvert(String destinationName, Class<T> expectedType) {
		Assert.notNull(expectedType, "expectedType must not be null");
		Object result = receiveAndConvert(destinationName);
		Assert.isTrue(expectedType.isInstance(result), "result is not of expected type:" + expectedType.getName());
		return expectedType.cast(result);
	}
}