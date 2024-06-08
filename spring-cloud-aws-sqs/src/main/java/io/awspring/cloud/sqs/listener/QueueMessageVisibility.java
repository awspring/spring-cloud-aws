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
package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.CollectionUtils;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;

/**
 * {@link Visibility} implementation for SQS messages.
 *
 * @author Szymon Dembek
 * @author Tomaz Fernandes
 * @since 1.3
 */
public class QueueMessageVisibility implements Visibility {

	private static final Logger logger = LoggerFactory.getLogger(QueueMessageVisibility.class);

	private final SqsAsyncClient sqsAsyncClient;

	private final String queueUrl;

	private final String receiptHandle;

	/**
	 * Create an instance for changing the visibility for the provided queue.
	 * @param amazonSqsAsync the client to be used.
	 * @param queueUrl the queue url.
	 * @param receiptHandle the message receipt handle.
	 */
	public QueueMessageVisibility(SqsAsyncClient amazonSqsAsync, String queueUrl, String receiptHandle) {
		this.sqsAsyncClient = amazonSqsAsync;
		this.queueUrl = queueUrl;
		this.receiptHandle = receiptHandle;
	}

	@Override
	public CompletableFuture<Void> changeToAsync(int seconds) {
		return this.sqsAsyncClient
				.changeMessageVisibility(
						req -> req.queueUrl(this.queueUrl).receiptHandle(this.receiptHandle).visibilityTimeout(seconds))
				.thenRun(() -> logger.trace("Changed the visibility of message {} to {} seconds", this.receiptHandle,
						seconds));
	}

	// TODO this is used by QueueMessageBatchVisibility to change visibility of multiple messages.
	// This design is far from ideal, but it is very simple and minimizes changes.
	// It is public so BatchVisibilityArgumentResolverTests can access the method.
	public CompletableFuture<Void> changeToAsyncBatch(int seconds, Collection<? extends Message<?>> messages) {
		CollectionUtils.partition(messages, 10).forEach(batch -> sqsAsyncClient
				.changeMessageVisibilityBatch(req -> req.queueUrl(queueUrl)
						.entries(batch.stream().map(QueueMessageVisibility::fromMessage)
								.map(v -> ChangeMessageVisibilityBatchRequestEntry.builder()
										.receiptHandle(v.receiptHandle).visibilityTimeout(seconds).build())
								.toList()))
				.thenRun(() -> logger.trace("Changed the visibility of {} message to {} seconds", batch.size(),
						seconds)));
		return CompletableFuture.completedFuture(null);
	}

	public static QueueMessageVisibility fromMessage(Message<?> message) {
		Object visibilityObject = message.getHeaders().get(SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER);
		if (visibilityObject == null) {
			throw new IllegalArgumentException("No visibility object found for message header: '"
					+ SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER + "'");
		}
		if (!(visibilityObject instanceof QueueMessageVisibility)) {
			throw new IllegalArgumentException(
					"Visibility object not of expected  " + QueueMessageBatchVisibility.class);
		}
		return (QueueMessageVisibility) visibilityObject;
	}

}
