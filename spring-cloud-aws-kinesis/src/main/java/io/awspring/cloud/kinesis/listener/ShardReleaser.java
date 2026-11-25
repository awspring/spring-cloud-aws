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
package io.awspring.cloud.kinesis.listener;

/**
 * Gives up the lease of a halted shard so that KCL reassigns it and the shard is processed again from its last
 * checkpoint, without restarting the application.
 * <p>
 * A {@link KclShardRecordProcessor} schedules a release after the configured
 * {@link KclContainerOptions.Builder#haltedShardReleaseDelay(java.time.Duration) delay} once its shard halts. The
 * release runs on a container thread rather than the shard thread, because a halted and idle shard may never receive
 * another {@code processRecords} call to trigger it from.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
@FunctionalInterface
interface ShardReleaser {

	/**
	 * Give up the lease identified by the given key. Looking up a lease that is no longer held, or releasing it while
	 * the scheduler is shutting down, is a no-op.
	 * @param leaseKey the shard id for a single-stream application, or {@code <serialized StreamIdentifier>:<shardId>}
	 *     for a multi-stream one.
	 */
	void releaseLease(String leaseKey);

}
