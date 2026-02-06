package io.awspring.cloud.sns.core.async;

import org.springframework.messaging.Message;

import java.util.Map;

/**
 * Converter for transforming Spring messages to SNS PublishRequest.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
public interface SnsPublishMessageConverter {

	/**
	 * Converts a Spring Message to a PublishRequest.
	 *
	 * @param message the message to convert
	 * @param <T> the message payload type
	 * @return a pair containing the PublishRequest and the original message
	 */
	<T> PublishRequestMessagePair<T> convert(Message<T> message);

	/**
	 * Converts a payload and headers to a PublishRequest.
	 *
	 * @param payload the payload to convert
	 * @param headers the headers to include
	 * @param <T> the payload type
	 * @return a pair containing the PublishRequest and the constructed message
	 */
	<T> PublishRequestMessagePair<T> convert(T payload, Map<String, Object> headers);
}
