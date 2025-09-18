/*
 * Copyright 2013-2018 the original author or authors.
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
package io.awspring.cloud.sns.integration;

import io.awspring.cloud.sns.core.SnsHeaders;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 * The {@link HeaderMapper} implementation for the mapping from headers to SNS message attributes.
 * <p>
 * On the Inbound side, the SNS message is fully mapped from the JSON to the message payload. Only important HTTP
 * headers are mapped to the message headers.
 *
 * @author Artem Bilan
 *
 * @since 4.0
 */
public class SnsHeaderMapper implements HeaderMapper<Map<String, MessageAttributeValue>> {

	private final Log logger = LogFactory.getLog(SnsHeaderMapper.class);

	private volatile String[] outboundHeaderNames = { "!" + MessageHeaders.ID, "!" + MessageHeaders.TIMESTAMP,
			"!" + NativeMessageHeaderAccessor.NATIVE_HEADERS, "!" + SnsHeaders.MESSAGE_ID_HEADER,
			"!" + SnsHeaders.TOPIC_HEADER, "*", };

	/**
	 * Provide the header names that should be mapped to an AWS request object attributes (for outbound adapters) from a
	 * Spring Integration Message's headers. The values can also contain simple wildcard patterns (e.g. "foo*" or
	 * "*foo") to be matched. Also supports negated ('!') patterns. First match wins (positive or negative). To match
	 * the names starting with {@code !} symbol, you have to escape it prepending with the {@code \} symbol in the
	 * pattern definition. Defaults to map all ({@code *}) if the type is supported by SNS. The
	 * {@link MessageHeaders#ID}, {@link MessageHeaders#TIMESTAMP}, {@link NativeMessageHeaderAccessor#NATIVE_HEADERS},
	 * {@link SnsHeaders#MESSAGE_ID_HEADER and {@link SnsHeaders#TOPIC_HEADER} are ignored by default.
	 * @param outboundHeaderNames The inbound header names.
	 */
	public void setOutboundHeaderNames(String... outboundHeaderNames) {
		Assert.notNull(outboundHeaderNames, "'outboundHeaderNames' must not be null.");
		Assert.noNullElements(outboundHeaderNames, "'outboundHeaderNames' must not contains null elements.");
		Arrays.sort(outboundHeaderNames);
		this.outboundHeaderNames = outboundHeaderNames;
	}

	@Override
	public void fromHeaders(MessageHeaders headers, Map<String, MessageAttributeValue> target) {
		for (Map.Entry<String, Object> messageHeader : headers.entrySet()) {
			String messageHeaderName = messageHeader.getKey();
			Object messageHeaderValue = messageHeader.getValue();

			if (Boolean.TRUE.equals(PatternMatchUtils.smartMatch(messageHeaderName, this.outboundHeaderNames))) {
				if (messageHeaderValue instanceof UUID || messageHeaderValue instanceof MimeType
						|| messageHeaderValue instanceof Boolean || messageHeaderValue instanceof String) {

					target.put(messageHeaderName, getStringMessageAttribute(messageHeaderValue.toString()));
				}
				else if (messageHeaderValue instanceof Number) {
					target.put(messageHeaderName, getNumberMessageAttribute(messageHeaderValue));
				}
				else if (messageHeaderValue instanceof ByteBuffer byteBuffer) {
					target.put(messageHeaderName, getBinaryMessageAttribute(byteBuffer));
				}
				else if (messageHeaderValue instanceof byte[] bytes) {
					target.put(messageHeaderName, getBinaryMessageAttribute(ByteBuffer.wrap(bytes)));
				}
				else {
					if (this.logger.isWarnEnabled()) {
						this.logger.warn(String.format(
								"Message header with name '%s' and type '%s' cannot be sent as"
										+ " message attribute because it is not supported by the current AWS service.",
								messageHeaderName, messageHeaderValue.getClass().getName()));
					}
				}

			}
		}
	}

	private static MessageAttributeValue getBinaryMessageAttribute(ByteBuffer messageHeaderValue) {
		return buildMessageAttribute("Binary", messageHeaderValue);
	}

	private static MessageAttributeValue getStringMessageAttribute(String messageHeaderValue) {
		return buildMessageAttribute("String", messageHeaderValue);
	}

	private static MessageAttributeValue getNumberMessageAttribute(Object messageHeaderValue) {
		Class<?> messageHeaderValueClass = messageHeaderValue.getClass();
		Assert.isTrue(NumberUtils.STANDARD_NUMBER_TYPES.contains(messageHeaderValueClass),
				"Only standard number types are accepted as message header.");

		return buildMessageAttribute("Number." + messageHeaderValueClass.getName(), messageHeaderValue);
	}

	private static MessageAttributeValue buildMessageAttribute(String dataType, Object value) {
		MessageAttributeValue.Builder messageAttributeValue = MessageAttributeValue.builder().dataType(dataType);
		if (value instanceof ByteBuffer byteBuffer) {
			messageAttributeValue.binaryValue(SdkBytes.fromByteBuffer(byteBuffer));
		}
		else {
			messageAttributeValue.stringValue(value.toString());
		}

		return messageAttributeValue.build();
	}

	@Override
	public Map<String, Object> toHeaders(Map<String, MessageAttributeValue> source) {
		throw new UnsupportedOperationException("The mapping from AWS Response Message is not supported");
	}

}
