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
import io.awspring.cloud.sqs.listener.sink.FanOutMessageSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.source.MessageSource;
import io.awspring.cloud.sqs.listener.source.SqsMessageSource;
import java.time.Duration;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class StandardSqsComponentFactory<T> implements ContainerComponentFactory<T> {

	private static final Duration DEFAULT_STANDARD_SQS_ACK_INTERVAL = Duration.ofSeconds(1);

	private static final Integer DEFAULT_STANDARD_SQS_ACK_THRESHOLD = 10;

	private static final AcknowledgementOrdering DEFAULT_STANDARD_SQS_ACK_ORDERING = AcknowledgementOrdering.PARALLEL;

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
	public AcknowledgementProcessor<T> createAcknowledgementProcessor(ContainerOptions options) {
		return options.getAcknowledgementInterval() == Duration.ZERO && options.getAcknowledgementThreshold() == 0
				? createAndConfigureImmediateProcessor(options)
				: createAndConfigureBatchingProcessor(options);
	}

	protected ImmediateAcknowledgementProcessor<T> createAndConfigureImmediateProcessor(ContainerOptions options) {
		ImmediateAcknowledgementProcessor<T> processor = createImmediateProcessorInstance();
		processor.setMaxAcknowledgementsPerBatch(10);
		ConfigUtils.INSTANCE.acceptIfNotNullOrElse(processor::setAcknowledgementOrdering,
				options.getAcknowledgementOrdering(), DEFAULT_STANDARD_SQS_ACK_ORDERING);
		return processor;
	}

	protected ImmediateAcknowledgementProcessor<T> createImmediateProcessorInstance() {
		return new ImmediateAcknowledgementProcessor<>();
	}

	protected AcknowledgementProcessor<T> createAndConfigureBatchingProcessor(ContainerOptions options) {
		BatchingAcknowledgementProcessor<T> processor = createBatchingProcessorInstance();
		processor.setMaxAcknowledgementsPerBatch(10);
		ConfigUtils.INSTANCE
				.acceptIfNotNullOrElse(processor::setAcknowledgementInterval, options.getAcknowledgementInterval(),
						DEFAULT_STANDARD_SQS_ACK_INTERVAL)
				.acceptIfNotNullOrElse(processor::setAcknowledgementThreshold, options.getAcknowledgementThreshold(),
						DEFAULT_STANDARD_SQS_ACK_THRESHOLD)
				.acceptIfNotNullOrElse(processor::setAcknowledgementOrdering, options.getAcknowledgementOrdering(),
						DEFAULT_STANDARD_SQS_ACK_ORDERING);
		return processor;
	}

	protected BatchingAcknowledgementProcessor<T> createBatchingProcessorInstance() {
		return new BatchingAcknowledgementProcessor<>();
	}

}
