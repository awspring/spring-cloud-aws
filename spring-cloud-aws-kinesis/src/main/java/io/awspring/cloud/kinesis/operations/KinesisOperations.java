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
package io.awspring.cloud.kinesis.operations;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Operations for sending records to Kinesis Data Streams.
 * <p>
 * Each method has a synchronous variant that blocks until Kinesis has acknowledged the record and an {@code Async}
 * variant returning a {@link CompletableFuture}. Payloads are serialized by the configured
 * {@link io.awspring.cloud.kinesis.support.converter.KinesisMessageConverter}.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public interface KinesisOperations {

	/**
	 * Sends a record with a random partition key, so it is distributed across the stream's shards.
	 * @param streamName the target stream.
	 * @param payload the payload to serialize and send.
	 * @return the sequence number and shard the record was written to.
	 */
	SendResult send(String streamName, Object payload);

	/**
	 * Sends a record with the given partition key, which determines the shard and therefore the ordering group.
	 * @param streamName the target stream.
	 * @param partitionKey the partition key.
	 * @param payload the payload to serialize and send.
	 * @return the sequence number and shard the record was written to.
	 */
	SendResult send(String streamName, String partitionKey, Object payload);

	/**
	 * Sends the given record, allowing all record attributes to be set.
	 * @param request the record to send.
	 * @return the sequence number and shard the record was written to.
	 */
	SendResult send(SendRequest request);

	/**
	 * Asynchronously sends a record with the given partition key.
	 * @param streamName the target stream.
	 * @param partitionKey the partition key.
	 * @param payload the payload to serialize and send.
	 * @return a future completing with the sequence number and shard the record was written to.
	 */
	CompletableFuture<SendResult> sendAsync(String streamName, String partitionKey, Object payload);

	/**
	 * Asynchronously sends the given record.
	 * @param request the record to send.
	 * @return a future completing with the sequence number and shard the record was written to.
	 */
	CompletableFuture<SendResult> sendAsync(SendRequest request);

	/**
	 * Sends any number of records to the same stream, splitting them into {@code PutRecords} calls of at most 500
	 * records and 5 MiB each, a conservative size below the current 10 MiB {@code PutRecords} limit, and merging the
	 * results in request order. Individual records can fail while a call as a whole succeeds, so the result reports
	 * successful and failed records separately.
	 * @param streamName the target stream, which every request must target.
	 * @param requests the records to send.
	 * @return the successful and failed records of the batch, in request order.
	 */
	BatchSendResult sendBatch(String streamName, List<SendRequest> requests);

	/**
	 * Asynchronously sends any number of records to the same stream, splitting them into {@code PutRecords} calls of at
	 * most 500 records and 5 MiB each, a conservative size below the current 10 MiB {@code PutRecords} limit.
	 * @param streamName the target stream, which every request must target.
	 * @param requests the records to send.
	 * @return a future completing with the successful and failed records of the batch, in request order.
	 */
	CompletableFuture<BatchSendResult> sendBatchAsync(String streamName, List<SendRequest> requests);

}
