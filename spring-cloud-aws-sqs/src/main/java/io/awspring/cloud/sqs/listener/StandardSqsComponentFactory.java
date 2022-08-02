package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.listener.acknowledgement.AckHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.OnSuccessAckHandler;
import io.awspring.cloud.sqs.listener.sink.BatchMessageSink;
import io.awspring.cloud.sqs.listener.sink.FanOutMessageSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.listener.source.SqsMessageSource;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class StandardSqsComponentFactory<T> implements ContainerComponentFactory<T> {

	@Override
	public MessageSource<T> createMessageSource(ContainerOptions options) {
		return new SqsMessageSource<>();
	}

	@Override
	public MessageSink<T> createMessageSink(ContainerOptions options) {
		return MessageDeliveryStrategy.SINGLE_MESSAGE.equals(options.getMessageDeliveryStrategy())
			? new FanOutMessageSink<>()
			: new BatchMessageSink<>();
	}

	@Override
	public AckHandler<T> createAckHandler(ContainerOptions options) {
		return new OnSuccessAckHandler<>();
	}

}
