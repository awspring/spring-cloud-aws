package io.awspring.cloud.sqs.support.converter;


import org.springframework.messaging.Message;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface MessagingMessageConverter<S> {

	Message<?> toMessagingMessage(S source);

}
