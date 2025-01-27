package io.awspring.cloud.sqs.support.converter;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.support.GenericMessage;

public class EventBridgeMessageConverter extends WrappedMessageConverter {
	private static final String WRITING_CONVERSION_ERROR = "This converter only supports reading an EventBridge message and not writing them";

	public EventBridgeMessageConverter(MessageConverter payloadConverter, ObjectMapper jsonMapper) {
		super(payloadConverter, jsonMapper);
	}

	@Override
	protected Object fromGenericMessage(GenericMessage<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		JsonNode jsonNode;
		try {
			jsonNode = this.jsonMapper.readTree(message.getPayload().toString());
		}
		catch (Exception e) {
			throw new MessageConversionException("Could not read JSON", e);
		}

		if (!jsonNode.has("detail")) {
			throw new MessageConversionException(
				"Payload: '" + message.getPayload() + "' does not contain a detail attribute", null);
		}

		// Unlike SNS, where the message is a nested JSON string, the message payload in EventBridge is a JSON object
		JsonNode messagePayload = jsonNode.get("detail");
		GenericMessage<JsonNode> genericMessage = new GenericMessage<>(messagePayload);
		if (payloadConverter instanceof SmartMessageConverter payloadConverter) {
			return payloadConverter.fromMessage(genericMessage, targetClass, conversionHint);
		} else {
			return payloadConverter.fromMessage(genericMessage, targetClass);
		}
	}

	@Override
	protected String getWritingConversionErrorMessage() {
		return WRITING_CONVERSION_ERROR;
	}
}
