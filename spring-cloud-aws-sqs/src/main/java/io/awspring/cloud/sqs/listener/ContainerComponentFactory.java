package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.listener.acknowledgement.AckHandler;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.MessageSource;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ContainerComponentFactory<T> {

	MessageSource<T> createMessageSource(ContainerOptions options);

	MessageSink<T> createMessageSink(ContainerOptions options);

	AckHandler<T> createAckHandler(ContainerOptions options);

}
