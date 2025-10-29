/*
 * Copyright 2013-2022 the original author or authors.
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
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;

/**
 * An {@link AbstractDelegatingMessageListeningSinkAdapter} that adds an
 * {@link OriginalBatchMessageVisibilityExtendingInterceptor} to the {@link MessageProcessingContext}. The interceptor
 * refreshes the visibility for remaining messages in the batch before each message is processed, except for the first.
 * After each message is processed, it is removed from the copy of the original batch that the interceptor holds.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see io.awspring.cloud.sqs.listener.FifoSqsComponentFactory
 */
public class MessageVisibilityExtendingSinkAdapter<T> extends AbstractDelegatingMessageListeningSinkAdapter<T>
		implements SqsAsyncClientAware {

	private static final Logger logger = LoggerFactory.getLogger(MessageVisibilityExtendingSinkAdapter.class);

	private static final Duration DEFAULT_VISIBILITY_TO_SET = Duration.ofSeconds(30);

	private int messageVisibility = (int) DEFAULT_VISIBILITY_TO_SET.getSeconds();

	private SqsAsyncClient sqsAsyncClient;

	public MessageVisibilityExtendingSinkAdapter(MessageSink<T> delegate) {
		super(delegate);
	}

	public void setMessageVisibility(Duration messageVisibility) {
		Assert.notNull(messageVisibility, "visibilityDuration cannot be null");
		this.messageVisibility = (int) messageVisibility.getSeconds();
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
		return getDelegate().emit(messages,
				context.addInterceptor(new OriginalBatchMessageVisibilityExtendingInterceptor(messages)));
	}

	private CompletableFuture<Collection<Message<T>>> changeVisibility(Collection<Message<T>> messages) {
		logger.trace("Changing visibility of messages {} to {} seconds", MessageHeaderUtils.getId(messages),
				this.messageVisibility);
		return this.sqsAsyncClient
				.changeMessageVisibilityBatch(
						req -> req.entries(getEntries(messages)).queueUrl(getQueueUrl(messages)).build())
				.whenComplete((v, t) -> logResult(messages, t)).thenApply(theVoid -> messages);
	}

	private String getQueueUrl(Collection<Message<T>> messages) {
		return messages.iterator().next().getHeaders().get(SqsHeaders.SQS_QUEUE_URL_HEADER, String.class);
	}

	// @formatter:off
	private Collection<ChangeMessageVisibilityBatchRequestEntry> getEntries(Collection<Message<T>> messages) {
		return MessageHeaderUtils
			.getHeader(messages, SqsHeaders.SQS_RECEIPT_HANDLE_HEADER, String.class)
			.stream()
			.map(handle -> ChangeMessageVisibilityBatchRequestEntry.builder()
				.receiptHandle(handle).id(UUID.randomUUID().toString())
				.visibilityTimeout(this.messageVisibility).build())
			.collect(Collectors.toList());
	}
	// @formatter:on

	private void logResult(Collection<Message<T>> messages, Throwable t) {
		if (t == null) {
			logger.trace("Finished changing visibility for messages {}", MessageHeaderUtils.getId(messages));
		}
		else {
			logger.error("Error changing visibility for messages {}", MessageHeaderUtils.getId(messages));
		}
	}

	private class OriginalBatchMessageVisibilityExtendingInterceptor implements AsyncMessageInterceptor<T> {
		private final Map<String, Message<T>> originalMessageBatchMap;
		private final int initialBatchSize;

		private OriginalBatchMessageVisibilityExtendingInterceptor(Collection<Message<T>> originalMessageBatch) {
			this.initialBatchSize = originalMessageBatch.size();
			this.originalMessageBatchMap = Collections.synchronizedMap(originalMessageBatch.stream()
					.collect(Collectors.toMap(MessageHeaderUtils::getId, message -> message)));
		}

		@Override
		public CompletableFuture<Message<T>> intercept(Message<T> message) {
			if (this.originalMessageBatchMap.size() == this.initialBatchSize) {
				return CompletableFuture.completedFuture(message);
			}

			return changeVisibility(this.originalMessageBatchMap.values()).thenApply(response -> message);
		}

		@Override
		public CompletableFuture<Collection<Message<T>>> intercept(Collection<Message<T>> messages) {
			if (this.originalMessageBatchMap.size() == this.initialBatchSize) {
				return CompletableFuture.completedFuture(messages);
			}

			return changeVisibility(this.originalMessageBatchMap.values()).thenApply(response -> messages);
		}

		@Override
		public CompletableFuture<Void> afterProcessing(Collection<Message<T>> messages, Throwable t) {
			messages.forEach(message -> this.originalMessageBatchMap.remove(MessageHeaderUtils.getId(message)));
			return CompletableFuture.completedFuture(null);
		}

		@Override
		public CompletableFuture<Void> afterProcessing(Message<T> message, Throwable t) {
			this.originalMessageBatchMap.remove(MessageHeaderUtils.getId(message));
			return CompletableFuture.completedFuture(null);
		}

	}

}
