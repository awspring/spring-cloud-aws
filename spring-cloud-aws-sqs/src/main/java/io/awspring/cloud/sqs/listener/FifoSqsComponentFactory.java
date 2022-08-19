/*
 * Copyright 2013-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.sqs.listener;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchingAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.ImmediateAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.sink.BatchMessageSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.sink.OrderedMessageSink;
import io.awspring.cloud.sqs.listener.sink.adapter.MessageGroupingSinkAdapter;
import io.awspring.cloud.sqs.listener.sink.adapter.MessageVisibilityExtendingSinkAdapter;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.listener.source.SqsMessageSource;
import java.time.Duration;
import java.util.function.Function;
import org.springframework.messaging.Message;

/**
 * {@link ContainerComponentFactory} implementation for creating components for FIFO queues.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see StandardSqsComponentFactory
 */
public class FifoSqsComponentFactory<T> implements ContainerComponentFactory<T> {

	// Defaults to immediate (sync) ack
	private static final Duration DEFAULT_FIFO_SQS_ACK_INTERVAL = Duration.ZERO;

	private static final Integer DEFAULT_FIFO_SQS_ACK_THRESHOLD = 0;

	// Since immediate acks hold the thread until done, we can execute in parallel and use processing order
	private static final AcknowledgementOrdering DEFAULT_FIFO_SQS_ACK_ORDERING_IMMEDIATE = AcknowledgementOrdering.PARALLEL;

	private static final AcknowledgementOrdering DEFAULT_FIFO_SQS_ACK_ORDERING_BATCHING = AcknowledgementOrdering.ORDERED;

	@Override
	public MessageSource<T> createMessageSource(ContainerOptions options) {
		return new SqsMessageSource<>();
	}

	@Override
	public MessageSink<T> createMessageSink(ContainerOptions options) {
		MessageSink<T> deliverySink = createDeliverySink(options.getMessageDeliveryStrategy());
		return new MessageGroupingSinkAdapter<>(
				maybeWrapWithVisibilityAdapter(deliverySink, options.getMessageVisibility()),
				getMessageGroupingHeader());
	}

	// @formatter:off
	private MessageSink<T> createDeliverySink(MessageDeliveryStrategy messageDeliveryStrategy) {
		return MessageDeliveryStrategy.SINGLE_MESSAGE.equals(messageDeliveryStrategy)
			? new OrderedMessageSink<>()
			: new BatchMessageSink<>();
	}

	private MessageSink<T> maybeWrapWithVisibilityAdapter(MessageSink<T> deliverySink, Duration messageVisibility) {
		return messageVisibility != null
			? addMessageVisibilityExtendingSinkAdapter(deliverySink, messageVisibility)
			: deliverySink;
	}

	private MessageVisibilityExtendingSinkAdapter<T> addMessageVisibilityExtendingSinkAdapter(
			MessageSink<T> deliverySink, Duration messageVisibility) {
		MessageVisibilityExtendingSinkAdapter<T> visibilityAdapter = new MessageVisibilityExtendingSinkAdapter<>(
				deliverySink);
		visibilityAdapter.setMessageVisibility(messageVisibility);
		return visibilityAdapter;
	}

	private Function<Message<T>, String> getMessageGroupingHeader() {
		return message -> message.getHeaders().get(SqsHeaders.MessageSystemAttribute.SQS_MESSAGE_GROUP_ID_HEADER,
				String.class);
	}

	@Override
	public AcknowledgementProcessor<T> createAcknowledgementProcessor(ContainerOptions options) {
		return (options.getAcknowledgementInterval() == null || DEFAULT_FIFO_SQS_ACK_INTERVAL.equals(options.getAcknowledgementInterval()))
			&& (options.getAcknowledgementThreshold() == null || DEFAULT_FIFO_SQS_ACK_THRESHOLD.equals(options.getAcknowledgementThreshold()))
				? createAndConfigureImmediateProcessor(options)
				: createAndConfigureBatchingAckProcessor(options);
	}
	// @formatter:on

	protected ImmediateAcknowledgementProcessor<T> createAndConfigureImmediateProcessor(ContainerOptions options) {
		ImmediateAcknowledgementProcessor<T> processor = new ImmediateAcknowledgementProcessor<>();
		processor.setMaxAcknowledgementsPerBatch(10);
		ConfigUtils.INSTANCE.acceptIfNotNullOrElse(processor::setAcknowledgementOrdering,
				options.getAcknowledgementOrdering(), DEFAULT_FIFO_SQS_ACK_ORDERING_IMMEDIATE);
		return processor;
	}

	protected BatchingAcknowledgementProcessor<T> createAndConfigureBatchingAckProcessor(ContainerOptions options) {
		BatchingAcknowledgementProcessor<T> processor = new BatchingAcknowledgementProcessor<>();
		processor.setMaxAcknowledgementsPerBatch(10);
		ConfigUtils.INSTANCE
				.acceptIfNotNullOrElse(processor::setAcknowledgementInterval, options.getAcknowledgementInterval(),
						DEFAULT_FIFO_SQS_ACK_INTERVAL)
				.acceptIfNotNullOrElse(processor::setAcknowledgementThreshold, options.getAcknowledgementThreshold(),
						DEFAULT_FIFO_SQS_ACK_THRESHOLD)
				.acceptIfNotNullOrElse(processor::setAcknowledgementOrdering, options.getAcknowledgementOrdering(),
						DEFAULT_FIFO_SQS_ACK_ORDERING_BATCHING);
		return processor;
	}

}
