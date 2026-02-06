package io.awspring.cloud.sns.core.async;

import io.awspring.cloud.sns.core.SnsNotification;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.MessagePostProcessor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * High level asynchronous SNS operations.
 *
 * @author Matej Nedic
 * @since 4.0.1
 */
public interface SnsAsyncOperations {

	/**
	 * Sends a message to a destination.
	 *
	 * @param destination the destination name
	 * @param message     the message to send
	 * @param <T>         the message payload type
	 * @return a CompletableFuture with the result
	 */
	<T> CompletableFuture<SnsResult<T>> send(String destination, Message<T> message) ;

	/**
	 * Converts and sends a payload to a destination.
	 *
	 * @param destination the destination name
	 * @param payload     the payload to send
	 * @param <T>         the payload type
	 * @return a CompletableFuture with the result
	 */
	<T> CompletableFuture<SnsResult<T>> convertAndSend(String destination, T payload) ;

	/**
	 * Converts and sends a payload with headers to a destination.
	 *
	 * @param destination the destination name
	 * @param payload     the payload to send
	 * @param headers     the headers to include
	 * @param <T>         the payload type
	 * @return a CompletableFuture with the result
	 */
	<T> CompletableFuture<SnsResult<T>> convertAndSend(String destination, T payload, @Nullable Map<String, Object> headers)
		;

	/**
	 * Converts and sends a payload with a post processor to a destination.
	 *
	 * @param destination   the destination name
	 * @param payload       the payload to send
	 * @param postProcessor the post processor to apply
	 * @param <T>           the payload type
	 * @return a CompletableFuture with the result
	 */
	<T> CompletableFuture<SnsResult<T>> convertAndSend(String destination, T payload, @Nullable MessagePostProcessor postProcessor)
		;

	/**
	 * Converts and sends a payload with headers and a post processor to a destination.
	 *
	 * @param destination   the destination name
	 * @param payload       the payload to send
	 * @param headers       the headers to include
	 * @param postProcessor the post processor to apply
	 * @param <T>           the payload type
	 * @return a CompletableFuture with the result
	 */
	<T> CompletableFuture<SnsResult<T>> convertAndSend(String destination, T payload, @Nullable Map<String, Object> headers,
													   @Nullable MessagePostProcessor postProcessor);

	/**
	 * Sends a notification with a message and subject to a destination.
	 *
	 * @param destinationName the destination name
	 * @param message         the message to send
	 * @param subject         the subject (can be null)
	 * @return a CompletableFuture with the result
	 */
	CompletableFuture<SnsResult<Object>> sendNotification(String destinationName, Object message, @Nullable String subject);

	/**
	 * Sends a notification to a topic.
	 *
	 * @param topic        the topic name
	 * @param notification the notification to send
	 * @param <T>          the notification payload type
	 * @return a CompletableFuture with the result
	 */
	<T> CompletableFuture<SnsResult<T>> sendNotification(String topic, SnsNotification<T> notification);

	/**
	 * Checks if a topic with the given ARN exists.
	 *
	 * @param topicArn the ARN of the topic
	 * @return a CompletableFuture with true if the topic exists, false otherwise
	 */
	CompletableFuture<Boolean> topicExists(String topicArn);
}
