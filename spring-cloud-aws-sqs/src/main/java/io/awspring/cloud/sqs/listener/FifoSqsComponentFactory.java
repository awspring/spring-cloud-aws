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
import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchingAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.ImmediateAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.sink.BatchMessageSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.sink.OrderedMessageSink;
import io.awspring.cloud.sqs.listener.sink.adapter.MessageGroupingSinkAdapter;
import io.awspring.cloud.sqs.listener.sink.adapter.MessageVisibilityExtendingSinkAdapter;
import io.awspring.cloud.sqs.listener.source.FifoSqsMessageSource;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Function;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

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
	public boolean supports(Collection<String> queueNames, ContainerOptions options) {
		return queueNames.stream().allMatch(name -> name.endsWith(".fifo"));
	}

	@Override
	public MessageSource<T> createMessageSource(ContainerOptions options) {
		return new FifoSqsMessageSource<>();
	}

	@Override
	public MessageSink<T> createMessageSink(ContainerOptions options) {
		MessageSink<T> deliverySink = createDeliverySink(options.getListenerMode());
		return new MessageGroupingSinkAdapter<>(
				maybeWrapWithVisibilityAdapter(deliverySink, options.getMessageVisibility()),
				getMessageGroupingFunction());
	}

	// @formatter:off
	private MessageSink<T> createDeliverySink(ListenerMode listenerMode) {
		return ListenerMode.SINGLE_MESSAGE.equals(listenerMode)
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

	private Function<Message<T>, String> getMessageGroupingFunction() {
		return message -> MessageHeaderUtils.getHeaderAsString(message, SqsHeaders.MessageSystemAttribute.SQS_MESSAGE_GROUP_ID_HEADER);
	}

	@Override
	public AcknowledgementProcessor<T> createAcknowledgementProcessor(ContainerOptions options) {
		validateFifoOptions(options);
		return hasNoAcknowledgementIntervalSet(options) && hasNoAcknowledgementThresholdSet(options)
				? createAndConfigureImmediateProcessor(options)
				: createAndConfigureBatchingAckProcessor(options);
	}

	private void validateFifoOptions(ContainerOptions options) {
		Assert.isTrue(options.getMessageSystemAttributeNames().contains(QueueAttributeName.ALL.toString())
				|| options.getMessageSystemAttributeNames().contains(MessageSystemAttributeName.MESSAGE_GROUP_ID.toString())
		, "MessageSystemAttributeName.MESSAGE_GROUP_ID is required for FIFO queues.");
	}

	private boolean hasNoAcknowledgementThresholdSet(ContainerOptions options) {
		return options.getAcknowledgementThreshold() == null || DEFAULT_FIFO_SQS_ACK_THRESHOLD.equals(options.getAcknowledgementThreshold());
	}

	private boolean hasNoAcknowledgementIntervalSet(ContainerOptions options) {
		return options.getAcknowledgementInterval() == null || DEFAULT_FIFO_SQS_ACK_INTERVAL.equals(options.getAcknowledgementInterval());
	}
	// @formatter:on

	private ImmediateAcknowledgementProcessor<T> createAndConfigureImmediateProcessor(ContainerOptions options) {
		return configureImmediateProcessor(createImmediateProcessorInstance(), options);
	}

	private BatchingAcknowledgementProcessor<T> createAndConfigureBatchingAckProcessor(ContainerOptions options) {
		return configureBatchingAckProcessor(options, createBatchingProcessorInstance());
	}

	protected ImmediateAcknowledgementProcessor<T> createImmediateProcessorInstance() {
		return new ImmediateAcknowledgementProcessor<>();
	}

	protected BatchingAcknowledgementProcessor<T> createBatchingProcessorInstance() {
		return new BatchingAcknowledgementProcessor<>();
	}

	protected ImmediateAcknowledgementProcessor<T> configureImmediateProcessor(
			ImmediateAcknowledgementProcessor<T> processor, ContainerOptions options) {
		processor.setMaxAcknowledgementsPerBatch(10);
		if (AcknowledgementOrdering.ORDERED_BY_GROUP.equals(options.getAcknowledgementOrdering())) {
			processor.setMessageGroupingFunction(getMessageGroupingFunction());
		}
		ContainerOptions.Builder builder = options.toBuilder();
		ConfigUtils.INSTANCE.acceptIfNotNullOrElse(builder::acknowledgementOrdering,
				options.getAcknowledgementOrdering(), DEFAULT_FIFO_SQS_ACK_ORDERING_IMMEDIATE);
		processor.configure(builder.build());
		return processor;
	}

	protected BatchingAcknowledgementProcessor<T> configureBatchingAckProcessor(ContainerOptions options,
			BatchingAcknowledgementProcessor<T> processor) {
		ContainerOptions.Builder builder = options.toBuilder();
		ConfigUtils.INSTANCE
				.acceptIfNotNullOrElse(builder::acknowledgementInterval, options.getAcknowledgementInterval(),
						DEFAULT_FIFO_SQS_ACK_INTERVAL)
				.acceptIfNotNullOrElse(builder::acknowledgementThreshold, options.getAcknowledgementThreshold(),
						DEFAULT_FIFO_SQS_ACK_THRESHOLD)
				.acceptIfNotNullOrElse(builder::acknowledgementOrdering, options.getAcknowledgementOrdering(),
						DEFAULT_FIFO_SQS_ACK_ORDERING_BATCHING);
		processor.setMaxAcknowledgementsPerBatch(10);
		if (AcknowledgementOrdering.ORDERED_BY_GROUP.equals(options.getAcknowledgementOrdering())) {
			processor.setMessageGroupingFunction(getMessageGroupingFunction());
		}
		processor.configure(builder.build());
		return processor;
	}

}
