/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.awspring.cloud.messaging.listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageSystemAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static io.awspring.cloud.messaging.core.QueueMessageUtils.createMessage;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Mete Alpaslan Katırcıoğlu
 * @since 1.0
 */
public class SimpleMessageListenerContainer extends AbstractMessageListenerContainer {

	private static final int DEFAULT_WORKER_THREADS = 2;

	private static final String DEFAULT_THREAD_NAME_PREFIX = ClassUtils
			.getShortName(SimpleMessageListenerContainer.class) + "-";

	private boolean defaultTaskExecutor;

	private long backOffTime = 10000;

	private long queueStopTimeout = 20000;

	private AsyncTaskExecutor taskExecutor;

	private ConcurrentHashMap<String, Future<?>> scheduledFutureByQueue;

	private ConcurrentHashMap<String, Boolean> runningStateByQueue;

	protected AsyncTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @return The number of milliseconds the polling thread must wait before trying to
	 * recover when an error occurs (e.g. connection timeout)
	 */
	public long getBackOffTime() {
		return this.backOffTime;
	}

	/**
	 * The number of milliseconds the polling thread must wait before trying to recover
	 * when an error occurs (e.g. connection timeout). Default is 10000 milliseconds.
	 * @param backOffTime in milliseconds
	 */
	public void setBackOffTime(long backOffTime) {
		this.backOffTime = backOffTime;
	}

	/**
	 * @return The number of milliseconds the
	 * {@link SimpleMessageListenerContainer#stop(String)} method waits for a queue to
	 * stop before interrupting the current thread. Default value is 10000 milliseconds
	 * (10 seconds).
	 */
	public long getQueueStopTimeout() {
		return this.queueStopTimeout;
	}

	/**
	 * The number of milliseconds the {@link SimpleMessageListenerContainer#stop(String)}
	 * method waits for a queue to stop before interrupting the current thread. Default
	 * value is 20000 milliseconds (20 seconds).
	 * @param queueStopTimeout in milliseconds
	 */
	public void setQueueStopTimeout(long queueStopTimeout) {
		this.queueStopTimeout = queueStopTimeout;
	}

	@Override
	protected void initialize() {
		super.initialize();

		if (this.taskExecutor == null) {
			this.defaultTaskExecutor = true;
			this.taskExecutor = createDefaultTaskExecutor();
		}

		initializeRunningStateByQueue();
		this.scheduledFutureByQueue = new ConcurrentHashMap<>(getRegisteredQueues().size());
	}

	private void initializeRunningStateByQueue() {
		this.runningStateByQueue = new ConcurrentHashMap<>(getRegisteredQueues().size());
		for (String queueName : getRegisteredQueues().keySet()) {
			this.runningStateByQueue.put(queueName, false);
		}
	}

	@Override
	protected void doStart() {
		synchronized (this.getLifecycleMonitor()) {
			scheduleMessageListeners();
		}
	}

	@Override
	protected void doStop() {
		notifyRunningQueuesToStop();
		waitForRunningQueuesToStop();
	}

	private void notifyRunningQueuesToStop() {
		for (Map.Entry<String, Boolean> runningStateByQueue : this.runningStateByQueue.entrySet()) {
			if (runningStateByQueue.getValue()) {
				stopQueue(runningStateByQueue.getKey());
			}
		}
	}

	private void waitForRunningQueuesToStop() {
		for (Map.Entry<String, Boolean> queueRunningState : this.runningStateByQueue.entrySet()) {
			String logicalQueueName = queueRunningState.getKey();
			Future<?> queueSpinningThread = this.scheduledFutureByQueue.get(logicalQueueName);

			if (queueSpinningThread != null) {
				try {
					queueSpinningThread.get(getQueueStopTimeout(), TimeUnit.MILLISECONDS);
				}
				catch (ExecutionException | TimeoutException e) {
					getLogger().warn("An exception occurred while stopping queue '" + logicalQueueName + "'", e);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	@Override
	protected void doDestroy() {
		if (this.defaultTaskExecutor) {
			((ThreadPoolTaskExecutor) this.taskExecutor).destroy();
		}
	}

	/**
	 * Create a default TaskExecutor. Called if no explicit TaskExecutor has been
	 * specified.
	 * <p>
	 * The default implementation builds a
	 * {@link org.springframework.core.task.SimpleAsyncTaskExecutor} with the specified
	 * bean name (or the class name, if no bean name specified) as thread name prefix.
	 * @return a {@link org.springframework.core.task.SimpleAsyncTaskExecutor} configured
	 * with the thread name prefix
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
	 */
	protected AsyncTaskExecutor createDefaultTaskExecutor() {
		String beanName = getBeanName();
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setThreadNamePrefix(beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
		int spinningThreads = this.getRegisteredQueues().size();

		if (spinningThreads > 0) {
			threadPoolTaskExecutor.setCorePoolSize(spinningThreads * DEFAULT_WORKER_THREADS);

			int maxNumberOfMessagePerBatch = getMaxNumberOfMessages() != null ? getMaxNumberOfMessages()
					: DEFAULT_MAX_NUMBER_OF_MESSAGES;
			threadPoolTaskExecutor.setMaxPoolSize(spinningThreads * (maxNumberOfMessagePerBatch + 1));
		}

		// No use of a thread pool executor queue to avoid retaining message to long in
		// memory
		threadPoolTaskExecutor.setQueueCapacity(0);
		threadPoolTaskExecutor.afterPropertiesSet();

		return threadPoolTaskExecutor;

	}

	private void scheduleMessageListeners() {
		for (Map.Entry<String, QueueAttributes> registeredQueue : getRegisteredQueues().entrySet()) {
			startQueue(registeredQueue.getKey(), registeredQueue.getValue());
		}
	}

	protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
		getMessageHandler().handleMessage(stringMessage);
	}

	/**
	 * Stops and waits until the specified queue has stopped. If the wait timeout
	 * specified by {@link SimpleMessageListenerContainer#getQueueStopTimeout()} is
	 * reached, the current thread is interrupted.
	 * @param logicalQueueName the name as defined on the listener method
	 */
	public void stop(String logicalQueueName) {
		stopQueue(logicalQueueName);

		try {
			if (isRunning(logicalQueueName)) {
				Future<?> future = this.scheduledFutureByQueue.remove(logicalQueueName);
				if (future != null) {
					future.get(this.queueStopTimeout, TimeUnit.MILLISECONDS);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException | TimeoutException e) {
			getLogger().warn("Error stopping queue with name: '" + logicalQueueName + "'", e);
		}
	}

	protected void stopQueue(String logicalQueueName) {
		Assert.isTrue(this.runningStateByQueue.containsKey(logicalQueueName),
				"Queue with name '" + logicalQueueName + "' does not exist");
		this.runningStateByQueue.put(logicalQueueName, false);
	}

	public void start(String logicalQueueName) {
		Assert.isTrue(this.runningStateByQueue.containsKey(logicalQueueName),
				"Queue with name '" + logicalQueueName + "' does not exist");

		QueueAttributes queueAttributes = this.getRegisteredQueues().get(logicalQueueName);
		startQueue(logicalQueueName, queueAttributes);
	}

	/**
	 * Checks if the spinning thread for the specified queue {@code logicalQueueName} is
	 * still running (polling for new messages) or not.
	 * @param logicalQueueName the name as defined on the listener method
	 * @return {@code true} if the spinning thread for the specified queue is running
	 * otherwise {@code false}.
	 */
	public boolean isRunning(String logicalQueueName) {
		Future<?> future = this.scheduledFutureByQueue.get(logicalQueueName);
		return future != null && !future.isCancelled() && !future.isDone();
	}

	protected void startQueue(String queueName, QueueAttributes queueAttributes) {
		if (isQueueRunning(queueName)) {
			return;
		}

		this.runningStateByQueue.put(queueName, true);
		Future<?> future = getTaskExecutor().submit(new AsynchronousMessageListener(queueName, queueAttributes));
		this.scheduledFutureByQueue.put(queueName, future);
	}

	protected boolean isQueueRunning(String logicalQueueName) {
		if (this.runningStateByQueue.containsKey(logicalQueueName)) {
			return this.runningStateByQueue.get(logicalQueueName);
		}
		else {
			getLogger().warn("Stopped queue '" + logicalQueueName + "' because it was not listed as running queue.");
			return false;
		}
	}

	private static final class SignalExecutingRunnable implements Runnable {

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
			}
			finally {
				this.countDownLatch.countDown();
			}
		}

	}

	private final class AsynchronousMessageListener implements Runnable {

		private final QueueAttributes queueAttributes;

		private final String logicalQueueName;

		private AsynchronousMessageListener(String logicalQueueName, QueueAttributes queueAttributes) {
			this.logicalQueueName = logicalQueueName;
			this.queueAttributes = queueAttributes;
		}

		@Override
		public void run() {
			while (isQueueRunning(this.logicalQueueName)) {
				try {
					ReceiveMessageResult receiveMessageResult = getAmazonSqs()
							.receiveMessage(this.queueAttributes.getReceiveMessageRequest());

					final List<MessageGroup> messageGroups = queueAttributes.isFifo()
							? groupByMessageGroupId(receiveMessageResult) : groupByMessage(receiveMessageResult);
					CountDownLatch messageBatchLatch = new CountDownLatch(messageGroups.size());
					for (MessageGroup messageGroup : messageGroups) {
						if (isQueueRunning(this.logicalQueueName)) {
							MessageGroupExecutor messageGroupExecutor = new MessageGroupExecutor(this.logicalQueueName,
									messageGroup, this.queueAttributes);
							getTaskExecutor()
									.execute(new SignalExecutingRunnable(messageBatchLatch, messageGroupExecutor));
						}
						else {
							messageBatchLatch.countDown();
						}
					}
					try {
						messageBatchLatch.await();
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				catch (Exception e) {
					getLogger().warn("An Exception occurred while polling queue '{}'. The failing operation will be "
							+ "retried in {} milliseconds", this.logicalQueueName, getBackOffTime(), e);
					try {
						// noinspection BusyWait
						Thread.sleep(getBackOffTime());
					}
					catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
			}

			SimpleMessageListenerContainer.this.scheduledFutureByQueue.remove(this.logicalQueueName);
		}

		private List<MessageGroup> groupByMessageGroupId(final ReceiveMessageResult receiveMessageResult) {
			return receiveMessageResult.getMessages().stream()
					.collect(Collectors.groupingBy(message -> message.getMessageAttributes()
							.get(MessageSystemAttributeName.MessageGroupId.name())))
					.values().stream().map(MessageGroup::new).collect(Collectors.toList());
		}

		private List<MessageGroup> groupByMessage(final ReceiveMessageResult receiveMessageResult) {
			return receiveMessageResult.getMessages().stream().map(MessageGroup::new).collect(Collectors.toList());
		}

	}

	private static final class MessageGroup {

		private final List<Message> messages;

		MessageGroup(final Message message) {
			this.messages = Collections.singletonList(message);
		}

		MessageGroup(final List<Message> messages) {
			this.messages = messages;
		}

		public List<Message> getMessages() {
			return this.messages;
		}

	}

	private final class MessageGroupExecutor implements Runnable {

		private final MessageGroup messageGroup;

		private final String logicalQueueName;

		private final String queueUrl;

		private final boolean hasRedrivePolicy;

		private final SqsMessageDeletionPolicy deletionPolicy;

		private MessageGroupExecutor(String logicalQueueName, MessageGroup messageGroup,
				QueueAttributes queueAttributes) {
			this.logicalQueueName = logicalQueueName;
			this.messageGroup = messageGroup;
			this.queueUrl = queueAttributes.getReceiveMessageRequest().getQueueUrl();
			this.hasRedrivePolicy = queueAttributes.hasRedrivePolicy();
			this.deletionPolicy = queueAttributes.getDeletionPolicy();
		}

		@Override
		public void run() {
			for (Message message : this.messageGroup.getMessages()) {
				String receiptHandle = message.getReceiptHandle();
				org.springframework.messaging.Message<String> queueMessage = getMessageForExecution(message);
				try {
					executeMessage(queueMessage);
					applyDeletionPolicyOnSuccess(receiptHandle);
				}
				catch (MessagingException messagingException) {
					applyDeletionPolicyOnError(receiptHandle);
				}
			}
		}

		private void applyDeletionPolicyOnSuccess(String receiptHandle) {
			if (this.deletionPolicy == SqsMessageDeletionPolicy.ON_SUCCESS
					|| this.deletionPolicy == SqsMessageDeletionPolicy.ALWAYS
					|| this.deletionPolicy == SqsMessageDeletionPolicy.NO_REDRIVE) {
				deleteMessage(receiptHandle);
			}
		}

		private void applyDeletionPolicyOnError(String receiptHandle) {
			if (this.deletionPolicy == SqsMessageDeletionPolicy.ALWAYS
					|| (this.deletionPolicy == SqsMessageDeletionPolicy.NO_REDRIVE && !this.hasRedrivePolicy)) {
				deleteMessage(receiptHandle);
			}
		}

		private void deleteMessage(String receiptHandle) {
			getAmazonSqs().deleteMessageAsync(new DeleteMessageRequest(this.queueUrl, receiptHandle),
					new DeleteMessageHandler(receiptHandle));
		}

		private org.springframework.messaging.Message<String> getMessageForExecution(final Message message) {
			HashMap<String, Object> additionalHeaders = new HashMap<>();
			additionalHeaders.put(QueueMessageHandler.LOGICAL_RESOURCE_ID, this.logicalQueueName);
			if (this.deletionPolicy == SqsMessageDeletionPolicy.NEVER) {
				String receiptHandle = message.getReceiptHandle();
				QueueMessageAcknowledgment acknowledgment = new QueueMessageAcknowledgment(
						SimpleMessageListenerContainer.this.getAmazonSqs(), this.queueUrl, receiptHandle);
				additionalHeaders.put(QueueMessageHandler.ACKNOWLEDGMENT, acknowledgment);
			}
			additionalHeaders.put(QueueMessageHandler.VISIBILITY, new QueueMessageVisibility(
					SimpleMessageListenerContainer.this.getAmazonSqs(), this.queueUrl, message.getReceiptHandle()));

			return createMessage(message, additionalHeaders);
		}

	}

}
