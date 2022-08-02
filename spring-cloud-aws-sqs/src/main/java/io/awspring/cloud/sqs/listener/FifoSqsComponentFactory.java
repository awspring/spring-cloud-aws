package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.listener.acknowledgement.AckHandler;
import io.awspring.cloud.sqs.listener.acknowledgement.OnSuccessAckHandler;
import io.awspring.cloud.sqs.listener.sink.BatchMessageSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.sink.OrderedMessageListeningSink;
import io.awspring.cloud.sqs.listener.sink.adapter.MessageGroupingSinkAdapter;
import io.awspring.cloud.sqs.listener.sink.adapter.MessageVisibilityExtendingSinkAdapter;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.listener.source.SqsMessageSource;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.function.Function;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class FifoSqsComponentFactory<T> implements ContainerComponentFactory<T> {

	@Override
	public MessageSource<T> createMessageSource(ContainerOptions options) {
		return new SqsMessageSource<>();
	}

	@Override
	public MessageSink<T> createMessageSink(ContainerOptions options) {
		MessageSink<T> deliverySink = createDeliverySink(options.getMessageDeliveryStrategy());
		return new MessageGroupingSinkAdapter<>(maybeWrapWithVisibilityAdapter(deliverySink, options.getMessageVisibility()), getMessageGroupingHeader());
	}

	private MessageSink<T> createDeliverySink(MessageDeliveryStrategy messageDeliveryStrategy) {
		return MessageDeliveryStrategy.SINGLE_MESSAGE.equals(messageDeliveryStrategy)
			? new OrderedMessageListeningSink<>()
			: new BatchMessageSink<>();
	}

	private MessageSink<T> maybeWrapWithVisibilityAdapter(MessageSink<T> deliverySink, Duration messageVisibility) {
		return messageVisibility != null
			? addMessageVisibilityExtendingSinkAdapter(deliverySink, messageVisibility)
			: deliverySink;
	}

	private MessageVisibilityExtendingSinkAdapter<T> addMessageVisibilityExtendingSinkAdapter(MessageSink<T> deliverySink, Duration messageVisibility) {
		MessageVisibilityExtendingSinkAdapter<T> visibilityAdapter = new MessageVisibilityExtendingSinkAdapter<>(deliverySink);
		visibilityAdapter.setMessageVisibility(messageVisibility);
		return visibilityAdapter;
	}

	private Function<Message<T>, String> getMessageGroupingHeader() {
		return message -> message.getHeaders().get(SqsHeaders.MessageSystemAttribute.SQS_MESSAGE_GROUP_ID_HEADER, String.class);
	}

	@Override
	public AckHandler<T> createAckHandler(ContainerOptions containerOptions) {
		return new OnSuccessAckHandler<>();
	}

}
