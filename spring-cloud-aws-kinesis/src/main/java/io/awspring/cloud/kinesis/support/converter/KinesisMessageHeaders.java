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
package io.awspring.cloud.kinesis.support.converter;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
public final class KinesisMessageHeaders {

	private KinesisMessageHeaders() {
	}

	public static final String PREFIX = "Kinesis_";

	public static final String PARTITION_KEY = PREFIX + "partitionKey";

	public static final String SEQUENCE_NUMBER = PREFIX + "sequenceNumber";

	public static final String SUBSEQUENCE_NUMBER = PREFIX + "subSequenceNumber";

	public static final String SHARD_ID = PREFIX + "shardId";

	public static final String APPROXIMATE_ARRIVAL_TIMESTAMP = PREFIX + "approximateArrivalTimestamp";

	public static final String STREAM_NAME = PREFIX + "streamName";

	public static final String CHECKPOINTER = PREFIX + "checkpointer";

}
