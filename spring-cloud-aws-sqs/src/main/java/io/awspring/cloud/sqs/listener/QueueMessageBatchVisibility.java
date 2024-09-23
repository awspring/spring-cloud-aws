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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;

/**
 * {@link BatchVisibility} implementation for SQS messages.
 *
 * @author Clement Denis
 * @author Tomaz Fernandes
 * @since 3.3
 */
public class QueueMessageBatchVisibility implements BatchVisibility {

	private static final Logger logger = LoggerFactory.getLogger(QueueMessageVisibility.class);

	private final SqsAsyncClient sqsAsyncClient;

	private final String queueUrl;

	private final Collection<String> receiptHandles;

	/**
	 * Create an instance for changing the visibility in batch for the provided queue.
	 */
	public QueueMessageBatchVisibility(SqsAsyncClient sqsAsyncClient, String queueUrl,
			Collection<String> receiptHandles) {
		this.sqsAsyncClient = sqsAsyncClient;
		this.queueUrl = queueUrl;
		this.receiptHandles = receiptHandles;
	}

	@Override
	public CompletableFuture<Void> changeToAsync(int seconds) {
		return changeToAsyncBatch(seconds);
	}

	private CompletableFuture<Void> changeToAsyncBatch(int seconds) {
		return CompletableFuture.allOf(CollectionUtils.partition(receiptHandles, 10).stream()
				.map(batch -> doChangeVisibility(seconds, batch).thenRun(() -> logger
						.trace("Changed the visibility of {} message to {} seconds", batch.size(), seconds)))
				.toArray(CompletableFuture[]::new));
	}

	private CompletableFuture<ChangeMessageVisibilityBatchResponse> doChangeVisibility(int seconds,
			Collection<String> batch) {
		return sqsAsyncClient
				.changeMessageVisibilityBatch(req -> req.queueUrl(queueUrl).entries(createEntries(seconds, batch)));
	}

	private List<ChangeMessageVisibilityBatchRequestEntry> createEntries(int seconds, Collection<String> batch) {
		return batch.stream().map(handle -> ChangeMessageVisibilityBatchRequestEntry.builder().receiptHandle(handle)
				.id(UUID.randomUUID().toString()).visibilityTimeout(seconds).build()).toList();
	}

}
