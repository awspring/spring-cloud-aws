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
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.BatchingAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.acknowledgement.ImmediateAcknowledgementProcessor;
import io.awspring.cloud.sqs.listener.sink.BatchMessageSink;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.listener.sink.OrderedMessageSink;
import io.awspring.cloud.sqs.listener.sink.adapter.MessageGroupingSinkAdapter;
import io.awspring.cloud.sqs.listener.sink.adapter.MessageVisibilityExtendingSinkAdapter;
import java.time.Duration;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FifoSqsComponentFactory}.
 *
 * @author Tomaz Fernandes
 */
class FifoSqsComponentFactoryTests {

	@Test
	void shouldCreateGroupingSink() {
		FifoSqsComponentFactory<Object> componentFactory = new FifoSqsComponentFactory<>();
		MessageSink<Object> messageSink = componentFactory.createMessageSink(SqsContainerOptions.builder().build());
		assertThat(messageSink).isInstanceOf(MessageGroupingSinkAdapter.class)
				.asInstanceOf(type(MessageGroupingSinkAdapter.class)).extracting("delegate")
				.isInstanceOf(OrderedMessageSink.class);
	}

	@Test
	void shouldCreateBatchSink() {
		FifoSqsComponentFactory<Object> componentFactory = new FifoSqsComponentFactory<>();
		MessageSink<Object> messageSink = componentFactory
				.createMessageSink(SqsContainerOptions.builder().listenerMode(ListenerMode.BATCH).build());
		assertThat(messageSink).isInstanceOf(MessageGroupingSinkAdapter.class)
				.asInstanceOf(type(MessageGroupingSinkAdapter.class)).extracting("delegate")
				.isInstanceOf(BatchMessageSink.class);
	}

	@Test
	void shouldCreateGroupingSinkWithVisibility() {
		FifoSqsComponentFactory<Object> componentFactory = new FifoSqsComponentFactory<>();
		Duration visbilityDuration = Duration.ofSeconds(1);
		MessageSink<Object> messageSink = componentFactory
				.createMessageSink(SqsContainerOptions.builder().messageVisibility(visbilityDuration).build());
		AbstractObjectAssert<?, ?> visibilitySinkAssertion = assertThat(messageSink)
				.isInstanceOf(MessageGroupingSinkAdapter.class).asInstanceOf(type(MessageGroupingSinkAdapter.class))
				.extracting("delegate").isInstanceOf(MessageVisibilityExtendingSinkAdapter.class);
		visibilitySinkAssertion.extracting("messageVisibility").isEqualTo((int) visbilityDuration.getSeconds());
		visibilitySinkAssertion.extracting("delegate").isInstanceOf(OrderedMessageSink.class);
	}

	@Test
	void shouldCreateAcknowledgementProcessorWithDefaults() {
		FifoSqsComponentFactory<Object> componentFactory = new FifoSqsComponentFactory<>();

		SqsContainerOptions options = SqsContainerOptions.builder().build();
		AcknowledgementProcessor<Object> processor = componentFactory.createAcknowledgementProcessor(options);
		assertThat(processor).isInstanceOf(ImmediateAcknowledgementProcessor.class)
				.extracting("acknowledgementOrdering").isEqualTo(AcknowledgementOrdering.PARALLEL);
		assertThat(processor).extracting("messageGroupingFunction").isNull();
	}

	@Test
	void shouldCreateBatchingAcknowledgementProcessor() {
		FifoSqsComponentFactory<Object> componentFactory = new FifoSqsComponentFactory<>();

		Duration acknowledgementInterval = Duration.ofSeconds(10);
		int acknowledgementThreshold = 10;
		SqsContainerOptions options = SqsContainerOptions.builder().acknowledgementInterval(acknowledgementInterval)
				.acknowledgementThreshold(acknowledgementThreshold).build();
		AcknowledgementProcessor<Object> processor = componentFactory.createAcknowledgementProcessor(options);
		assertThat(processor).isInstanceOf(BatchingAcknowledgementProcessor.class).extracting("acknowledgementOrdering")
				.isEqualTo(AcknowledgementOrdering.ORDERED);
		assertThat(processor).extracting("ackThreshold").isEqualTo(acknowledgementThreshold);
		assertThat(processor).extracting("ackInterval").isEqualTo(acknowledgementInterval);
		assertThat(processor).extracting("messageGroupingFunction").isNull();

	}

	@Test
	void shouldCreateBatchingAcknowledgementProcessorOrderedByGroup() {
		FifoSqsComponentFactory<Object> componentFactory = new FifoSqsComponentFactory<>();

		Duration acknowledgementInterval = Duration.ofSeconds(10);
		int acknowledgementThreshold = 10;
		SqsContainerOptions options = SqsContainerOptions.builder().acknowledgementInterval(acknowledgementInterval)
				.acknowledgementThreshold(acknowledgementThreshold)
				.acknowledgementOrdering(AcknowledgementOrdering.ORDERED_BY_GROUP).build();
		AcknowledgementProcessor<Object> processor = componentFactory.createAcknowledgementProcessor(options);
		assertThat(processor).isInstanceOf(BatchingAcknowledgementProcessor.class).extracting("acknowledgementOrdering")
				.isEqualTo(AcknowledgementOrdering.ORDERED_BY_GROUP);
		assertThat(processor).extracting("ackThreshold").isEqualTo(acknowledgementThreshold);
		assertThat(processor).extracting("ackInterval").isEqualTo(acknowledgementInterval);
		assertThat(processor).extracting("messageGroupingFunction").isNotNull();

	}

	@Test
	void shouldCreateImmediateAcknowledgementProcessorOrderedByGroup() {
		FifoSqsComponentFactory<Object> componentFactory = new FifoSqsComponentFactory<>();

		SqsContainerOptions options = SqsContainerOptions.builder()
				.acknowledgementOrdering(AcknowledgementOrdering.ORDERED_BY_GROUP).build();
		AcknowledgementProcessor<Object> processor = componentFactory.createAcknowledgementProcessor(options);
		assertThat(processor).isInstanceOf(ImmediateAcknowledgementProcessor.class)
				.extracting("acknowledgementOrdering").isEqualTo(AcknowledgementOrdering.ORDERED_BY_GROUP);
		assertThat(processor).extracting("messageGroupingFunction").isNotNull();
	}

}
