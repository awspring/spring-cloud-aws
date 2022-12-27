package io.awspring.cloud.sqs.operations;

import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 * @param <T>
 */
public interface SqsAsyncOperations<T> extends AsyncMessagingOperations<T, SendMessageBatchResponse> {

	CompletableFuture<UUID> sendAsync(Consumer<SqsSendOptions.Standard<T>> to);

	CompletableFuture<UUID> sendFifoAsync(Consumer<SqsSendOptions.Fifo<T>> to);

	CompletableFuture<Optional<Message<T>>> receiveAsync(Consumer<SqsReceiveOptions.Standard<T>> from);

	CompletableFuture<Optional<Message<T>>> receiveFifoAsync(Consumer<SqsReceiveOptions.Fifo<T>> from);

	CompletableFuture<Collection<Message<T>>> receiveManyAsync(Consumer<SqsReceiveOptions.Standard<T>> from);

	CompletableFuture<Collection<Message<T>>> receiveManyFifoAsync(Consumer<SqsReceiveOptions.Fifo<T>> from);

}
