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
 * Synchronous messaging operations.
 * Implementations should have defaults for {@link Nullable} fields,
 * and can provide chained methods interfaces to provide a more fluent API.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface MessagingOperations<T, R> {

	/**
	 * Send a {@link Message} to the default endpoint with the provided payload.
	 * The payload will be serialized if necessary.
	 *
	 * @param payload the payload to send.
	 * @return The message's {@link UUID}.
	 */
	UUID send(T payload);

	/**
	 * Send a message to the provided endpoint with the provided payload.
	 * The payload will be serialized if necessary.
	 *
	 * @param endpointName the endpoint to send the message to.
	 * @param payload the payload to send.
	 * @return The message's {@link UUID}.
	 */
	UUID send(@Nullable String endpointName, T payload);

	/**
	 * Send the provided message along with its headers to the provided endpoint.
	 * The payload will be serialized if necessary, and headers will be converted
	 * to the specific messaging system metadata types.
	 *
	 * @param endpointName the endpoint to send the message to.
	 * @param message the message to be sent.
	 * @return The message's {@link UUID}.
	 */
	UUID send(@Nullable String endpointName, Message<T> message);

	/**
	 * Send the provided messages along with their headers to the provided endpoint.
	 * The payloads will be serialized if necessary, and headers will be converted
	 * to the specific messaging system metadata types.
	 *
	 * @param endpointName the endpoint to send the messages to.
	 * @param messages the messages to be sent.
	 * @return The message's {@link UUID}.
	 */
	R send(@Nullable String endpointName, Collection<Message<T>> messages);

	/**
	 * Receive a message from the default endpoint with default settings.
	 * @return the message or {@link Optional#empty()} if none is returned.
	 */
	Optional<Message<T>> receive();

	/**
	 * Receive a message from the provided endpoint and convert the payload to the
	 * provided class. If no message is returned after the specified {@link Duration},
	 * an {@link Optional#empty()} is returned.
	 * <p>
	 * Any headers provided in the additional headers parameter will be added to the
	 * {@link Message} instances returned by this method.
	 * The implementation can also allow some specific headers to change particular settings,
	 * in which case the headers are removed before sending.
	 * See the implementation javadocs for more information.
	 *
	 * @param endpointName the endpoint from which to receive the messages.
	 * @param payloadClass the class to which the payload should be converted to.
	 * @param pollTimeout the maximum amount of time to wait for messages.
	 * @param additionalHeaders headers to be added to the received messages.
	 * @return the message, or {@link Optional#empty()} if none is returned.
	 */
	Optional<Message<T>> receive(@Nullable String endpointName,
								 @Nullable Class<T> payloadClass,
								 @Nullable Duration pollTimeout,
								 @Nullable Map<String, Object> additionalHeaders);

	/**
	 * Receive a batch of messages from the default endpoint with default settings.
	 * @return The messages, or an empty collection if none is returned.
	 */
	Collection<Message<T>> receiveMany();

	/**
	 * Receive a message from the provided endpoint and convert the payload to the
	 * provided class. If no message is returned after the specified {@link Duration},
	 * an {@link Optional#empty()} is returned.
	 * <p>
	 * Any headers provided in the additional headers parameter will be added to the
	 * {@link Message} instances returned by this method.
	 * The implementation can also allow some specific headers to change particular settings,
	 * in which case the headers are removed before sending.
	 * See the implementation javadocs for more information.
	 *
	 * @param endpointName the endpoint from which to receive the messages.
	 * @param payloadClass the class to which the payloads should be converted to.
	 * @param pollTimeout the maximum amount of time to wait for messages.
	 * @param additionalHeaders headers to be added to the received messages.
	 * @return the messages, or {@link Optional#empty()} if none is returned.
	 */
	Collection<Message<T>> receiveMany(@Nullable String endpointName,
									   @Nullable Class<T> payloadClass,
									   @Nullable Duration pollTimeout,
									   @Nullable Integer maxNumberOfMessages,
									   @Nullable Map<String, Object> additionalHeaders);

}
