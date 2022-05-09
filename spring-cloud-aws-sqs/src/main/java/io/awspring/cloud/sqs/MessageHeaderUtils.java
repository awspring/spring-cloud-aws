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

import io.awspring.cloud.sqs.listener.SqsMessageHeaders;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

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
		return Objects.requireNonNull(message.getHeaders().get(MessageHeaders.ID, UUID.class),
				() -> "No ID found for message " + message).toString();
	}

	/**
	 * Return the message's {@link Acknowledgement}
	 * @param message the message.
	 * @return the acknowledgement.
	 */
	public static Acknowledgement getAcknowledgement(Message<?> message) {
		return Objects.requireNonNull(
				message.getHeaders().get(SqsMessageHeaders.ACKNOWLEDGMENT_HEADER, Acknowledgement.class),
				() -> "No Acknowledgment found for message " + message);
	}

	public static Object getHeader(Message<?> message, String headerName) {
		return Objects.requireNonNull(message.getHeaders().get(headerName),
			() -> String.format("Header %s not found in message %s", headerName, getId(message)));
	}

	public static <T> T getHeader(Message<?> message, String headerName, Class<T> classToCast) {
		Object header = getHeader(message, headerName);
		Assert.isInstanceOf(classToCast, header,
			() -> String.format("Header %s from message %s not instance of class %s", header, getId(message), classToCast));
		return classToCast.cast(header);
	}

	public static <T> String getId(Collection<Message<T>> messages) {
		return messages.stream().map(MessageHeaderUtils::getId).collect(Collectors.joining("; "));
	}
}
