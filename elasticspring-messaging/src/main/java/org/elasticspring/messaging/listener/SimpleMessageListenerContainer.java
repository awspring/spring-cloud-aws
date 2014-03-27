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
import java.util.concurrent.CountDownLatch;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer {

	private static final String DEFAULT_THREAD_NAME_PREFIX =
			ClassUtils.getShortName(SimpleMessageListenerContainer.class) + "-";

	private TaskExecutor taskExecutor;

	private volatile CountDownLatch stopLatch;

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
		try {
			this.stopLatch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void scheduleMessageListeners() {
		this.stopLatch = new CountDownLatch(getMessageRequests().size());
		for (Map.Entry<String, ReceiveMessageRequest> messageRequest : getMessageRequests().entrySet()) {
			getTaskExecutor().execute(new AsynchronousMessageListener(messageRequest.getKey(), messageRequest.getValue(), this.stopLatch));
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
		private final CountDownLatch messageBatchLatch;
		private final String logicalQueueName;

		private AsynchronousMessageListener(String logicalQueueName, ReceiveMessageRequest receiveMessageRequest, CountDownLatch messageBatchLatch) {
			this.logicalQueueName = logicalQueueName;
			this.receiveMessageRequest = receiveMessageRequest;
			this.messageBatchLatch = messageBatchLatch;
		}

		@Override
		public void run() {
			while (isRunning()) {
				ReceiveMessageResult receiveMessageResult = getAmazonSqs().receiveMessage(this.receiveMessageRequest);
				CountDownLatch messageBatchLatch = new CountDownLatch(receiveMessageResult.getMessages().size());
				for (Message message : receiveMessageResult.getMessages()) {
					if (isRunning()) {
						MessageExecutor messageExecutor = new MessageExecutor(this.logicalQueueName, message, this.receiveMessageRequest.getQueueUrl());
						getTaskExecutor().execute(new CountingRunnableDecorator(messageBatchLatch, messageExecutor));
					} else {
						break;
					}
				}
				try {
					messageBatchLatch.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			this.messageBatchLatch.countDown();
		}
	}

	private static class CountingRunnableDecorator implements Runnable {

		private final CountDownLatch countDownLatch;
		private final Runnable runnable;

		private CountingRunnableDecorator(CountDownLatch countDownLatch, Runnable runnable) {
			this.countDownLatch = countDownLatch;
			this.runnable = runnable;
		}

		@Override
		public void run() {
			this.runnable.run();
			this.countDownLatch.countDown();
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
