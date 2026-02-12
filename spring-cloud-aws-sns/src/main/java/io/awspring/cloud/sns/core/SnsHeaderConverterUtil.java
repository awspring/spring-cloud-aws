package io.awspring.cloud.sns.core;

import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_DEDUPLICATION_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.MESSAGE_GROUP_ID_HEADER;
import static io.awspring.cloud.sns.core.SnsHeaders.NOTIFICATION_SUBJECT_HEADER;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.NumberUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

/**
 *
 * Util class to convert header map to SNS request.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Gyozo Papp
 * @author Matej Nedic
 */
class SnsHeaderConverterUtil {
	private static final Log logger = LogFactory.getLog(SnsHeaderConverterUtil.class);
	private static final JsonStringEncoder jsonStringEncoder = JsonStringEncoder.create();

	public static Map<String, MessageAttributeValue> toSnsMessageAttributes(Message<?> message) {
		HashMap<String, MessageAttributeValue> messageAttributes = new HashMap<>();
		for (Map.Entry<String, Object> messageHeader : message.getHeaders().entrySet()) {
			String messageHeaderName = messageHeader.getKey();
			Object messageHeaderValue = messageHeader.getValue();

			if (isSkipHeader(messageHeaderName)) {
				continue;
			}

			if (MessageHeaders.CONTENT_TYPE.equals(messageHeaderName) && messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName, getContentTypeMessageAttribute(messageHeaderValue));
			} else if (MessageHeaders.ID.equals(messageHeaderName) && messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName, getStringMessageAttribute(messageHeaderValue.toString()));
			} else if (MessageHeaders.TIMESTAMP.equals(messageHeaderName) && messageHeaderValue != null) {
				messageAttributes.put(messageHeaderName, getDetailedNumberMessageAttribute(messageHeaderValue));
			} else if (messageHeaderValue instanceof String) {
				messageAttributes.put(messageHeaderName, getStringMessageAttribute((String) messageHeaderValue));
			} else if (messageHeaderValue instanceof Number) {
				messageAttributes.put(messageHeaderName, getDetailedNumberMessageAttribute(messageHeaderValue));
			} else if (messageHeaderValue instanceof ByteBuffer) {
				messageAttributes.put(messageHeaderName, getBinaryMessageAttribute((ByteBuffer) messageHeaderValue));
			} else if (messageHeaderValue instanceof List) {
				messageAttributes.put(messageHeaderName, getStringArrayMessageAttribute((List<?>) messageHeaderValue));
			} else {
				logger.warn(String.format(
					"Message header with name '%s' and type '%s' cannot be sent as"
						+ " message attribute because it is not supported by SNS.",
					messageHeaderName, messageHeaderValue != null ? messageHeaderValue.getClass().getName() : ""));
			}
		}

		return messageAttributes;
	}

	private static boolean isSkipHeader(String headerName) {
		return NOTIFICATION_SUBJECT_HEADER.equals(headerName) || MESSAGE_GROUP_ID_HEADER.equals(headerName)
			|| MESSAGE_DEDUPLICATION_ID_HEADER.equals(headerName);
	}

	private static <T> MessageAttributeValue getStringArrayMessageAttribute(List<T> messageHeaderValue) {
		String stringValue = messageHeaderValue.stream().map(item -> {
			StringBuilder sb = new StringBuilder();
			jsonStringEncoder.quoteAsString(item.toString(), sb);
			return "\"" + sb + "\"";
		}).collect(Collectors.joining(", ", "[", "]"));

		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING_ARRAY).stringValue(stringValue)
			.build();
	}

	private static MessageAttributeValue getBinaryMessageAttribute(ByteBuffer messageHeaderValue) {
		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.BINARY)
			.binaryValue(SdkBytes.fromByteBuffer(messageHeaderValue)).build();
	}

	@Nullable
	private static MessageAttributeValue getContentTypeMessageAttribute(Object messageHeaderValue) {
		if (messageHeaderValue instanceof MimeType) {
			return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
				.stringValue(messageHeaderValue.toString()).build();
		} else if (messageHeaderValue instanceof String) {
			return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
				.stringValue((String) messageHeaderValue).build();
		}
		return null;
	}

	private static MessageAttributeValue getStringMessageAttribute(String messageHeaderValue) {
		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.STRING)
			.stringValue(messageHeaderValue).build();
	}

	private static MessageAttributeValue getDetailedNumberMessageAttribute(Object messageHeaderValue) {
		Assert.isTrue(NumberUtils.STANDARD_NUMBER_TYPES.contains(messageHeaderValue.getClass()),
			"Only standard number types are accepted as message header.");

		return MessageAttributeValue.builder()
			.dataType(MessageAttributeDataTypes.NUMBER + "." + messageHeaderValue.getClass().getName())
			.stringValue(messageHeaderValue.toString()).build();
	}

	private static MessageAttributeValue getNumberMessageAttribute(Object messageHeaderValue) {
		return MessageAttributeValue.builder().dataType(MessageAttributeDataTypes.NUMBER)
			.stringValue(messageHeaderValue.toString()).build();
	}
}
