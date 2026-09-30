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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Opt-in {@link SqsHeaderMapper} that maps attributes from SNS notification envelopes. Existing SQS and additional
 * headers take precedence over SNS attributes. Configure this mapper through
 * {@link AbstractMessagingMessageConverter#setHeaderMapper(HeaderMapper)} for queues receiving SNS notifications
 * without raw message delivery.
 *
 * @author WooSeong Kim
 * @since 4.1.0
 */
public class SnsAwareSqsHeaderMapper extends SqsHeaderMapper {

	private static final Logger logger = LoggerFactory.getLogger(SnsAwareSqsHeaderMapper.class);

	private static final TypeReference<Map<String, SnsNotification.MessageAttribute>> SNS_MESSAGE_ATTRIBUTES_TYPE = new TypeReference<>() {
	};

	private final JsonMapper jsonMapper;

	/**
	 * Create a mapper using the application's configured JSON mapper.
	 * @param jsonMapper the mapper used for message conversion; must not be null.
	 */
	public SnsAwareSqsHeaderMapper(JsonMapper jsonMapper) {
		Assert.notNull(jsonMapper, "jsonMapper cannot be null");
		this.jsonMapper = jsonMapper;
	}

	@Override
	public MessageHeaders toHeaders(Message source) {
		MessageHeaders headers = super.toHeaders(source);
		Map<String, Object> attributes = getSnsMessageAttributesAsHeaders(source);
		if (attributes.isEmpty()) {
			return headers;
		}
		Map<String, Object> result = new HashMap<>(headers);
		attributes.forEach(result::putIfAbsent);
		return new MessagingMessageHeaders(result, headers.getId(), headers.getTimestamp());
	}

	private Map<String, Object> getSnsMessageAttributesAsHeaders(Message source) {
		String body = source.body();
		if (body == null || !body.contains("\"MessageAttributes\"")) {
			return Map.of();
		}
		try {
			JsonNode jsonNode = jsonMapper.readTree(body);
			if (!isSnsNotification(jsonNode)) {
				return Map.of();
			}
			JsonNode messageAttributes = jsonNode.get("MessageAttributes");
			if (messageAttributes == null || !messageAttributes.isObject()) {
				return Map.of();
			}
			Map<String, SnsNotification.MessageAttribute> attributes = jsonMapper.convertValue(messageAttributes,
					SNS_MESSAGE_ATTRIBUTES_TYPE);
			return attributes.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, entry -> getValue(entry.getValue())));
		}
		catch (JacksonException | IllegalArgumentException e) {
			logger.trace("Could not map SNS message attributes for message " + source.messageId(), e);
			return Map.of();
		}
	}

	private boolean isSnsNotification(JsonNode jsonNode) {
		JsonNode type = jsonNode.get("Type");
		return type != null && "Notification".equals(type.asString()) && jsonNode.has("Message");
	}

	private Object getValue(SnsNotification.MessageAttribute value) {
		String dataType = value.getType();
		Assert.notNull(dataType, "dataType must not be null");
		String baseDataType = dataType.contains(".") ? dataType.substring(0, dataType.indexOf('.')) : dataType;

		return switch (baseDataType) {
		case MessageAttributeDataTypes.NUMBER -> getNumberValue(value.getValue(), dataType);
		case MessageAttributeDataTypes.BINARY -> SdkBytes.fromByteArray(Base64.getDecoder().decode(value.getValue()));
		default -> value.getValue();
		};
	}

}
