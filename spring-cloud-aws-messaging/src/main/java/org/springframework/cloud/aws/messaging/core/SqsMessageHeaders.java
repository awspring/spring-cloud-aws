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

package org.springframework.cloud.aws.messaging.core;

import java.util.Map;

import org.springframework.messaging.MessageHeaders;

/**
 * Specialization of the {@link MessageHeaders} class that allows to set an ID. This was
 * done to support cases where the ID sent by the producer must be restored on the
 * consumer side for traceability.
 *
 * @author Alain Sahli
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

	public SqsMessageHeaders(Map<String, Object> headers) {
		super(headers);

		if (headers.containsKey(ID)) {
			this.getRawHeaders().put(ID, headers.get(ID));
		}
	}

}
