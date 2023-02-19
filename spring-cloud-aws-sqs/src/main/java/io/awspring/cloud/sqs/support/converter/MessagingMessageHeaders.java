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

import java.util.Map;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;

/**
 * {@link MessageHeaders} implementation that allows providing an external {@link UUID}.
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessagingMessageHeaders extends MessageHeaders {

	/**
	 * Create an instance with the provided headers.
	 * @param headers the original headers.
	 */
	public MessagingMessageHeaders(@Nullable Map<String, Object> headers) {
		super(headers);
	}

	/**
	 * Create an instance with the provided headers and id.
	 * @param headers the headers.
	 * @param id the id.
	 */
	public MessagingMessageHeaders(@Nullable Map<String, Object> headers, @Nullable UUID id) {
		super(headers, id, null);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param headers the message headers.
	 * @param id the id.
	 * @param timestamp the timestamp.
	 */
	public MessagingMessageHeaders(@Nullable Map<String, Object> headers, @Nullable UUID id, @Nullable Long timestamp) {
		super(headers, id, timestamp);
	}
}
