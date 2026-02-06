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
package io.awspring.cloud.sns.core.batch.converter;

import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_GROUP_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_ID;

import io.awspring.cloud.sns.core.SnsHeaderConverterUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;

/**
 * Default implementation of {@link SnsMessageConverter} for converting Spring {@link Message} to
 * {@link PublishBatchRequestEntry}.
 * 
 * This converter handles message payload conversion using a {@link CompositeMessageConverter} that includes a
 * {@link StringMessageConverter} by default. Additional converters can be provided during construction.
 * 
 * @author Matej Nedic
 * @since 4.0.1
 */
public class DefaultSnsMessageConverter implements SnsMessageConverter {

	private final MessageConverter messageConverter;

	public DefaultSnsMessageConverter() {
		this(null);
	}

	public DefaultSnsMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = initMessageConverter(messageConverter);
	}

	private static CompositeMessageConverter initMessageConverter(@Nullable MessageConverter messageConverter) {
		List<MessageConverter> converters = new ArrayList<>();

		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);
		converters.add(stringMessageConverter);

		if (messageConverter != null) {
			converters.add(messageConverter);
		}

		return new CompositeMessageConverter(converters);
	}

	/**
	 * Converts a Spring message to an SNS batch request entry.
	 *
	 * @param originalMessage The Spring message to convert
	 * @param <T> The type of the message payload
	 * @return PublishBatchRequestEntry ready to be included in a batch publish request
	 */
	@Override
	public <T> PublishBatchRequestEntry covertMessage(Message<T> originalMessage) {
		PublishBatchRequestEntry.Builder publishBatchRequestEntry = PublishBatchRequestEntry.builder();
		populateHeader(publishBatchRequestEntry, originalMessage);
		Message<?> message = messageConverter.toMessage(originalMessage.getPayload(), originalMessage.getHeaders());
		publishBatchRequestEntry.message(message.getPayload().toString());
		return publishBatchRequestEntry.build();
	}

	private <T> void populateHeader(PublishBatchRequestEntry.Builder publishBatchRequestEntry, Message<T> message) {
		Map<String, MessageAttributeValue> messageAttributes = SnsHeaderConverterUtil.toSnsMessageAttributes(message);
		if (!messageAttributes.isEmpty()) {
			publishBatchRequestEntry.messageAttributes(messageAttributes);
		}

		String id = Optional.ofNullable(message.getHeaders().get(MESSAGE_ID, String.class)).filter(StringUtils::hasText)
				.orElseGet(() -> UUID.randomUUID().toString());
		publishBatchRequestEntry.id(id);

		Optional.ofNullable(message.getHeaders().get(MESSAGE_GROUP_ID_HEADER, String.class))
				.ifPresent(publishBatchRequestEntry::messageGroupId);
		Optional.ofNullable(message.getHeaders().get(MESSAGE_DEDUPLICATION_ID_HEADER, String.class))
				.ifPresent(publishBatchRequestEntry::messageDeduplicationId);
	}

}
