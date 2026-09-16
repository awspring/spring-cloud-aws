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
package io.awspring.cloud.sns.core.async;

import java.util.Map;
import org.springframework.messaging.Message;

/**
 * Converter for transforming Spring messages to SNS PublishRequest.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public interface SnsPublishMessageConverter {

	/**
	 * Converts a Spring Message to a PublishRequest.
	 *
	 * @param message the message to convert
	 * @param <T> the message payload type
	 * @return a pair containing the PublishRequest and the original message
	 */
	<T> PublishRequestMessagePair<T> convert(Message<T> message);

	/**
	 * Converts a payload and headers to a PublishRequest.
	 *
	 * @param payload the payload to convert
	 * @param headers the headers to include
	 * @param <T> the payload type
	 * @return a pair containing the PublishRequest and the constructed message
	 */
	<T> PublishRequestMessagePair<T> convert(T payload, Map<String, Object> headers);
}
