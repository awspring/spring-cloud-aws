package io.awspring.cloud.sqs.listener.acknowledgement;

import org.springframework.messaging.Message;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface AcknowledgementExecutor<T> {

	CompletableFuture<Void> execute(Collection<Message<T>> messagesToAck);

}
