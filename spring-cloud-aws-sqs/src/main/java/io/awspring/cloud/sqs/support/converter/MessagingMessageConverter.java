/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sqs.support.converter;

import org.springframework.messaging.Message;

/**
 * A converter for converting source or target objects to and from Spring Messaging {@link Message}s.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface MessagingMessageConverter<S> {

	/**
	 * Convert a source message from a specific messaging system to a {@link Message}.
	 * @param source the source message.
	 * @return the converted message.
	 */
	Message<?> toMessagingMessage(S source);

	/**
	 * Convert a {@link Message} to a message from a specific messaging system.
	 * @param message the message from which to convert.
	 * @return the system specific message.
	 */
	S fromMessagingMessage(Message<?> message);
}
