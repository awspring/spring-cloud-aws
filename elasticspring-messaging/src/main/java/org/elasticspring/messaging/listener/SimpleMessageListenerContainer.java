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

package org.elasticspring.messaging.listener;

import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ClassUtils;

import java.util.Map;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer {

	private static final String DEFAULT_THREAD_NAME_PREFIX =
			ClassUtils.getShortName(SimpleMessageListenerContainer.class) + "-";

	private TaskExecutor taskExecutor;

	protected TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	protected void initialize() {
		if (this.taskExecutor == null) {
			this.taskExecutor = createDefaultTaskExecutor();
		}

		super.initialize();
	}

	/**
	 * Create a default TaskExecutor. Called if no explicit TaskExecutor has been specified.
	 * <p>The default implementation builds a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
	 * with the specified bean name (or the class name, if no bean name specified) as thread name prefix.
	 *
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
	 */
	protected TaskExecutor createDefaultTaskExecutor() {
		String beanName = getBeanName();
		String threadNamePrefix = (beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
		return new SimpleAsyncTaskExecutor(threadNamePrefix);
	}

	@Override
	protected void doStart() {
		synchronized (this.getLifecycleMonitor()) {
			scheduleMessageListeners();
		}
	}

	@Override
	protected void doStop() {

	}

	private void scheduleMessageListeners() {
		for (Map.Entry<String, ReceiveMessageRequest> messageRequest : getMessageRequests().entrySet()) {
			getTaskExecutor().execute(new AsynchronousMessageListener(messageRequest.getKey(), messageRequest.getValue()));
		}
	}

	protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
		try {
			getMessageHandler().handleMessage(stringMessage);
		} catch (Throwable throwable) {
			handleError(throwable);
		}
	}

	private class AsynchronousMessageListener implements Runnable {

		private final ReceiveMessageRequest receiveMessageRequest;
		private final String logicalQueueName;

		private AsynchronousMessageListener(String logicalQueueName, ReceiveMessageRequest receiveMessageRequest) {
			this.logicalQueueName = logicalQueueName;
			this.receiveMessageRequest = receiveMessageRequest;
		}

		@Override
		public void run() {
			while (isRunning()) {
				ReceiveMessageResult receiveMessageResult = getAmazonSqs().receiveMessage(this.receiveMessageRequest);
				for (Message message : receiveMessageResult.getMessages()) {
					if (isRunning()) {
						getTaskExecutor().execute(new MessageExecutor(this.logicalQueueName, message, this.receiveMessageRequest.getQueueUrl()));
					} else {
						break;
					}
				}
			}
		}
	}

	private class MessageExecutor implements Runnable {

		private final Message message;
		private final String logicalQueueName;
		private final String queueUrl;

		private MessageExecutor(String logicalQueueName, Message message, String queueUrl) {
			this.logicalQueueName = logicalQueueName;
			this.message = message;
			this.queueUrl = queueUrl;
		}

		@Override
		public void run() {
			String receiptHandle = this.message.getReceiptHandle();
			String payload = this.message.getBody();
			executeMessage(MessageBuilder.withPayload(payload).setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, this.logicalQueueName).build());
			getAmazonSqs().deleteMessage(new DeleteMessageRequest(this.queueUrl, receiptHandle));
			getLogger().debug("Deleted message with id {} and receipt handle {}", this.message.getMessageId(), this.message.getReceiptHandle());
		}
	}
}
