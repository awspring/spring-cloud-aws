package io.awspring.cloud.sqs.support.converter.context;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.HeaderMapper;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ContextAwareHeaderMapper<S> extends HeaderMapper<S> {

	MessageHeaders getContextHeaders(S source, MessageConversionContext context);

}
