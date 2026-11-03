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
 * A shard a {@link KclMessageListenerContainer} has halted and not yet recovered, identified by its stream and shard
 * id. The stream is part of the identity because a multi-stream application processes shards of several streams under
 * one container, and different streams can have a shard with the same id.
 *
 * @param streamName the stream the halted shard belongs to.
 * @param shardId the halted shard.
 * @author Matej Nedic
 * @since 4.3.0
 */
public record HaltedShard(String streamName, String shardId) {
}
