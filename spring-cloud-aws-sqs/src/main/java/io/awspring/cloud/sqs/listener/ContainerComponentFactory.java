package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.listener.acknowledgement.AckHandler;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.MessageSource;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface ContainerComponentFactory<T> extends ConfigurableContainerComponent {

	MessageSource<T> createMessageSource();

	MessageSink<T> createMessageSink();

	AckHandler<T> createAckHandler();

}
