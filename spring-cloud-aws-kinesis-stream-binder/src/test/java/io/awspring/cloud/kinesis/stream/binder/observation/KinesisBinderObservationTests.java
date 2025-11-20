/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.kinesis.stream.binder.observation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import brave.handler.SpanHandler;
import brave.test.TestSpanHandler;
import io.awspring.cloud.kinesis.integration.KinesisHeaders;
import io.awspring.cloud.kinesis.stream.binder.LocalstackContainerTest;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.brave.bridge.BraveFinishedSpan;
import io.micrometer.tracing.test.simple.SpansAssert;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.management.observation.IntegrationObservation;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Flux;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
		"spring.cloud.function.definition=kinesisSupplier;kinesisConsumer",
		"spring.cloud.stream.bindings.kinesisSupplier-out-0.destination="
				+ KinesisBinderObservationTests.KINESIS_STREAM,
		"spring.cloud.stream.kinesis.bindings.kinesisSupplier-out-0.producer.recordMetadataChannel=producerResultsChannel",
		"spring.cloud.stream.bindings.kinesisConsumer-in-0.destination=" + KinesisBinderObservationTests.KINESIS_STREAM,
		"spring.cloud.stream.bindings.kinesisConsumer-in-0.group=observation-group",
		"spring.cloud.stream.kinesis.binder.enable-observation=true",
		"logging.level.org.springframework.cloud.stream.binder.kinesis.observation=debug",
		"management.tracing.sampling.probability=1.0" })
@AutoConfigureObservability
@DirtiesContext
public class KinesisBinderObservationTests implements LocalstackContainerTest {

	private static final Log LOGGER = LogFactory.getLog(KinesisBinderObservationTests.class);

	static final String KINESIS_STREAM = "test_observation_stream";

	private static final TestSpanHandler SPANS = new TestSpanHandler();

	@Autowired
	private CountDownLatch messageBarrier;

	@Autowired
	private AtomicReference<Message<String>> messageHolder;

	@Autowired
	QueueChannel producerResultsChannel;

	@Test
	void observationIsPropagatedFromSupplierToConsumer() throws InterruptedException {
		assertThat(this.messageBarrier.await(30, TimeUnit.SECONDS)).isTrue();
		Message<String> message = this.messageHolder.get();
		assertThat(message.getHeaders()).containsKeys("traceparent");
		assertThat(message.getPayload()).isEqualTo("test data");

		await().until(() -> SPANS.spans().size() == 3);
		SpansAssert.assertThat(SPANS.spans().stream().map(BraveFinishedSpan::fromBrave).toList()).haveSameTraceId()
				.hasASpanWithName("kinesisSupplier-out-0 send",
						spanAssert -> spanAssert.hasKindEqualTo(Span.Kind.PRODUCER)
								.hasTag(IntegrationObservation.ProducerTags.COMPONENT_TYPE, "producer"))
				.hasASpanWithName(String.format("Consumer for [%s] receive", KINESIS_STREAM),
						spanAssert -> spanAssert.hasKindEqualTo(Span.Kind.CONSUMER)
								.hasTag(IntegrationObservation.HandlerTags.COMPONENT_TYPE, "message-producer"))
				.hasASpanWithName("kinesisConsumer process"); // Function part

		Message<?> producerResultMessage = producerResultsChannel.receive(10_000);
		assertThat(producerResultMessage.getPayload()).isEqualTo("test data".getBytes());
		assertThat(producerResultMessage.getHeaders()).containsKeys(KinesisHeaders.SHARD,
				KinesisHeaders.SEQUENCE_NUMBER);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class TestConfiguration {

		@Bean
		SpanHandler spanHandler() {
			return SPANS;
		}

		@Bean
		public LockRegistry<?> lockRegistry() {
			return new DefaultLockRegistry();
		}

		@Bean
		public ConcurrentMetadataStore checkpointStore() {
			return new SimpleMetadataStore();
		}

		@Bean
		public AtomicReference<Message<String>> messageHolder() {
			return new AtomicReference<>();
		}

		@Bean
		public CountDownLatch messageBarrier() {
			return new CountDownLatch(1);
		}

		@Bean
		public Supplier<Flux<Message<String>>> kinesisSupplier() {
			return () -> Flux.just("test data").map(GenericMessage::new);
		}

		@Bean
		QueueChannel producerResultsChannel() {
			return new QueueChannel();
		}

		@Bean
		@GlobalChannelInterceptor(patterns = "kinesisSupplier-out-0")
		ChannelInterceptor loggingChannelInterceptor() {
			return new ChannelInterceptor() {

				@Override
				public Message<?> preSend(Message<?> message, MessageChannel channel) {
					LOGGER.debug("Send message: " + message);
					return message;
				}

			};
		}

		@Bean
		public Consumer<Message<String>> kinesisConsumer(AtomicReference<Message<String>> messageHolder,
				CountDownLatch messageBarrier) {

			return message -> {
				LOGGER.debug("Received message: " + message);
				messageHolder.set(message);
				messageBarrier.countDown();
			};
		}
	}

}
