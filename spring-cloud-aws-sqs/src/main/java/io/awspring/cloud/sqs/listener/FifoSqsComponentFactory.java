package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchingAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.ImmediateAcknowledgementProcessor;
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

	// Immediate (sync) ack
	private static final Duration DEFAULT_FIFO_SQS_ACK_INTERVAL = Duration.ZERO;

	private static final Integer DEFAULT_FIFO_SQS_ACK_THRESHOLD = 0;

	private static final AcknowledgementOrdering DEFAULT_FIFO_SQS_ACK_ORDERING = AcknowledgementOrdering.ORDERED;

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
	public AcknowledgementProcessor<T> createAcknowledgementProcessor(ContainerOptions options) {
		return (options.getAcknowledgementInterval() == null || options.getAcknowledgementInterval() == Duration.ZERO)
			&& (options.getAcknowledgementThreshold() == null || options.getAcknowledgementThreshold() == 0)
				? createAndConfigureImmediateProcessor(options)
				: createAndConfigureBatchingAckProcessor(options);
	}

	private ImmediateAcknowledgementProcessor<T> createAndConfigureImmediateProcessor(ContainerOptions options) {
		ImmediateAcknowledgementProcessor<T> processor = new ImmediateAcknowledgementProcessor<>();
		ConfigUtils.INSTANCE
			.acceptIfNotNullOrElse(processor::setAcknowledgementOrdering, options.getAcknowledgementOrdering(), DEFAULT_FIFO_SQS_ACK_ORDERING);
		return processor;
	}

	private BatchingAcknowledgementProcessor<T> createAndConfigureBatchingAckProcessor(ContainerOptions options) {
		BatchingAcknowledgementProcessor<T> processor = new BatchingAcknowledgementProcessor<>();
		ConfigUtils.INSTANCE
			.acceptIfNotNullOrElse(processor::setAcknowledgementInterval, options.getAcknowledgementInterval(), DEFAULT_FIFO_SQS_ACK_INTERVAL)
			.acceptIfNotNullOrElse(processor::setAcknowledgementThreshold, options.getAcknowledgementThreshold(), DEFAULT_FIFO_SQS_ACK_THRESHOLD)
			.acceptIfNotNullOrElse(processor::setAcknowledgementOrdering, options.getAcknowledgementOrdering(), DEFAULT_FIFO_SQS_ACK_ORDERING);
		return processor;
	}

}
