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
import io.awspring.cloud.sqs.FifoUtils;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchingAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.ImmediateAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.sink.*;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.listener.source.StandardSqsMessageSource;
import java.time.Duration;
import java.util.Collection;
import org.springframework.util.Assert;

/**
 * A {@link ContainerComponentFactory} implementation for Standard SQS queues.
 * @author Tomaz Fernandes
 * @since 3.0
 * @see FifoSqsComponentFactory
 */
public class StandardSqsComponentFactory<T> implements ContainerComponentFactory<T, SqsContainerOptions> {

	private static final Duration DEFAULT_STANDARD_SQS_ACK_INTERVAL = Duration.ofSeconds(1);

	private static final Integer DEFAULT_STANDARD_SQS_ACK_THRESHOLD = 10;

	private static final AcknowledgementOrdering DEFAULT_STANDARD_SQS_ACK_ORDERING = AcknowledgementOrdering.PARALLEL;

	@Override
	public boolean supports(Collection<String> queueNames, SqsContainerOptions options) {
		return FifoUtils.areNotFifo(queueNames);
	}

	@Override
	public MessageSource<T> createMessageSource(SqsContainerOptions options) {
		return new StandardSqsMessageSource<>();
	}

	// @formatter:off
	@Override
	public MessageSink<T> createMessageSink(SqsContainerOptions options) {
		return ListenerMode.SINGLE_MESSAGE.equals(options.getListenerMode())
				? new FanOutMessageSink<>() : new BatchMessageSink<>();
	}

	// @formatter:on
	@Override
	public AcknowledgementProcessor<T> createAcknowledgementProcessor(SqsContainerOptions options) {
		validateAcknowledgementOrdering(options);
		return options.getAcknowledgementInterval() == Duration.ZERO
				&& (options.getAcknowledgementThreshold() == null || options.getAcknowledgementThreshold() == 0)
						? createAndConfigureImmediateProcessor(options)
						: createAndConfigureBatchingProcessor(options);
	}

	private void validateAcknowledgementOrdering(SqsContainerOptions options) {
		Assert.isTrue(!AcknowledgementOrdering.ORDERED_BY_GROUP.equals(options.getAcknowledgementOrdering()),
				"Standard SQS queues are not compatible with " + AcknowledgementOrdering.ORDERED_BY_GROUP);
	}

	private AcknowledgementProcessor<T> createAndConfigureBatchingProcessor(SqsContainerOptions options) {
		return configureBatchingAcknowledgementProcessor(options, createBatchingProcessorInstance());
	}

	protected ImmediateAcknowledgementProcessor<T> createAndConfigureImmediateProcessor(SqsContainerOptions options) {
		return configureImmediateAcknowledgementProcessor(createImmediateProcessorInstance(), options);
	}

	protected ImmediateAcknowledgementProcessor<T> createImmediateProcessorInstance() {
		return new ImmediateAcknowledgementProcessor<>();
	}

	protected BatchingAcknowledgementProcessor<T> createBatchingProcessorInstance() {
		return new BatchingAcknowledgementProcessor<>();
	}

	protected ImmediateAcknowledgementProcessor<T> configureImmediateAcknowledgementProcessor(
			ImmediateAcknowledgementProcessor<T> processor, SqsContainerOptions options) {
		processor.setMaxAcknowledgementsPerBatch(10);
		SqsContainerOptionsBuilder builder = options.toBuilder();
		ConfigUtils.INSTANCE.acceptIfNotNullOrElse(builder::acknowledgementOrdering,
				options.getAcknowledgementOrdering(), DEFAULT_STANDARD_SQS_ACK_ORDERING);
		processor.configure(builder.build());
		return processor;
	}

	protected BatchingAcknowledgementProcessor<T> configureBatchingAcknowledgementProcessor(SqsContainerOptions options,
			BatchingAcknowledgementProcessor<T> processor) {
		processor.setMaxAcknowledgementsPerBatch(10);
		SqsContainerOptionsBuilder builder = options.toBuilder();
		ConfigUtils.INSTANCE
				.acceptIfNotNullOrElse(builder::acknowledgementInterval, options.getAcknowledgementInterval(),
						DEFAULT_STANDARD_SQS_ACK_INTERVAL)
				.acceptIfNotNullOrElse(builder::acknowledgementThreshold, options.getAcknowledgementThreshold(),
						DEFAULT_STANDARD_SQS_ACK_THRESHOLD)
				.acceptIfNotNullOrElse(builder::acknowledgementOrdering, options.getAcknowledgementOrdering(),
						DEFAULT_STANDARD_SQS_ACK_ORDERING);
		processor.configure(builder.build());
		return processor;
	}

}
