/*
 * Copyright 2022 the original author or authors.
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
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.SqsAsyncClientAware;
import io.awspring.cloud.sqs.listener.SqsMessageHeaders;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageVisibilityExtendingSinkAdapter<T> extends AbstractDelegatingMessageListeningSinkAdapter<T> implements SqsAsyncClientAware {

	private static final Logger logger = LoggerFactory.getLogger(MessageVisibilityExtendingSinkAdapter.class);

	private static final Duration DEFAULT_VISIBILITY_TO_SET = Duration.ofSeconds(30);

	private static final Strategy DEFAULT_VISIBILITY_STRATEGY = Strategy.MESSAGES_BEING_PROCESSED;

	private int messageVisibility = (int) DEFAULT_VISIBILITY_TO_SET.getSeconds();

	private Strategy visibilityStrategy = DEFAULT_VISIBILITY_STRATEGY;

	private SqsAsyncClient sqsAsyncClient;

	public MessageVisibilityExtendingSinkAdapter(MessageSink<T> delegate) {
		super(delegate);
	}

	public void setMessageVisibility(Duration messageVisibility) {
		Assert.notNull(messageVisibility, "visibilityDuration cannot be null");
		this.messageVisibility = (int) messageVisibility.getSeconds();
	}

	public void setVisibilityStrategy(Strategy strategy) {
		Assert.notNull(strategy, "visibilityStrategy cannot be null");
		this.visibilityStrategy = strategy;
	}

	@Override
	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null");
		super.setSqsAsyncClient(sqsAsyncClient);
		this.sqsAsyncClient = sqsAsyncClient;
	}

	@Override
	public CompletableFuture<Void> emit(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		logger.trace("Adding visibility interceptor for messages {}", MessageHeaderUtils.getId(messages));
		return Strategy.ONCE_ON_RECEIVE.equals(this.visibilityStrategy)
			? changeVisibility(messages).thenCompose(msgs -> getDelegate().emit(msgs, context))
			: getDelegate().emit(messages, context.addInterceptor(createInterceptor(messages)));
	}

	private AsyncMessageInterceptor<T> createInterceptor(Collection<Message<T>> messages) {
		return Strategy.MESSAGES_BEING_PROCESSED.equals(this.visibilityStrategy)
			? new ProcessingMessageVisibilityExtendingInterceptor()
			: new OriginalBatchMessageVisibilityExtendingInterceptor(messages);
	}

	private CompletableFuture<Collection<Message<T>>> changeVisibility(Collection<Message<T>> messages) {
		logger.trace("Changing visibility of messages {} to {} seconds", MessageHeaderUtils.getId(messages),
			this.messageVisibility);
		CompletableFuture<Object> result = new CompletableFuture<>();
		StopWatch watch = new StopWatch();
		watch.start();
		return this.sqsAsyncClient.changeMessageVisibilityBatch(req -> req
				.entries(getEntries(messages))
				.queueUrl(getQueueUrl(messages))
				.build())
			.whenComplete((v, t) -> {
					watch.stop();
					if (t == null) {
						logger.trace("Visibility change took {}ms for messages {}", watch.getTotalTimeMillis(),
							MessageHeaderUtils.getId(messages));
					} else {
						logger.error("Error changing visibility for messages {} after {}ms",
							MessageHeaderUtils.getId(messages), watch.getTotalTimeMillis());
					}
				}
			).thenApply(theVoid -> messages);
	}

	private String getQueueUrl(Collection<Message<T>> messages) {
		return MessageHeaderUtils.getHeader(messages.iterator().next(), SqsMessageHeaders.SQS_QUEUE_URL, String.class);
	}

	private Collection<ChangeMessageVisibilityBatchRequestEntry> getEntries(Collection<Message<T>> messages) {
		return MessageHeaderUtils
			.getHeader(messages, SqsMessageHeaders.RECEIPT_HANDLE_MESSAGE_ATTRIBUTE_NAME, String.class)
			.stream()
			.map(handle -> ChangeMessageVisibilityBatchRequestEntry.builder()
				.receiptHandle(handle).id(UUID.randomUUID().toString())
				.visibilityTimeout(this.messageVisibility).build())
			.collect(Collectors.toList());
	}

	private class OriginalBatchMessageVisibilityExtendingInterceptor implements AsyncMessageInterceptor<T> {

		private final Collection<Message<T>> originalMessageBatchCopy;

		private OriginalBatchMessageVisibilityExtendingInterceptor(Collection<Message<T>> originalMessageBatch) {
			this.originalMessageBatchCopy = Collections.synchronizedCollection(new ArrayList<>(originalMessageBatch));
		}

		@Override
		public CompletableFuture<Message<T>> intercept(Message<T> message) {
			return changeVisibility(this.originalMessageBatchCopy).thenApply(response -> message);
		}

		@Override
		public CompletableFuture<Collection<Message<T>>> intercept(Collection<Message<T>> messages) {
			return changeVisibility(this.originalMessageBatchCopy).thenApply(response -> messages);
		}

		@Override
		public CompletableFuture<Collection<Message<T>>> afterProcessing(Collection<Message<T>> messages) {
			this.originalMessageBatchCopy.removeAll(messages);
			return CompletableFuture.completedFuture(messages);
		}

		@Override
		public CompletableFuture<Message<T>> afterProcessing(Message<T> message) {
			this.originalMessageBatchCopy.remove(message);
			return CompletableFuture.completedFuture(message);
		}

	}

	private class ProcessingMessageVisibilityExtendingInterceptor implements AsyncMessageInterceptor<T> {

		@Override
		public CompletableFuture<Message<T>> intercept(Message<T> message) {
			CompletableFuture<Message<T>> result = new CompletableFuture<>();
			changeVisibility(Collections.singletonList(message)).whenComplete((v, t) -> {
				if (t == null) {
					result.complete(message);
				}
				else {
					result.completeExceptionally(t);
				}
			});
			CompletableFuture<Message<T>> completedFuture = CompletableFuture.completedFuture(message);
			return result.thenCombine(completedFuture, (msg1, msg2) -> msg1);
		}

		@Override
		public CompletableFuture<Collection<Message<T>>> intercept(Collection<Message<T>> messages) {
			return changeVisibility(messages).thenApply(response -> messages);
		}

	}

	public enum Strategy {

		MESSAGES_BEING_PROCESSED,

		REMAINING_ORIGINAL_BATCH_MESSAGES,

		ONCE_ON_RECEIVE

	}

}
