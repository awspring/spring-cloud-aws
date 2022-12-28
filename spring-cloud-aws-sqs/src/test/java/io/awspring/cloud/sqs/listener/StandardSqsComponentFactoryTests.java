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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchingAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.ImmediateAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.sink.BatchMessageSink;
import io.awspring.cloud.sqs.listener.sink.FanOutMessageSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
class StandardSqsComponentFactoryTests {

	@Test
	void shouldCreateGroupingSink() {
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = new StandardSqsComponentFactory<>();
		MessageSink<Object> messageSink = componentFactory.createMessageSink(SqsContainerOptions.builder().build());
		assertThat(messageSink).isInstanceOf(FanOutMessageSink.class);
	}

	@Test
	void shouldCreateBatchSink() {
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = new StandardSqsComponentFactory<>();
		MessageSink<Object> messageSink = componentFactory
				.createMessageSink(SqsContainerOptions.builder().listenerMode(ListenerMode.BATCH).build());
		assertThat(messageSink).isInstanceOf(BatchMessageSink.class);
	}

	@Test
	void shouldCreateAcknowledgementProcessorWithDefaults() {
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = new StandardSqsComponentFactory<>();
		SqsContainerOptions options = SqsContainerOptions.builder().build();
		AcknowledgementProcessor<Object> processor = componentFactory.createAcknowledgementProcessor(options);
		assertThat(processor).isInstanceOf(BatchingAcknowledgementProcessor.class).extracting("acknowledgementOrdering")
				.isEqualTo(AcknowledgementOrdering.PARALLEL);
		assertThat(processor).extracting("messageGroupingFunction").isNull();
		assertThat(processor).extracting("ackThreshold").isEqualTo(10);
		assertThat(processor).extracting("ackInterval").isEqualTo(Duration.ofSeconds(1));
	}

	@Test
	void shouldCreateImmediateAcknowledgementProcessor() {
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = new StandardSqsComponentFactory<>();
		Duration acknowledgementInterval = Duration.ZERO;
		int acknowledgementThreshold = 0;
		SqsContainerOptions options = SqsContainerOptions.builder().acknowledgementInterval(acknowledgementInterval)
				.acknowledgementThreshold(acknowledgementThreshold).build();
		AcknowledgementProcessor<Object> processor = componentFactory.createAcknowledgementProcessor(options);
		assertThat(processor).isInstanceOf(ImmediateAcknowledgementProcessor.class)
				.extracting("acknowledgementOrdering").isEqualTo(AcknowledgementOrdering.PARALLEL);
		assertThat(processor).extracting("messageGroupingFunction").isNull();
	}

	@Test
	void shouldThrowIfOrderedByGroup() {
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = new StandardSqsComponentFactory<>();
		SqsContainerOptions options = SqsContainerOptions.builder()
				.acknowledgementOrdering(AcknowledgementOrdering.ORDERED_BY_GROUP).build();
		assertThatThrownBy(() -> componentFactory.createAcknowledgementProcessor(options))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldCreateBatchingAcknowledgementProcessorOrdered() {
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = new StandardSqsComponentFactory<>();
		SqsContainerOptions options = SqsContainerOptions.builder()
				.acknowledgementOrdering(AcknowledgementOrdering.ORDERED).build();
		AcknowledgementProcessor<Object> processor = componentFactory.createAcknowledgementProcessor(options);
		assertThat(processor).isInstanceOf(BatchingAcknowledgementProcessor.class).extracting("acknowledgementOrdering")
				.isEqualTo(AcknowledgementOrdering.ORDERED);
		assertThat(processor).extracting("ackThreshold").isEqualTo(10);
		assertThat(processor).extracting("ackInterval").isEqualTo(Duration.ofSeconds(1));
		assertThat(processor).extracting("messageGroupingFunction").isNull();
	}

	@Test
	void shouldCreateImmediateAcknowledgementProcessorOrdered() {
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = new StandardSqsComponentFactory<>();
		Duration acknowledgementInterval = Duration.ZERO;
		int acknowledgementThreshold = 0;
		SqsContainerOptions options = SqsContainerOptions.builder()
				.acknowledgementOrdering(AcknowledgementOrdering.ORDERED)
				.acknowledgementInterval(acknowledgementInterval).acknowledgementThreshold(acknowledgementThreshold)
				.build();
		AcknowledgementProcessor<Object> processor = componentFactory.createAcknowledgementProcessor(options);
		assertThat(processor).isInstanceOf(ImmediateAcknowledgementProcessor.class)
				.extracting("acknowledgementOrdering").isEqualTo(AcknowledgementOrdering.ORDERED);
		assertThat(processor).extracting("messageGroupingFunction").isNull();
	}

}
