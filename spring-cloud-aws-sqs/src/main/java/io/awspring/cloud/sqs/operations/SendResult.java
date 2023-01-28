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

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.messaging.Message;

/**
 * The result of a send operation.
 * @param messageId the message id as returned by the endpoint if successful, the id from the original {@link Message} if failed.
 * @param message the message that was sent, with any additional headers added by the framework.
 * @param additionalInformation additional information on the send operation.
 * @param <T> the message payload type.
 */
public record SendResult<T>(UUID messageId, String endpoint, Message<T> message, Map<String, Object> additionalInformation) {

	/**
	 * The result of a batch send operation.
	 * @param successful the {@link SendResult} for messages successfully sent.
	 * @param failed the {@link SendResult} for messages that failed to be sent.
	 * @param <T> the message payload type.
	 */
	public record Batch<T>(Collection<SendResult<T>> successful, Collection<SendResult.Failed<T>> failed) {}

	/**
	 * The result of a failed send operation.
	 * @param errorMessage a message with information on the error.
	 * @param <T> the message payload type.
	 */
	public record Failed<T> (String errorMessage, String endpoint, Message<T> message, Map<String, Object> additionalInformation) {}

}
