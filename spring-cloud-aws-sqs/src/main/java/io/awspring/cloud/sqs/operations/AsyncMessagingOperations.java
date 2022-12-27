package io.awspring.cloud.sqs.operations;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface AsyncMessagingOperations<T, R> {

	CompletableFuture<UUID> sendAsync(T payload);

	CompletableFuture<UUID> sendAsync(@Nullable String endpointName, T payload);

	CompletableFuture<UUID> sendAsync(@Nullable String endpointName, Message<T> message);

	CompletableFuture<R> sendAsync(@Nullable String endpointName, Collection<Message<T>> message);

	CompletableFuture<Optional<Message<T>>> receiveAsync();

	CompletableFuture<Optional<Message<T>>> receiveAsync(@Nullable String endpoint,
														 @Nullable Class<T> payloadClass,
														 @Nullable Duration pollTimeout,
														 @Nullable Map<String, Object> additionalHeaders);

	CompletableFuture<Collection<Message<T>>> receiveManyAsync();

	CompletableFuture<Collection<Message<T>>> receiveManyAsync(@Nullable String endpoint,
															   @Nullable Class<T> payloadClass,
															   @Nullable Duration pollTimeout,
															   @Nullable Integer maxNumberOfMessages,
															   @Nullable Map<String, Object> additionalHeaders);

}
