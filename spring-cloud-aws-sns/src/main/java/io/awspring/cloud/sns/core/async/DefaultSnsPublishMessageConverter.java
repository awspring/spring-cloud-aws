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

import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_GROUP_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.NOTIFICATION_SUBJECT_HEADER;

import io.awspring.cloud.sns.core.SnsHeaderConverterUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Default implementation of {@link SnsPublishMessageConverter}.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
public class DefaultSnsPublishMessageConverter implements SnsPublishMessageConverter {

	private final MessageConverter messageConverter;

	public DefaultSnsPublishMessageConverter() {
		this(null);
	}

	public DefaultSnsPublishMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = initMessageConverter(messageConverter);
	}

	@Override
	public <T> PublishRequestMessagePair<T> convert(Message<T> originalMessage) {
		Assert.notNull(originalMessage, "message cannot be null");

		PublishRequest.Builder publishRequest = PublishRequest.builder();
		populateHeaders(publishRequest, originalMessage);

		Message<?> convertedMessage = messageConverter.toMessage(originalMessage.getPayload(),
				originalMessage.getHeaders());
		publishRequest.message(convertedMessage.getPayload().toString());

		return new PublishRequestMessagePair<>(publishRequest.build(), originalMessage);
	}

	@Override
	public <T> PublishRequestMessagePair<T> convert(T payload, Map<String, Object> headers) {
		Assert.notNull(payload, "payload cannot be null");
		Assert.notNull(headers, "headers cannot be null");

		Message<T> originalMessage = MessageBuilder.withPayload(payload).copyHeaders(headers).build();

		return convert(originalMessage);
	}

	private <T> void populateHeaders(PublishRequest.Builder publishRequest, Message<T> message) {
		Map<String, MessageAttributeValue> messageAttributes = SnsHeaderConverterUtil.toSnsMessageAttributes(message);
		if (!messageAttributes.isEmpty()) {
			publishRequest.messageAttributes(messageAttributes);
		}

		Optional.ofNullable(message.getHeaders().get(NOTIFICATION_SUBJECT_HEADER, String.class))
				.ifPresent(publishRequest::subject);

		Optional.ofNullable(message.getHeaders().get(MESSAGE_GROUP_ID_HEADER, String.class))
				.ifPresent(publishRequest::messageGroupId);

		Optional.ofNullable(message.getHeaders().get(MESSAGE_DEDUPLICATION_ID_HEADER, String.class))
				.ifPresent(publishRequest::messageDeduplicationId);
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
}
