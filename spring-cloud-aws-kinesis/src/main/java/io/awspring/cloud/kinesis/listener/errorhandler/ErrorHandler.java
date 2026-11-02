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
package io.awspring.cloud.kinesis.listener.errorhandler;

/**
 * Decides what happens to a record (or batch) whose listener invocation or conversion failed. This is the single
 * extension point for retry and dead-letter policies: the framework itself never retries a listener and never lets an
 * exception escape to the Kinesis Client Library, which would silently skip the records.
 * <p>
 * The outcome is signalled by how {@link #handle(FailedDelivery)} completes:
 * <ul>
 * <li><b>Returns normally</b> - the failure is resolved (for example the retry succeeded, or the record was sent to a
 * dead-letter destination). Processing continues with the next record and checkpointing proceeds past the failed one
 * according to the checkpoint mode.</li>
 * <li><b>Throws</b> - the failure is unresolved. The shard is halted: the remaining records of the batch are not
 * delivered, no further checkpoint is taken and later batches for the shard are ignored. The records after the last
 * checkpoint are redelivered once the shard lease is acquired again, after an application restart or when the lease
 * moves to another worker.</li>
 * </ul>
 * Retries are implemented by calling {@link FailedDelivery#retry()}, which re-invokes the listener on the shard thread.
 * Blocking in the handler blocks only the failed shard, preserving per-shard ordering.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@FunctionalInterface
public interface ErrorHandler {

	/**
	 * Handle a failed delivery.
	 * @param failedDelivery the failed delivery.
	 * @throws Exception to leave the failure unresolved and halt the shard.
	 */
	void handle(FailedDelivery failedDelivery) throws Exception;

}
