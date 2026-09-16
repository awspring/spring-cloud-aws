/*
 * Copyright 2013-2025 the original author or authors.
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

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * Utility class for resolving SQS message IDs. Consolidates UUID validation, conversion, and fail-fast logic used by
 * both {@link SqsHeaderMapper} and {@link io.awspring.cloud.sqs.operations.SqsTemplate}.
 *
 * @author Jeongmin Kim
 * @since 4.1.0
 */
public final class SqsMessageIdResolver {

	private SqsMessageIdResolver() {
	}

	/**
	 * Resolve the message ID and add it to the provided headers. The raw message ID is always stored in the
	 * {@link SqsHeaders#SQS_RAW_MESSAGE_ID_HEADER} header.
	 *
	 * <p>
	 * If the message ID is a valid UUID, it is used directly. If not, and {@code convertMessageIdToUuid} is
	 * {@code true}, a {@link MessagingException} is thrown with instructions to disable UUID conversion. If
	 * {@code convertMessageIdToUuid} is {@code false}, a deterministic UUID is generated from the raw message ID.
	 * @param messageId the raw message ID from SQS.
	 * @param headers the existing message headers.
	 * @param convertMessageIdToUuid whether to enforce UUID message IDs.
	 * @return the resolved {@link MessageHeaders} with the message ID set.
	 * @throws MessagingException if the message ID is not a valid UUID and conversion is enabled.
	 */
	public static MessageHeaders resolveAndAddMessageId(String messageId, MessageHeaders headers,
			boolean convertMessageIdToUuid) {
		MessageHeaders withRawId = MessageHeaderUtils.addHeaderIfAbsent(headers, SqsHeaders.SQS_RAW_MESSAGE_ID_HEADER,
				messageId);
		Optional<UUID> uuid = tryParseUuid(messageId);
		if (uuid.isPresent()) {
			return new MessagingMessageHeaders(withRawId, uuid.get());
		}
		if (convertMessageIdToUuid) {
			throw new MessagingException(String.format(
					"Message ID '%s' is not a valid UUID. To support non-UUID message IDs, "
							+ "set 'spring.cloud.aws.sqs.convert-message-id-to-uuid=false'. "
							+ "The raw message ID will be available via the '%s' header.",
					messageId, SqsHeaders.SQS_RAW_MESSAGE_ID_HEADER));
		}
		return new MessagingMessageHeaders(withRawId,
				UUID.nameUUIDFromBytes(messageId.getBytes(StandardCharsets.UTF_8)));
	}

	/**
	 * Configure message ID resolution on the given converter. If the converter is an
	 * {@link AbstractMessagingMessageConverter} with a {@link SqsHeaderMapper}, sets the {@code convertMessageIdToUuid}
	 * flag on it.
	 * @param converter the messaging message converter.
	 * @param convertMessageIdToUuid whether to enforce UUID message IDs.
	 */
	public static void configureMessageIdResolution(MessagingMessageConverter<Message> converter,
			boolean convertMessageIdToUuid) {
		if (converter instanceof AbstractMessagingMessageConverter<Message> abstractConverter) {
			abstractConverter.configureHeaderMapper(headerMapper -> {
				if (headerMapper instanceof SqsHeaderMapper sqsHeaderMapper) {
					sqsHeaderMapper.setConvertMessageIdToUuid(convertMessageIdToUuid);
				}
			});
		}
	}

	private static Optional<UUID> tryParseUuid(String value) {
		try {
			return Optional.of(UUID.fromString(value));
		}
		catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

}