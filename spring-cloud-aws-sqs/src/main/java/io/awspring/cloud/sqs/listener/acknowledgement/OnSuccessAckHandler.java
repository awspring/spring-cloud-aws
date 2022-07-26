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

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.SqsException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;

/**
 * Default {@link AckHandler} implementation that only acknowledges on success.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class OnSuccessAckHandler<T> implements AckHandler<T> {

	private static final Logger logger = LoggerFactory.getLogger(OnSuccessAckHandler.class);

	@Override
	public CompletableFuture<Void> onSuccess(Message<T> message) {
		logger.trace("Acknowledging message {}", MessageHeaderUtils.getId(message));
		try {
			return MessageHeaderUtils.getAcknowledgement(message).acknowledge()
				.thenRun(() -> logger.trace("Message {} acknowledged", MessageHeaderUtils.getId(message)))
				.exceptionally(t -> logError(message, t));
		}
		catch (Exception e) {
			logError(message, e);
			throw new SqsException("Error acknowledging message", e);
		}
	}

	@Override
	public CompletableFuture<Void> onSuccess(Collection<Message<T>> messages) {
		if (messages.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		// TODO: Rewrite this logic as part of the AckHandler design
		List<SqsAcknowledge> acks = messages.stream().map(MessageHeaderUtils::getAcknowledgement)
				.map(SqsAcknowledge.class::cast).collect(Collectors.toList());
		String queueUrl = acks.get(0).getQueueUrl();
		SqsAsyncClient client = acks.get(0).getSqsAsyncClient();
		List<String> handles = acks.stream().map(SqsAcknowledge::getReceiptHandle).collect(Collectors.toList());
		return client
				.deleteMessageBatch(req -> req.queueUrl(queueUrl)
						.entries(handles.stream().map(this::toDeleteRequest).collect(Collectors.toList())).build())
				.thenRun(() -> logger.trace("Acknowledged messages {}", MessageHeaderUtils.getId(messages)));
	}

	private DeleteMessageBatchRequestEntry toDeleteRequest(String handle) {
		return DeleteMessageBatchRequestEntry.builder().receiptHandle(handle).id(UUID.randomUUID().toString()).build();
	}

	private Void logError(Message<T> message, Throwable t) {
		logger.error("Error acknowledging message {}", MessageHeaderUtils.getId(message), t);
		return null;
	}

}
