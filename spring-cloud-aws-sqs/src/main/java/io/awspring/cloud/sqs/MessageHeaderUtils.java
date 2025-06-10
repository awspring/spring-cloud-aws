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
package io.awspring.cloud.sqs;

import io.awspring.cloud.sqs.support.converter.MessagingMessageHeaders;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * Utility class for extracting {@link MessageHeaders} from a {@link Message}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageHeaderUtils {

	private MessageHeaderUtils() {
	}

	/**
	 * Return the message's ID as {@link String].
	 * @param message the message.
	 * @return the ID.
	 */
	public static String getId(Message<?> message) {
		return getHeader(message, MessageHeaders.ID, UUID.class).toString();
	}

	/**
	 * Return the messages' ID as a concatenated {@link String].
	 * @param messages the messages.
	 * @return the IDs.
	 */
	public static <T> String getId(Collection<Message<T>> messages) {
		return messages.stream().map(MessageHeaderUtils::getId).collect(Collectors.joining("; "));
	}

	/**
	 * Get the specified header or throw an exception if such header is not present.
	 * @param message the message.
	 * @param headerName the header name.
	 * @param classToCast the class to which the header should be cast to.
	 * @param <T> the class type.
	 * @return the header value.
	 */
	public static <T> T getHeader(Message<?> message, String headerName, Class<T> classToCast) {
		return Objects.requireNonNull(message.getHeaders().get(headerName, classToCast),
				() -> String.format("Header %s not found in message %s", headerName, message));
	}

	/**
	 * Get the specified header or throw an exception if such header is not present.
	 * @param messages the messages.
	 * @param headerName the header name.
	 * @param classToCast the class to which the header should be cast to.
	 * @param <T> the header class type.
	 * @param <U> the messages payload class type.
	 * @return the header value.
	 */
	public static <T, U> Collection<T> getHeader(Collection<Message<U>> messages, String headerName,
			Class<T> classToCast) {
		return messages.stream().map(msg -> getHeader(msg, headerName, classToCast)).collect(Collectors.toList());
	}

	/**
	 * Get the provided header as {@link String} or throw if not present.
	 * @param message the message.
	 * @param headerName the header name.
	 * @return the header value.
	 */
	public static String getHeaderAsString(Message<?> message, String headerName) {
		return getHeader(message, headerName, String.class);
	}

	/**
	 * Add a header to a {@link Message} while preserving the id and timestamp. Note that since messages are immutable,
	 * a new instance will be returned.
	 * @param message the message to add headers to.
	 * @param headerName the header name.
	 * @param headerValue the header value.
	 * @return the new message.
	 * @param <T> the payload type.
	 */
	public static <T> Message<T> addHeaderIfAbsent(Message<T> message, String headerName, Object headerValue) {
		return addHeadersIfAbsent(message, Map.of(headerName, headerValue));
	}

	/**
	 * Add headers to a {@link Message} while preserving the id and timestamp. Note that since messages are immutable, a
	 * new instance will be returned.
	 * @param message the message to add headers to.
	 * @param newHeaders the headers to add.
	 * @return the new message.
	 * @param <T> the payload type.
	 */
	public static <T> Message<T> addHeadersIfAbsent(Message<T> message, Map<String, Object> newHeaders) {
		return new GenericMessage<>(message.getPayload(), addHeadersIfAbsent(message.getHeaders(), newHeaders));
	}

	public static MessageHeaders addHeaderIfAbsent(MessageHeaders headers, String headerName, Object headerValue) {
		return addHeadersIfAbsent(headers, Map.of(headerName, headerValue));
	}

	public static MessageHeaders addHeadersIfAbsent(MessageHeaders headers, Map<String, Object> newHeaders) {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.copyHeaders(headers);
		accessor.copyHeadersIfAbsent(newHeaders);
		return new MessagingMessageHeaders(accessor.toMessageHeaders(), headers.getId(), headers.getTimestamp());
	}

	/**
	 * Remove the provided header key from the {@link Message} if present. Note that a new {@link Message} instance will
	 * be returned.
	 *
	 * @param message the message from which the header should be removed.
	 * @param key the header key to be removed.
	 * @return a new {@link Message} instance with the header removed.
	 * @param <T> the message type.
	 */
	public static <T> Message<T> removeHeaderIfPresent(Message<T> message, String key) {
		if (!message.getHeaders().containsKey(key)) {
			return message;
		}
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		MessageHeaders headers = message.getHeaders();
		accessor.copyHeaders(headers);
		accessor.removeHeader(key);
		MessagingMessageHeaders newHeaders = new MessagingMessageHeaders(accessor.toMessageHeaders(), headers.getId(),
				headers.getTimestamp());
		return new GenericMessage<>(message.getPayload(), newHeaders);
	}

}
