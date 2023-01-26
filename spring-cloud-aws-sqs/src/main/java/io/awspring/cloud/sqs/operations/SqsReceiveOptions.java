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

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Options for receiving messages from SQS queues, with a method chaining API.
 */
public interface SqsReceiveOptions {

	/**
	 * Set the queue name or url from which to receive messages from.
	 * @param queue the queue name.
	 * @return the options instance.
	 */
	SqsReceiveOptions queue(String queue);

	/**
	 * Set the maximum amount of time to wait for messages in the queue before returning with less than maximum of
	 * messages or empty.
	 * @param pollTimeout the amount of time.
	 * @return the options instance.
	 */
	SqsReceiveOptions pollTimeout(Duration pollTimeout);

	/**
	 * Set the visibility timeout to be applied by received messages.
	 * @param visibilityTimeout the timeout.
	 * @return the options instance.
	 */
	SqsReceiveOptions visibilityTimeout(Duration visibilityTimeout);

	/**
	 * Provide a header name and value to be added to returned messages.
	 * @param name the header name.
	 * @param value the header value.
	 * @return the options instance.
	 */
	SqsReceiveOptions additionalHeader(String name, Object value);

	/**
	 * Provide headers to be added to returned messages.
	 * @param headers the headers to add.
	 * @return the options instance.
	 */
	SqsReceiveOptions additionalHeaders(Map<String, Object> headers);

	/**
	 * Set the maximum number of messages to be returned.
	 * @param maxNumberOfMessages the number of messages.
	 * @return the options instance.
	 */
	SqsReceiveOptions maxNumberOfMessages(Integer maxNumberOfMessages);

	/**
	 * Set the receiveRequestAttemptId required attribute. If none is provided for a FIFO queue a random one is
	 * generated.
	 * @param receiveRequestAttemptId the id.
	 * @return the options instance.
	 */
	SqsReceiveOptions receiveRequestAttemptId(UUID receiveRequestAttemptId);

}
