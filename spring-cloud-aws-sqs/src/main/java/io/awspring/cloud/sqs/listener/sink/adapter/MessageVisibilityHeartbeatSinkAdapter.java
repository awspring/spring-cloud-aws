/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.sqs.listener.sink.adapter;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.BatchVisibility;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * A {@link AbstractDelegatingMessageListeningSinkAdapter} that adds an interceptor responsible for periodically
 * extending visibility while a message (or message batch) is being processed.
 * This is known as heart beating and recommended in SQS best practices for cases when expected duration is unknown.
 * See https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-visibility-timeout.html#visibility-timeout-best-practices
 * > If you're unsure about the exact processing time, begin with a shorter timeout (for example, 2 minutes)
 *   and extend it as necessary. Implement a heartbeat mechanism to periodically extend the visibility timeout,
 *   ensuring the message remains invisible until processing is complete.
 *
 * @author kzurawski
 * @since 4.1.0
 */
public class MessageVisibilityHeartbeatSinkAdapter<T> extends AbstractDelegatingMessageListeningSinkAdapter<T> {

	private static final Logger logger = LoggerFactory.getLogger(MessageVisibilityHeartbeatSinkAdapter.class);

	private static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofSeconds(10);

	private static final Duration DEFAULT_HEARTBEAT_VISIBILITY_TIMEOUT = Duration.ofSeconds(30);

	private static final String THREAD_NAME_PREFIX = "sqs-message-visibility-heartbeat-";

	private Duration heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;

	private int heartbeatVisibilityTimeoutSeconds = (int) DEFAULT_HEARTBEAT_VISIBILITY_TIMEOUT.getSeconds();

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();

	@Nullable
	private ScheduledExecutorService scheduler;

	public MessageVisibilityHeartbeatSinkAdapter(MessageSink<T> delegate) {
		super(delegate);
	}

	public void setHeartbeatInterval(Duration heartbeatInterval) {
		Assert.notNull(heartbeatInterval, "heartbeatInterval cannot be null");
		Assert.isTrue(!heartbeatInterval.isNegative() && !heartbeatInterval.isZero(),
				"heartbeatInterval must be greater than zero");
		this.heartbeatInterval = heartbeatInterval;
	}

	public void setHeartbeatVisibilityTimeout(Duration heartbeatVisibilityTimeout) {
		Assert.notNull(heartbeatVisibilityTimeout, "heartbeatVisibilityTimeout cannot be null");
		Assert.isTrue(!heartbeatVisibilityTimeout.isNegative() && !heartbeatVisibilityTimeout.isZero(),
				"heartbeatVisibilityTimeout must be greater than zero");
		this.heartbeatVisibilityTimeoutSeconds = (int) heartbeatVisibilityTimeout.getSeconds();
	}

	@Override
	public CompletableFuture<Void> emit(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		logger.trace("Adding visibility heartbeat interceptor for messages {}", MessageHeaderUtils.getId(messages));
		return getDelegate().emit(messages,
				context.addInterceptor(new MessageVisibilityHeartbeatInterceptor(messages)));
	}

	@Override
	public void start() {
		if (isRunning()) {
			return;
		}
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				return;
			}
			this.scheduler = createScheduler();
			super.start();
			this.running = true;
		}
	}

	@Override
	public void stop() {
		if (!isRunning()) {
			return;
		}
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				return;
			}
			this.running = false;
			ScheduledExecutorService localScheduler = this.scheduler;
			this.scheduler = null;
			if (localScheduler != null) {
				localScheduler.shutdownNow();
			}
			super.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running && super.isRunning();
	}

	protected ScheduledExecutorService createScheduler() {
		return Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory(THREAD_NAME_PREFIX));
	}

	private class MessageVisibilityHeartbeatInterceptor implements AsyncMessageInterceptor<T> {

		private final Collection<Message<T>> originalMessages;

		private final Map<String, ScheduledFuture<?>> runningMessageHeartbeats = new ConcurrentHashMap<>();

		@Nullable
		private ScheduledFuture<?> runningBatchHeartbeat;

		MessageVisibilityHeartbeatInterceptor(Collection<Message<T>> originalMessages) {
			this.originalMessages = Collections.unmodifiableCollection(new ArrayList<>(originalMessages));
		}

		@Override
		public CompletableFuture<Message<T>> intercept(Message<T> message) {
			String messageId = MessageHeaderUtils.getId(message);
			this.runningMessageHeartbeats.computeIfAbsent(messageId,
					id -> scheduleHeartbeat(() -> getMessageVisibility(message).changeToAsync(
							MessageVisibilityHeartbeatSinkAdapter.this.heartbeatVisibilityTimeoutSeconds), id));
			return CompletableFuture.completedFuture(message);
		}

		@Override
		public CompletableFuture<Collection<Message<T>>> intercept(Collection<Message<T>> messages) {
			if (this.runningBatchHeartbeat == null) {
				this.runningBatchHeartbeat = scheduleHeartbeat(
						() -> getBatchVisibility(messages).changeToAsync(
								MessageVisibilityHeartbeatSinkAdapter.this.heartbeatVisibilityTimeoutSeconds),
						MessageHeaderUtils.getId(messages));
			}
			return CompletableFuture.completedFuture(messages);
		}

		@Override
		public CompletableFuture<Void> afterProcessing(Message<T> message, @Nullable Throwable t) {
			String messageId = MessageHeaderUtils.getId(message);
			cancelHeartbeat(this.runningMessageHeartbeats.remove(messageId), messageId);
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> afterProcessing(Collection<Message<T>> messages, @Nullable Throwable t) {
			cancelHeartbeat(this.runningBatchHeartbeat, MessageHeaderUtils.getId(messages));
			this.runningBatchHeartbeat = null;
			this.originalMessages.forEach(msg -> {
				String id = MessageHeaderUtils.getId(msg);
				cancelHeartbeat(this.runningMessageHeartbeats.remove(id), id);
			});
			return CompletableFuture.completedFuture(null);
		}

		private ScheduledFuture<?> scheduleHeartbeat(Supplier<CompletableFuture<Void>> heartbeatAction, String id) {
			ScheduledExecutorService localScheduler = MessageVisibilityHeartbeatSinkAdapter.this.scheduler;
			Assert.state(localScheduler != null, "heartbeat scheduler not initialized");
			logger.trace("Scheduling visibility heartbeat for {} every {}ms", id,
					MessageVisibilityHeartbeatSinkAdapter.this.heartbeatInterval.toMillis());
			return localScheduler.scheduleAtFixedRate(() -> {
				try {
					heartbeatAction.get().whenComplete((v, t) -> {
						if (t != null) {
							logger.warn("Error sending visibility heartbeat for {}", id, t);
						}
						else {
							logger.trace("Visibility heartbeat sent for {}", id);
						}
					});
				}
				catch (Exception ex) {
					logger.warn("Error preparing visibility heartbeat for {}", id, ex);
				}
			}, MessageVisibilityHeartbeatSinkAdapter.this.heartbeatInterval.toMillis(),
					MessageVisibilityHeartbeatSinkAdapter.this.heartbeatInterval.toMillis(), TimeUnit.MILLISECONDS);
		}

		private void cancelHeartbeat(@Nullable ScheduledFuture<?> future, String id) {
			if (future != null) {
				future.cancel(true);
				logger.trace("Cancelled visibility heartbeat for {}", id);
			}
		}

		private QueueMessageVisibility getMessageVisibility(Message<T> message) {
			return MessageHeaderUtils.getHeader(message, SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER,
					QueueMessageVisibility.class);
		}

		@SuppressWarnings("unchecked")
		private BatchVisibility getBatchVisibility(Collection<Message<T>> messages) {
			QueueMessageVisibility visibility = getMessageVisibility(messages.iterator().next());
			return visibility.toBatchVisibility((Collection<Message<?>>) (Collection<?>) messages);
		}

	}

}
