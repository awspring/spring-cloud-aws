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
package io.awspring.cloud.sqs.listener.acknowledgement;

import io.awspring.cloud.sqs.CompletableFutures;
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.QueueAttributes;
import io.awspring.cloud.sqs.listener.QueueAttributesAware;
import io.awspring.cloud.sqs.listener.SqsAsyncClientAware;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;

/**
 * {@link AcknowledgementExecutor} implementation for SQS queues. Handle the messages deletion, usually requested by an
 * {@link ExecutingAcknowledgementProcessor}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see ExecutingAcknowledgementProcessor
 */
public class SqsAcknowledgementExecutor<T>
		implements AcknowledgementExecutor<T>, SqsAsyncClientAware, QueueAttributesAware {

	private static final Logger logger = LoggerFactory.getLogger(SqsAcknowledgementExecutor.class);

	private SqsAsyncClient sqsAsyncClient;

	private String queueUrl;

	private String queueName;

	@Override
	public void setQueueAttributes(QueueAttributes queueAttributes) {
		Assert.notNull(queueAttributes, "queueAttributes cannot be null");
		this.queueUrl = queueAttributes.getQueueUrl();
		this.queueName = queueAttributes.getQueueName();
	}

	@Override
	public void setSqsAsyncClient(SqsAsyncClient sqsAsyncClient) {
		Assert.notNull(sqsAsyncClient, "sqsAsyncClient cannot be null");
		this.sqsAsyncClient = sqsAsyncClient;
	}

	@Override
	public CompletableFuture<Void> execute(Collection<Message<T>> messagesToAck) {
		try {
			logger.debug("Executing acknowledgement for {} messages", messagesToAck.size());
			Assert.notEmpty(messagesToAck, () -> "empty collection sent to acknowledge in queue " + this.queueName);
			return deleteMessages(messagesToAck);
		}
		catch (Exception e) {
			return CompletableFutures.failedFuture(createAcknowledgementException(messagesToAck, e));
		}
	}

	private SqsAcknowledgementException createAcknowledgementException(Collection<Message<T>> messagesToAck,
			Throwable e) {
		return new SqsAcknowledgementException(
				"Error acknowledging messages " + MessageHeaderUtils.getId(messagesToAck), messagesToAck, this.queueUrl,
				e);
	}

	// @formatter:off
	private CompletableFuture<Void> deleteMessages(Collection<Message<T>> messagesToAck) {
		logger.trace("Acknowledging messages for queue {}: {}", this.queueName,
				MessageHeaderUtils.getId(messagesToAck));
		StopWatch watch = new StopWatch();
		watch.start();
		return CompletableFutures.exceptionallyCompose(this.sqsAsyncClient
			.deleteMessageBatch(createDeleteMessageBatchRequest(messagesToAck))
			.thenRun(() -> {}),
				t -> CompletableFutures.failedFuture(createAcknowledgementException(messagesToAck, t)))
			.whenComplete((v, t) -> logAckResult(messagesToAck, t, watch));
	}

	private DeleteMessageBatchRequest createDeleteMessageBatchRequest(Collection<Message<T>> messagesToAck) {
		return DeleteMessageBatchRequest
			.builder()
			.queueUrl(this.queueUrl)
			.entries(messagesToAck.stream().map(this::toDeleteMessageEntry).collect(Collectors.toList()))
			.build();
	}

	private DeleteMessageBatchRequestEntry toDeleteMessageEntry(Message<T> message) {
		return DeleteMessageBatchRequestEntry
			.builder()
			.receiptHandle(MessageHeaderUtils.getHeaderAsString(message, SqsHeaders.SQS_RECEIPT_HANDLE_HEADER))
			.id(UUID.randomUUID().toString())
			.build();
	}
	// @formatter:on

	private void logAckResult(Collection<Message<T>> messagesToAck, Throwable t, StopWatch watch) {
		watch.stop();
		long totalTimeMillis = watch.getTotalTimeMillis();
		if (totalTimeMillis > 1000) {
			logger.warn("Acknowledgement operation took {} seconds to finish in queue {} for messages {}",
					totalTimeMillis, this.queueName, MessageHeaderUtils.getId(messagesToAck));
		}
		if (t != null) {
			logger.error("Error acknowledging in queue {} messages {} in {}ms", this.queueName,
					MessageHeaderUtils.getId(messagesToAck), totalTimeMillis, t);
		}
		else {
			logger.trace("Done acknowledging in queue {} messages: {} in {}ms", this.queueName,
					MessageHeaderUtils.getId(messagesToAck), totalTimeMillis);
		}
	}

}
