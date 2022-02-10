/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.v3.messaging.core;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.messaging.MessageHeaders;

/**
 * Specialization of the {@link MessageHeaders} class that allows to set an ID. This was
 * done to support cases where the ID sent by the producer must be restored on the
 * consumer side for traceability.
 *
 * @author Alain Sahli
 * @author Wojciech Mąka
 * @since 1.0
 */
public class SqsMessageHeaders extends MessageHeaders {

	/**
	 * Delay header in a SQS message.
	 */
	public static final String SQS_DELAY_HEADER = "delay";

	/**
	 * Group id header in a SQS message.
	 */
	public static final String SQS_GROUP_ID_HEADER = "message-group-id";

	/**
	 * Deduplication header in a SQS message.
	 */
	public static final String SQS_DEDUPLICATION_ID_HEADER = "message-deduplication-id";

	/**
	 * ApproximateFirstReceiveTimestamp header in a SQS message.
	 */
	public static final String SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP = "ApproximateFirstReceiveTimestamp";

	/**
	 * ApproximateReceiveCount header in a SQS message.
	 */
	public static final String SQS_APPROXIMATE_RECEIVE_COUNT = "ApproximateReceiveCount";

	/**
	 * SentTimestamp header in a SQS message.
	 */
	public static final String SQS_SENT_TIMESTAMP = "SentTimestamp";

	public SqsMessageHeaders(Map<String, Object> headers) {
		super(headers, getId(headers), getTimestamp(headers));
	}

	public Long getApproximateFirstReceiveTimestamp() {
		return containsKey(SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP)
				? Long.parseLong(Objects.requireNonNull(get(SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP, String.class)))
				: null;
	}

	public Long getSentTimestamp() {
		return getTimestamp(getRawHeaders());
	}

	public Long getApproximateReceiveCount() {
		return containsKey(SQS_APPROXIMATE_RECEIVE_COUNT)
				? Long.parseLong(Objects.requireNonNull(get(SQS_APPROXIMATE_RECEIVE_COUNT, String.class))) : null;
	}

	private static Long getTimestamp(Map<String, Object> headers) {
		return headers.containsKey(SQS_SENT_TIMESTAMP)
				? Long.parseLong(Objects.requireNonNull((String) headers.get(SQS_SENT_TIMESTAMP))) : null;
	}

	private static UUID getId(Map<String, Object> headers) {
		return headers.containsKey(ID) ? (UUID) headers.get(ID) : null;
	}

}
