package io.awspring.cloud.sqs.listener.acknowledgement;

import org.springframework.messaging.Message;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class ImmediateAcknowledgementProcessor<T> extends AbstractAcknowledgementProcessor<T> {

	@Override
	protected CompletableFuture<Void> doOnAcknowledge(Message<T> message) {
		return execute(Collections.singletonList(message));
	}

	@Override
	protected CompletableFuture<Void> doOnAcknowledge(Collection<Message<T>> messages) {
		return execute(messages);
	}

}
