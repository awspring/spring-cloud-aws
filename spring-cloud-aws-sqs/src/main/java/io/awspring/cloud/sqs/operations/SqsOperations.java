package io.awspring.cloud.sqs.operations;

import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 * @param <T>
 */
public interface SqsOperations<T> extends MessagingOperations<T, SendMessageBatchResponse> {

	UUID send(Consumer<SqsSendOptions.Standard<T>> to);

	UUID sendFifo(Consumer<SqsSendOptions.Fifo<T>> to);

	Optional<Message<T>> receive(Consumer<SqsReceiveOptions.Standard<T>> from);

	Optional<Message<T>> receiveFifo(Consumer<SqsReceiveOptions.Fifo<T>> from);

	Collection<Message<T>> receiveMany(Consumer<SqsReceiveOptions.Standard<T>> from);

	Collection<Message<T>> receiveManyFifo(Consumer<SqsReceiveOptions.Fifo<T>> from);

}
