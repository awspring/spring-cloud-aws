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

package org.elasticspring.messaging.listener;

import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.elasticspring.messaging.support.converter.MessageConverter;

import java.util.List;

/**
 *
 */
public class DefaultMessageListenerContainer extends AbstractPollingMessageListenerContainer {


	@Override
	protected void initialize() {
		getTaskScheduler().scheduleAtFixedRate(new MessageListener(), 5L);
	}

	@Override
	protected void destroy() {

	}

	private class MessageListener implements Runnable {

		public void run() {
			ReceiveMessageResult receiveMessageResult = getAmazonSQS().receiveMessage(new ReceiveMessageRequest(""));
			List<Message> messages = receiveMessageResult.getMessages();
			for (Message message : messages) {
				getTaskExecutor().execute(new MessageExecutor(message));
			}
		}
	}

	private class MessageExecutor implements Runnable {

		private final Message message;
		private MessageConverter messageConverter;

		private MessageExecutor(Message message) {
			this.message = message;
		}

		public void run() {

			String receiptHandle = this.message.getReceiptHandle();
			getAmazonSQS().deleteMessage(new DeleteMessageRequest("", receiptHandle));
		}
	}
}
