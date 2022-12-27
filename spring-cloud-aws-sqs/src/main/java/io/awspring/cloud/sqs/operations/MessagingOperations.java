package io.awspring.cloud.sqs.operations;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface MessagingOperations<T, R> {

	UUID send(T payload);

	UUID send(@Nullable String endpointName, T payload);

	UUID send(@Nullable String endpointName, Message<T> message);

	R send(@Nullable String endpointName, Collection<Message<T>> messages);

	Optional<Message<T>> receive();

	Optional<Message<T>> receive(@Nullable String endpointName,
								 @Nullable Class<T> payloadClass,
								 @Nullable Duration pollTimeout,
								 @Nullable Map<String, Object> additionalHeaders);

	Collection<Message<T>> receiveMany();

	Collection<Message<T>> receiveMany(@Nullable String endpointName,
									   @Nullable Class<T> payloadClass,
									   @Nullable Duration pollTimeout,
									   @Nullable Integer maxNumberOfMessages,
									   @Nullable Map<String, Object> additionalHeaders);

}
