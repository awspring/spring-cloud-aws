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

package org.elasticspring.messaging.sqs;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.elasticspring.messaging.MessageOperations;
import org.springframework.beans.factory.DisposableBean;

/**
 *
 */
public class SimpleQueueServiceMessageTemplate implements MessageOperations, DisposableBean {

	private final AmazonSQS amazonSQS;
	private final String defaultDestination;

	public SimpleQueueServiceMessageTemplate(String accessKey, String secretKey, String defaultDestination) {
		this.amazonSQS = new AmazonSQSClient(new BasicAWSCredentials(accessKey, secretKey));
		this.defaultDestination = defaultDestination;
	}

	public void convertAndSend(Object payLoad) {
		this.convertAndSend(this.defaultDestination, payLoad);
	}

	public void convertAndSend(String destinationName, Object payLoad) {
		CreateQueueResult createQueueResult = this.getQueueingService().createQueue(new CreateQueueRequest(destinationName));
		SendMessageRequest request = new SendMessageRequest(createQueueResult.getQueueUrl(), payLoad.toString());
		this.getQueueingService().sendMessage(request);
	}

	public Object receiveAndConvert() {
		return this.receiveAndConvert(this.defaultDestination);
	}

	public Object receiveAndConvert(String destinationName) {
		CreateQueueResult createQueueResult = this.getQueueingService().createQueue(new CreateQueueRequest(destinationName));
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(createQueueResult.getQueueUrl()).withMaxNumberOfMessages(1);
		ReceiveMessageResult receiveMessageResult = this.getQueueingService().receiveMessage(receiveMessageRequest);
		if (receiveMessageResult.getMessages().isEmpty()) {
			return null;
		}

		Message message = receiveMessageResult.getMessages().get(0);
		this.getQueueingService().deleteMessage(new DeleteMessageRequest(createQueueResult.getQueueUrl(), message.getReceiptHandle()));
		return receiveMessageResult.getMessages().get(0).getBody();
	}


	protected AmazonSQS getQueueingService(){
		return this.amazonSQS;
	}

	public void destroy() throws Exception {
		this.getQueueingService().shutdown();
	}
}