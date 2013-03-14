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
import org.elasticspring.messaging.StringMessage;
import org.elasticspring.messaging.core.QueueingOperations;
import org.elasticspring.messaging.support.converter.MessageConverter;
import org.elasticspring.messaging.support.converter.StringMessageConverter;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicQueueDestinationResolver;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class SimpleQueueingServiceTemplate implements QueueingOperations {

	private final AmazonSQS amazonSQS;
	private DestinationResolver destinationResolver;
	private MessageConverter messageConverter = new StringMessageConverter();
	private String defaultDestinationName;

	public SimpleQueueingServiceTemplate(AmazonSQS amazonSQS) {
		this.amazonSQS = amazonSQS;
		this.destinationResolver = new CachingDestinationResolver(new DynamicQueueDestinationResolver(this.amazonSQS));
	}

	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	public void setDefaultDestinationName(String defaultDestinationName) {
		this.defaultDestinationName = defaultDestinationName;
	}

	@Override
	public void convertAndSend(Object payLoad) {
		Assert.state(this.defaultDestinationName != null, "No default destination name configured for this template.");
		this.convertAndSend(this.defaultDestinationName, payLoad);
	}

	@Override
	public void convertAndSend(String destinationName, Object payLoad) {
		Assert.notNull(destinationName, "destinationName must not be null.");
		String destinationUrl = this.destinationResolver.resolveDestinationName(destinationName);
		org.elasticspring.messaging.Message<String> message = this.messageConverter.toMessage(payLoad);
		SendMessageRequest request = new SendMessageRequest(destinationUrl, message.getPayload());
		this.amazonSQS.sendMessage(request);
	}

	@Override
	public Object receiveAndConvert() {
		Assert.state(this.defaultDestinationName != null, "No default destination name configured for this template.");
		return this.receiveAndConvert(this.defaultDestinationName);
	}

	@Override
	public <T> T receiveAndConvert(Class<T> expectedType) {
		Assert.state(this.defaultDestinationName != null, "No default destination name configured for this template.");
		return receiveAndConvert(this.defaultDestinationName, expectedType);
	}

	@Override
	public Object receiveAndConvert(String destinationName) {
		Assert.notNull(destinationName, "destinationName must not be null.");
		String destinationUrl = this.destinationResolver.resolveDestinationName(destinationName);
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(destinationUrl).withMaxNumberOfMessages(1);
		ReceiveMessageResult receiveMessageResult = this.amazonSQS.receiveMessage(receiveMessageRequest);
		if (receiveMessageResult.getMessages().isEmpty()) {
			return null;
		}

		Message message = receiveMessageResult.getMessages().get(0);

		org.elasticspring.messaging.Message<String> msg = new StringMessage(message.getBody());
		Object result = this.messageConverter.fromMessage(msg);

		this.amazonSQS.deleteMessage(new DeleteMessageRequest(destinationUrl, message.getReceiptHandle()));

		return result;
	}

	@Override
	public <T> T receiveAndConvert(String destinationName, Class<T> expectedType) {
		Assert.notNull(expectedType, "expectedType must not be null");
		Object result = receiveAndConvert(destinationName);
		Assert.isTrue(expectedType.isInstance(result), "result is not of expected type:" + expectedType.getName());
		return expectedType.cast(result);
	}
}