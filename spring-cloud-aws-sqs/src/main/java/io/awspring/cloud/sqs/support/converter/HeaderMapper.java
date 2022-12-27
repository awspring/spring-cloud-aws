package io.awspring.cloud.sqs.support.converter;

import org.springframework.messaging.MessageHeaders;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 * @param <T>
 */
public interface HeaderMapper<T> {

	T fromHeaders(MessageHeaders headers);

	MessageHeaders toHeaders(T source);

}
