/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.sqs.operations;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.*;

/**
 * An {@link SqsAsyncClient} adapter that provides automatic batching capabilities using AWS SDK's
 * {@link SqsAsyncBatchManager}.
 * 
 * <p>
 * This adapter automatically batches SQS operations to improve performance and reduce costs by combining multiple
 * requests into fewer AWS API calls. All standard SQS operations are supported: send message, receive message, delete
 * message, and change message visibility.
 *
 * <p>
 * <strong>Important - Asynchronous Behavior:</strong> This adapter processes requests asynchronously through
 * batching. The returned {@link CompletableFuture} reflects the batching operation,
 * not the final transmission to AWS SQS. This can lead to false positives where the operation appears successful locally but fails during actual transmission.
 * The actual transmission happens in a background thread, up to the configured {@code sendRequestFrequency} after enqueuing.
 * Applications must:
 * <ul>
 * <li>Handle the returned {@link CompletableFuture} to detect transmission errors.
 * Calling {@code .join()} will block until the batch is sent (up to {@code sendRequestFrequency}),
 * while {@code .exceptionally()} or {@code .handle()} are required for non-blocking error handling.</li>
 * <li>Implement appropriate error handling, monitoring, and retry mechanisms for critical operations.</li>
 * </ul>
 *
 * <p>
 * <strong>Batch Optimization:</strong> The underlying {@code SqsAsyncBatchManager} from the AWS SDK bypasses batching
 * for {@code receiveMessage} calls that include per-request configurations for certain parameters. To ensure batching
 * is not bypassed, it is recommended to configure these settings globally on the {@code SqsAsyncBatchManager} builder
 * instead of on each {@code ReceiveMessageRequest}. The parameters that trigger this bypass are:
 * <ul>
 * <li>{@code messageAttributeNames}</li>
 * <li>{@code messageSystemAttributeNames}</li>
 * <li>{@code messageSystemAttributeNamesWithStrings}</li>
 * <li>{@code overrideConfiguration}</li>
 * </ul>
 * <p>
 * By configuring these globally on the manager, you ensure consistent batching performance. If you require per-request
 * attribute configurations, using the standard {@code SqsAsyncClient} without this adapter may be more appropriate.
 * @author Heechul Kang
 * @since 4.0.0
 * @see SqsAsyncBatchManager
 * @see SqsAsyncClient
 */
public class BatchingSqsClientAdapter implements SqsAsyncClient {
	private final SqsAsyncBatchManager batchManager;

	/**
	 * Creates a new {@code BatchingSqsClientAdapter} with the specified batch manager.
	 * 
	 * @param batchManager the {@link SqsAsyncBatchManager} to use for batching operations
	 * @throws IllegalArgumentException if batchManager is null
	 */
	public BatchingSqsClientAdapter(SqsAsyncBatchManager batchManager) {
		Assert.notNull(batchManager, "batchManager cannot be null");
		this.batchManager = batchManager;
	}

	@Override
	public String serviceName() {
		return SqsAsyncClient.SERVICE_NAME;
	}

	/**
	 * Closes the underlying batch manager and releases associated resources.
	 * 
	 * <p>
	 * This method should be called when the adapter is no longer needed to ensure proper cleanup of threads and
	 * connections.
	 */
	@Override
	public void close() {
		batchManager.close();
	}

	/**
	 * Sends a message to the specified SQS queue using automatic batching.
	 * 
	 * <p>
	 * <strong>Important:</strong> This method returns immediately, but the actual sending is performed asynchronously.
	 * Handle the returned {@link CompletableFuture} to detect transmission errors.
	 * 
	 * @param sendMessageRequest the request containing queue URL and message details
	 * @return a {@link CompletableFuture} that completes with the send result
	 */
	@Override
	public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) {
		return batchManager.sendMessage(sendMessageRequest);
	}

	/**
	 * Sends a message to the specified SQS queue using automatic batching.
	 * 
	 * <p>
	 * <strong>Important:</strong> This method returns immediately, but the actual sending is performed asynchronously.
	 * Handle the returned {@link CompletableFuture} to detect transmission errors.
	 * 
	 * @param sendMessageRequest a consumer to configure the send message request
	 * @return a {@link CompletableFuture} that completes with the send result
	 */
	@Override
	public CompletableFuture<SendMessageResponse> sendMessage(Consumer<SendMessageRequest.Builder> sendMessageRequest) {
		return batchManager.sendMessage(sendMessageRequest);
	}

	/**
	 * Receives messages from the specified SQS queue using automatic batching.
	 * 
	 * <p>
	 * The batching manager may combine multiple receive requests to optimize AWS API usage.
	 * 
	 * @param receiveMessageRequest the request containing queue URL and receive options
	 * @return a {@link CompletableFuture} that completes with the received messages
	 */
	@Override
	public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
		return batchManager.receiveMessage(receiveMessageRequest);
	}

	/**
	 * Receives messages from the specified SQS queue using automatic batching.
	 * 
	 * <p>
	 * The batching manager may combine multiple receive requests to optimize AWS API usage.
	 * 
	 * @param receiveMessageRequest a consumer to configure the receive message request
	 * @return a {@link CompletableFuture} that completes with the received messages
	 */
	@Override
	public CompletableFuture<ReceiveMessageResponse> receiveMessage(
			Consumer<ReceiveMessageRequest.Builder> receiveMessageRequest) {
		return batchManager.receiveMessage(receiveMessageRequest);
	}

	/**
	 * Deletes a message from the specified SQS queue using automatic batching.
	 * 
	 * <p>
	 * <strong>Important:</strong> The actual deletion may be delayed due to batching. Handle the returned
	 * {@link CompletableFuture} to confirm successful deletion.
	 * 
	 * @param deleteMessageRequest the request containing queue URL and receipt handle
	 * @return a {@link CompletableFuture} that completes with the deletion result
	 */
	@Override
	public CompletableFuture<DeleteMessageResponse> deleteMessage(DeleteMessageRequest deleteMessageRequest) {
		return batchManager.deleteMessage(deleteMessageRequest);
	}

	/**
	 * Deletes a message from the specified SQS queue using automatic batching.
	 * 
	 * <p>
	 * <strong>Important:</strong> The actual deletion may be delayed due to batching. Handle the returned
	 * {@link CompletableFuture} to confirm successful deletion.
	 * 
	 * @param deleteMessageRequest a consumer to configure the delete message request
	 * @return a {@link CompletableFuture} that completes with the deletion result
	 */
	@Override
	public CompletableFuture<DeleteMessageResponse> deleteMessage(
			Consumer<DeleteMessageRequest.Builder> deleteMessageRequest) {
		return batchManager.deleteMessage(deleteMessageRequest);
	}

	/**
	 * Changes the visibility timeout of a message in the specified SQS queue using automatic batching.
	 * 
	 * <p>
	 * The batching manager may combine multiple visibility change requests to optimize AWS API usage.
	 * 
	 * @param changeMessageVisibilityRequest the request containing queue URL, receipt handle, and new timeout
	 * @return a {@link CompletableFuture} that completes with the visibility change result
	 */
	@Override
	public CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(
			ChangeMessageVisibilityRequest changeMessageVisibilityRequest) {
		return batchManager.changeMessageVisibility(changeMessageVisibilityRequest);
	}

	/**
	 * Changes the visibility timeout of a message in the specified SQS queue using automatic batching.
	 * 
	 * <p>
	 * The batching manager may combine multiple visibility change requests to optimize AWS API usage.
	 * 
	 * @param changeMessageVisibilityRequest a consumer to configure the change visibility request
	 * @return a {@link CompletableFuture} that completes with the visibility change result
	 */
	@Override
	public CompletableFuture<ChangeMessageVisibilityResponse> changeMessageVisibility(
			Consumer<ChangeMessageVisibilityRequest.Builder> changeMessageVisibilityRequest) {
		return batchManager.changeMessageVisibility(changeMessageVisibilityRequest);
	}
}
