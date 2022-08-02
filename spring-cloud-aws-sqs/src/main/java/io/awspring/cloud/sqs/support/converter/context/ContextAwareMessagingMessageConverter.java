package io.awspring.cloud.sqs.support.converter.context;

import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ContextAwareMessagingMessageConverter<S> extends MessagingMessageConverter<S> {

	@Override
	default Message<?> toMessagingMessage(S source) {
		return toMessagingMessage(source, null);
	}

	Message<?> toMessagingMessage(S source, @Nullable MessageConversionContext context);

	MessageConversionContext createMessageConversionContext();

}
