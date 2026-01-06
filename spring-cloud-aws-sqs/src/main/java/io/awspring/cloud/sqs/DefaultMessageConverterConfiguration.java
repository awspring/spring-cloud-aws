package io.awspring.cloud.sqs;

import io.awspring.cloud.core.support.JacksonPresent;
import io.awspring.cloud.sqs.config.MessageConverterFactory;
import io.awspring.cloud.sqs.config.legacy.LegacyJacskon2MessageConverterFactory;
import org.springframework.messaging.converter.MessageConverter;

public class DefaultMessageConverterConfiguration {
	public static MessageConverter createDefaultMessageConverter() {
		if (JacksonPresent.isJackson3Present()) {
			return  MessageConverterFactory.createDefaultMappingJacksonMessageConverter();
		} else if (JacksonPresent.isJackson2Present()) {
			return LegacyJacskon2MessageConverterFactory.createDefaultMappingLegacyJackson2MessageConverter();
		}
		throw new IllegalStateException(
			"Sqs integration requires a Jackson 2 or Jackson 3 library on the classpath");
	}
}
