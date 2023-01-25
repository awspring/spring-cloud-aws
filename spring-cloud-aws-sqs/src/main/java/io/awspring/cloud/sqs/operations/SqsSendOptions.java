/*
 * Copyright 2013-2023 the original author or authors.
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

import java.util.Map;
import java.util.UUID;

/**
 * Options for sending messages to SQS queues, with a method chaining API.
 * @param <T> the payload type.
 */
public interface SqsSendOptions<T> {

	/**
	 * Set the queue name or url to send the message to.
	 * @param queue the queue name.
	 * @return the options instance.
	 */
	SqsSendOptions<T> queue(String queue);

	/**
	 * Set the payload to send in the message. The payload will be serialized if necessary.
	 * @param payload the payload.
	 * @return the options instance.
	 */
	SqsSendOptions<T> payload(T payload);

	/**
	 * Add a header to be sent in the message. The header will be sent as a MessageAttribute.
	 *
	 * @param headerName the header name.
	 * @param headerValue the header value.
	 * @return the options instance.
	 */
	SqsSendOptions<T> header(String headerName, Object headerValue);

	/**
	 * Add headers to be sent in the message. The headers will be sent as MessageAttributes.
	 *
	 * @param headers the headers to add.
	 * @return the options instance.
	 */
	SqsSendOptions<T> headers(Map<String, Object> headers);

	/**
	 * Set a delay for the message in seconds.
	 * @param delaySeconds the delay in seconds.
	 * @return the options instance.
	 */
	SqsSendOptions<T> delaySeconds(Integer delaySeconds);

	/**
	 * Set the messageGroupId for the message. If none is provided for a FIFO queue, a random one is added.
	 * @param messageGroupId the id.
	 * @return the options instance.
	 */
	SqsSendOptions<T> messageGroupId(UUID messageGroupId);

	/**
	 * Set the messageDeduplicationId for the message. If none is provided for a FIFO queue, a random one is added.
	 * @param messageDeduplicationId the id.
	 * @return the options instance.
	 */
	SqsSendOptions<T> messageDeduplicationId(UUID messageDeduplicationId);

}
