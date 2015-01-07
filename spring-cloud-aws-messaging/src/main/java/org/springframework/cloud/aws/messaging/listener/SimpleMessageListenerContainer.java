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

package org.springframework.cloud.aws.messaging.listener;

import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer {

	private static final int DEFAULT_WORKER_THREADS = 2;
	private static final String DEFAULT_THREAD_NAME_PREFIX =
			ClassUtils.getShortName(SimpleMessageListenerContainer.class) + "-";
	private TaskExecutor taskExecutor;

	private volatile CountDownLatch stopLatch;
	private boolean defaultTaskExecutor;

	protected TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	protected void initialize() {
		if (this.taskExecutor == null) {
			this.defaultTaskExecutor = true;
			this.taskExecutor = createDefaultTaskExecutor();
		}

		super.initialize();
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

	@Override
	protected void doDestroy() {
		if (this.defaultTaskExecutor) {
			((ThreadPoolTaskExecutor) this.taskExecutor).destroy();
		}
	}

	/**
	 * Create a default TaskExecutor. Called if no explicit TaskExecutor has been specified.
	 * <p>The default implementation builds a {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
	 * with the specified bean name (or the class name, if no bean name specified) as thread name prefix.
	 *
	 * @return a {@link org.springframework.core.task.SimpleAsyncTaskExecutor} configured with the thread name prefix
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
	 */
	protected TaskExecutor createDefaultTaskExecutor() {
		String beanName = getBeanName();
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setThreadNamePrefix(beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
		int spinningThreads = this.getRegisteredQueues().size();

		if (spinningThreads > 0) {
			threadPoolTaskExecutor.setCorePoolSize(spinningThreads * DEFAULT_WORKER_THREADS);

			int maxNumberOfMessagePerBatch = getMaxNumberOfMessages() != null ? getMaxNumberOfMessages() : DEFAULT_WORKER_THREADS;
			threadPoolTaskExecutor.setMaxPoolSize(spinningThreads * maxNumberOfMessagePerBatch);
		}

		// No use of a thread pool executor queue to avoid retaining message to long in memory
		threadPoolTaskExecutor.setQueueCapacity(0);
		threadPoolTaskExecutor.afterPropertiesSet();

		return threadPoolTaskExecutor;

	}

	private void scheduleMessageListeners() {
		this.stopLatch = new CountDownLatch(getRegisteredQueues().size());
		for (Map.Entry<String, QueueAttributes> messageRequest : getRegisteredQueues().entrySet()) {
			getTaskExecutor().execute(new SignalExecutingRunnable(this.stopLatch, new AsynchronousMessageListener(messageRequest.getKey(), messageRequest.getValue())));
		}
	}

	protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
		getMessageHandler().handleMessage(stringMessage);
	}

	private class AsynchronousMessageListener implements Runnable {

		private final QueueAttributes queueAttributes;
		private final String logicalQueueName;

		private AsynchronousMessageListener(String logicalQueueName, QueueAttributes queueAttributes) {
			this.logicalQueueName = logicalQueueName;
			this.queueAttributes = queueAttributes;
		}

		@Override
		public void run() {
			while (isRunning()) {
				ReceiveMessageResult receiveMessageResult = getAmazonSqs().receiveMessage(this.queueAttributes.getReceiveMessageRequest());
				CountDownLatch messageBatchLatch = new CountDownLatch(receiveMessageResult.getMessages().size());
				for (Message message : receiveMessageResult.getMessages()) {
					if (isRunning()) {
						MessageExecutor messageExecutor = new MessageExecutor(this.logicalQueueName, message, this.queueAttributes);
						getTaskExecutor().execute(new SignalExecutingRunnable(messageBatchLatch, messageExecutor));
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
		}
	}

	private class MessageExecutor implements Runnable {

		private final Message message;
		private final String logicalQueueName;
		private final String queueUrl;
		private final boolean hasRedrivePolicy;

		private MessageExecutor(String logicalQueueName, Message message, QueueAttributes queueAttributes) {
			this.logicalQueueName = logicalQueueName;
			this.message = message;
			this.queueUrl = queueAttributes.getReceiveMessageRequest().getQueueUrl();
			this.hasRedrivePolicy = queueAttributes.hasRedrivePolicy();
		}

		private void copyAttributesToHeaders(MessageBuilder<String> messageBuilder) {
			for (Map.Entry<String, String> attribute : this.message.getAttributes().entrySet()) {
				messageBuilder.setHeader(attribute.getKey(), attribute.getValue());
			}

			if (this.message.getMessageAttributes().containsKey(MessageHeaders.CONTENT_TYPE)) {
				messageBuilder.setHeader(MessageHeaders.CONTENT_TYPE,
						MimeType.valueOf(this.message.getMessageAttributes().get(MessageHeaders.CONTENT_TYPE).getStringValue()));
			}
		}

		@Override
		public void run() {
			String receiptHandle = this.message.getReceiptHandle();
			String payload = this.message.getBody();
			MessageBuilder<String> messageBuilder = MessageBuilder.
					withPayload(payload).
					setHeader(QueueMessageHandler.Headers.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, this.logicalQueueName);
			copyAttributesToHeaders(messageBuilder);
			try {
				executeMessage(messageBuilder.build());
				getAmazonSqs().deleteMessageAsync(new DeleteMessageRequest(this.queueUrl, receiptHandle));
			} catch (MessagingException e) {
				if (!this.hasRedrivePolicy) {
					getAmazonSqs().deleteMessageAsync(new DeleteMessageRequest(this.queueUrl, receiptHandle));
				}

			}
		}
	}

	private static class SignalExecutingRunnable implements Runnable {

		private final CountDownLatch countDownLatch;
		private final Runnable runnable;

		private SignalExecutingRunnable(CountDownLatch endSignal, Runnable runnable) {
			this.countDownLatch = endSignal;
			this.runnable = runnable;
		}

		@Override
		public void run() {
			this.runnable.run();
			this.countDownLatch.countDown();
		}
	}
}
