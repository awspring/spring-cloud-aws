package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.MessageHeaderUtils;
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
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;
import java.util.function.Function;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class FifoSqsComponentFactory<T> implements ContainerComponentFactory<T> {

	private MessageDeliveryStrategy messageDeliveryStrategy;

	private Duration messageVisibility;

	@Override
	public void configure(ContainerOptions containerOptions) {
		this.messageDeliveryStrategy = containerOptions.getMessageDeliveryStrategy();
		this.messageVisibility = containerOptions.getMessageVisibility();
	}

	@Override
	public MessageSource<T> createMessageSource() {
		return new SqsMessageSource<>();
	}

	@Override
	public MessageSink<T> createMessageSink() {
		return new MessageGroupingSinkAdapter<>(maybeWrapWithVisibilityAdapter(createDeliverySink()), getMessageHeader());
	}

	private MessageSink<T> createDeliverySink() {
		return MessageDeliveryStrategy.SINGLE_MESSAGE.equals(this.messageDeliveryStrategy)
			? new OrderedMessageListeningSink<>()
			: new BatchMessageSink<>();
	}

	private MessageSink<T> maybeWrapWithVisibilityAdapter(MessageSink<T> deliverySink) {
		return this.messageVisibility != null
			? addMessageVisibilityExtendingSinkAdapter(deliverySink)
			: deliverySink;
	}

	private MessageVisibilityExtendingSinkAdapter<T> addMessageVisibilityExtendingSinkAdapter(MessageSink<T> deliverySink) {
		MessageVisibilityExtendingSinkAdapter<T> visibilityAdapter = new MessageVisibilityExtendingSinkAdapter<>(deliverySink);
		visibilityAdapter.setVisibilityStrategy(MessageVisibilityExtendingSinkAdapter.Strategy.REMAINING_ORIGINAL_BATCH_MESSAGES);
		visibilityAdapter.setMessageVisibility(this.messageVisibility);
		return visibilityAdapter;
	}

	private Function<Message<T>, String> getMessageHeader() {
		return message -> MessageHeaderUtils.getHeader(message, SqsMessageHeaders.SQS_GROUP_ID_HEADER, String.class);
	}

	@Override
	public AckHandler<T> createAckHandler() {
		return new OnSuccessAckHandler<>();
	}

}
