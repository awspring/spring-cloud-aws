/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.elasticspring.messaging.StringMessage;
import org.springframework.core.task.TaskExecutor;

/**
 *
 */
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer {

	private TaskExecutor taskExecutor;

	protected TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	protected void doStart() {
		synchronized (this.getLifecycleMonitor()) {
			scheduleMessageListener();
		}
	}

	@Override
	protected void doStop() {

	}

	private void scheduleMessageListener() {
		getTaskExecutor().execute(new AsyncMessageListener());
	}

	protected void executeMessage(org.elasticspring.messaging.Message<?> stringMessage) {
		try {
			getMessageListener().onMessage(stringMessage);
		} catch (Throwable throwable) {
			handleException();
		}
	}

	protected void handleException() {
	}


	private class AsyncMessageListener implements Runnable {

		@Override
		public void run() {
			while (isRunning()) {
				synchronized (SimpleMessageListenerContainer.this.getLifecycleMonitor()) {
					ReceiveMessageResult receiveMessageResult = getAmazonSQS().receiveMessage(getReceiveMessageRequest());
					for (Message message : receiveMessageResult.getMessages()) {
						getTaskExecutor().execute(new MessageExecutor(message));
					}
				}
			}
		}
	}

	private class MessageExecutor implements Runnable {

		private final Message message;

		private MessageExecutor(Message message) {
			this.message = message;
		}

		@Override
		public void run() {
			String receiptHandle = this.message.getReceiptHandle();
			executeMessage(new StringMessage(receiptHandle, this.message.getAttributes()));
			getAmazonSQS().deleteMessageAsync(new DeleteMessageRequest(this.message.getMessageId(), receiptHandle));
		}
	}
}
