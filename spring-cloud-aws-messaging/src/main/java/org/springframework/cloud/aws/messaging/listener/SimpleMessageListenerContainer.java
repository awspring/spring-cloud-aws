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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.springframework.cloud.aws.messaging.core.QueueMessageUtils.createMessage;

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
	private boolean deleteMessageOnException = true;
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMessageListenerContainer.class);
	private long backOffTime = 10000;

	protected TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public boolean isDeleteMessageOnException() {
		return this.deleteMessageOnException;
	}

	public void setDeleteMessageOnException(boolean deleteMessageOnException) {
		this.deleteMessageOnException = deleteMessageOnException;
	}

	/**
	 * @return The number of milliseconds the polling thread must wait before trying to recover when an error occurs
	 * (e.g. connection timeout)
	 */
	public long getBackOffTime() {
		return this.backOffTime;
	}

	/**
	 * The number of milliseconds the polling thread must wait before trying to recover when an error occurs
	 * (e.g. connection timeout)
	 *
	 * @param backOffTime
	 * 		in milliseconds
	 */
	public void setBackOffTime(long backOffTime) {
		this.backOffTime = backOffTime;
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
			if (this.stopLatch != null) {
				this.stopLatch.await();
			}
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
				try {
					ReceiveMessageResult receiveMessageResult = getAmazonSqs().receiveMessage(this.queueAttributes.getReceiveMessageRequest());
					CountDownLatch messageBatchLatch = new CountDownLatch(receiveMessageResult.getMessages().size());
					for (Message message : receiveMessageResult.getMessages()) {
						if (isRunning()) {
							MessageExecutor messageExecutor = new MessageExecutor(this.logicalQueueName, message, this.queueAttributes);
							getTaskExecutor().execute(new SignalExecutingRunnable(messageBatchLatch, messageExecutor));
						} else {
							messageBatchLatch.countDown();
						}
					}
					try {
						messageBatchLatch.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				} catch (Exception e) {
					getLogger().warn("An Exception occurred while pooling queue '{}'. The failing operation will be " +
							"retried in {} milliseconds", this.logicalQueueName, getBackOffTime(), e);
					try {
						//noinspection BusyWait
						Thread.sleep(getBackOffTime());
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
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

		@Override
		public void run() {
			String receiptHandle = this.message.getReceiptHandle();
			org.springframework.messaging.Message<String> queueMessage = createMessage(this.message,
					Collections.<String, Object>singletonMap(QueueMessageHandler.Headers.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, this.logicalQueueName));
			try {
				executeMessage(queueMessage);
				getAmazonSqs().deleteMessageAsync(new DeleteMessageRequest(this.queueUrl, receiptHandle));
			} catch (MessagingException e) {
				LOGGER.error("Exception encountered while processing message.", e);
				if (!this.hasRedrivePolicy && SimpleMessageListenerContainer.this.isDeleteMessageOnException()) {
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
			try {
				this.runnable.run();
			} finally {
				this.countDownLatch.countDown();
			}
		}
	}
}
