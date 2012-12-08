/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.core.sqs;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.elasticspring.messaging.core.MessageOperations;
import org.elasticspring.messaging.core.StringMessage;
import org.elasticspring.messaging.support.converter.MessageConverter;
import org.elasticspring.messaging.support.converter.StringMessageConverter;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicDestinationResolver;
import org.springframework.beans.factory.DisposableBean;

/**
 *
 */
public class SimpleQueueServiceMessageTemplate implements MessageOperations, DisposableBean {

	private final AmazonSQS amazonSQS;
	private final String defaultDestination;
	private final DestinationResolver destinationResolver;
	private MessageConverter messageConverter = new StringMessageConverter();

	public SimpleQueueServiceMessageTemplate(String accessKey, String secretKey, String defaultDestination) {
		this.amazonSQS = new AmazonSQSClient(new BasicAWSCredentials(accessKey, secretKey));
		this.defaultDestination = defaultDestination;
		this.destinationResolver = new CachingDestinationResolver(new DynamicDestinationResolver(this.getQueueingService()));
	}

	public void convertAndSend(Object payLoad) {
		this.convertAndSend(this.defaultDestination, payLoad);
	}

	public void convertAndSend(String destinationName, Object payLoad) {
		String destinationUrl = this.destinationResolver.resolveDestinationName(destinationName);
		org.elasticspring.messaging.core.Message<String> message = this.getMessageConverter().toMessage(payLoad);
		SendMessageRequest request = new SendMessageRequest(destinationUrl, message.getPayload());
		this.getQueueingService().sendMessage(request);
	}

	public Object receiveAndConvert() {
		return this.receiveAndConvert(this.defaultDestination);
	}

	public Object receiveAndConvert(String destinationName) {
		String destinationUrl = this.destinationResolver.resolveDestinationName(destinationName);
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(destinationUrl).withMaxNumberOfMessages(1);
		ReceiveMessageResult receiveMessageResult = this.getQueueingService().receiveMessage(receiveMessageRequest);
		if (receiveMessageResult.getMessages().isEmpty()) {
			return null;
		}

		Message message = receiveMessageResult.getMessages().get(0);

		org.elasticspring.messaging.core.Message<String> msg = new StringMessage(message.getBody(),message.getAttributes());
		Object result = this.getMessageConverter().fromMessage(msg);

		this.getQueueingService().deleteMessage(new DeleteMessageRequest(destinationUrl, message.getReceiptHandle()));

		return result;
	}


	protected AmazonSQS getQueueingService(){
		return this.amazonSQS;
	}

	protected MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	public void destroy() throws Exception {
		this.getQueueingService().shutdown();
	}
}