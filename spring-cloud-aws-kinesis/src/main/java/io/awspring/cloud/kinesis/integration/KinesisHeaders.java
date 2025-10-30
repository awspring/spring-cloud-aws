/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.integration;

/**
 * Constants for Kinesis message headers.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public final class KinesisHeaders {

	/**
	 * Kinesis headers prefix to be used by all headers added by the framework.
	 */
	public static final String PREFIX = "Kinesis_";

	/**
	 * The {@value STREAM} header for sending data to Kinesis.
	 */
	public static final String STREAM = PREFIX + "stream";

	/**
	 * The {@value RECEIVED_STREAM} header for receiving data from Kinesis.
	 */
	public static final String RECEIVED_STREAM = PREFIX + "receivedStream";

	/**
	 * The {@value PARTITION_KEY} header for sending data to Kinesis.
	 */
	public static final String PARTITION_KEY = PREFIX + "partitionKey";

	/**
	 * The {@value SEQUENCE_NUMBER} header for sending data to Kinesis.
	 */
	public static final String SEQUENCE_NUMBER = PREFIX + "sequenceNumber";

	/**
	 * The {@value SHARD} header to represent Kinesis shardId.
	 */
	public static final String SHARD = PREFIX + "shard";

	/**
	 * The {@value SERVICE_RESULT} header represents a
	 * {@link software.amazon.awssdk.services.kinesis.model.KinesisResponse}.
	 */
	public static final String SERVICE_RESULT = PREFIX + "serviceResult";

	/**
	 * The {@value RECEIVED_PARTITION_KEY} header for receiving data from Kinesis.
	 */
	public static final String RECEIVED_PARTITION_KEY = PREFIX + "receivedPartitionKey";

	/**
	 * The {@value RECEIVED_SEQUENCE_NUMBER} header for receiving data from Kinesis.
	 */
	public static final String RECEIVED_SEQUENCE_NUMBER = PREFIX + "receivedSequenceNumber";

	/**
	 * The {@value CHECKPOINTER} header for checkpoint the shard sequenceNumber.
	 */
	public static final String CHECKPOINTER = PREFIX + "checkpointer";

	/**
	 * The {@value RAW_RECORD} header represents received Kinesis record(s).
	 */
	public static final String RAW_RECORD = PREFIX + "rawRecord";

	private KinesisHeaders() {
	}

}
