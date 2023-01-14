package io.awspring.cloud.sqs.operations;

import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Sqs-specific synchronous messaging operations for Standard and Fifo queues.
 * <p>
 * Note that the Standard queue methods can be used for Fifo queues
 * as long as necessary headers are added for required attributes such
 * as message deduplication id.
 * See {@link io.awspring.cloud.sqs.listener.SqsHeaders} for reference
 * of available headers.
 * <p>
 * Fifo queue methods accept the required attributes and add a random value
 * if none is specified.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @param <T>
 */
public interface SqsOperations<T> extends MessagingOperations<T, SendMessageBatchResponse> {

	/**
	 * Send a message to a Standard SQS queue using the {@link SqsSendOptions.Standard} options.
	 * @param to a {@link SqsSendOptions.Standard} consumer.
	 * @return The {@link UUID} of the message.
	 */
	UUID send(Consumer<SqsSendOptions.Standard<T>> to);

	/**
	 * Send a message to a Fifo SQS queue using the {@link SqsSendOptions.Fifo} options.
	 * @param to a {@link SqsSendOptions.Fifo} consumer.
	 * @return The {@link UUID} of the message.
	 */
	UUID sendFifo(Consumer<SqsSendOptions.Fifo<T>> to);

	/**
	 * Send a batch of messages to a Fifo SQS queue.
	 * @param endpoint the endpoint to which to send the messages.
	 * @param messages the messages.
	 * @return the {@link SendMessageBatchResponse}
	 */
	SendMessageBatchResponse sendFifo(String endpoint, Collection<Message<T>> messages);

	/**
	 * Receive a message from a Standard SQS queue using the {@link SqsReceiveOptions.Standard} options.
	 * @param from a {@link SqsReceiveOptions.Standard} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	Optional<Message<T>> receive(Consumer<SqsReceiveOptions.Standard<T>> from);

	/**
	 * Receive a message from a Fifo SQS queue using the {@link SqsReceiveOptions.Fifo} options.
	 * @param from a {@link SqsReceiveOptions.Fifo} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	Optional<Message<T>> receiveFifo(Consumer<SqsReceiveOptions.Fifo<T>> from);

	/**
	 * Receive a batch of messages from a Standard SQS queue using the {@link SqsReceiveOptions.Standard}
	 * options.
	 * @param from a {@link SqsReceiveOptions.Standard} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	Collection<Message<T>> receiveMany(Consumer<SqsReceiveOptions.Standard<T>> from);

	/**
	 * Receive a batch of messages from a Fifo SQS queue using the {@link SqsReceiveOptions.Fifo}
	 * options.
	 * @param from a {@link SqsReceiveOptions.Fifo} consumer.
	 * @return The message, or an empty collection if none is returned.
	 */
	Collection<Message<T>> receiveManyFifo(Consumer<SqsReceiveOptions.Fifo<T>> from);

}
