package io.awspring.cloud.sqs.support.converter;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;

/**
 * {@link SmartMessageConverter} implementation that returns the payload unchanged if the target class
 * for Serialization / Deserialization matches the payload class.
 *
 * @author Tomaz Fernandes
 * @since 3.3
 */
public class SimpleClassMatchingMessageConverter extends AbstractMessageConverter {

	@Override
	protected boolean supports(Class<?> clazz) {
		return true;
	}

	@Nullable
	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		Object payload = message.getPayload();
		return payload.getClass().isAssignableFrom(targetClass) ? payload : null;
	}

	@Nullable
	@Override
	protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {
		return payload.getClass().isAssignableFrom(getSerializedPayloadClass()) ? payload : null;
	}
}
