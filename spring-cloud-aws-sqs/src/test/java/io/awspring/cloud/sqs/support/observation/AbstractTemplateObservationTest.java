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
package io.awspring.cloud.sqs.support.observation;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Tests for {@link AbstractTemplateObservation}.
 *
 * @author Tomaz Fernandes
 */
class AbstractTemplateObservationTest {

	@Test
	void conventionShouldReturnExpectedValues() {
		// given
		TestConvention convention = new TestConvention();
		String destinationName = "test-destination";

		Message<String> message = MessageBuilder.createMessage("testPayload", new MessageHeaders(Map.of()));
		AbstractTemplateObservation.Context context = new AbstractTemplateObservation.Context(message,
				destinationName) {
		};

		// when
		KeyValues lowCardinalityKeyValues = convention.getLowCardinalityKeyValues(context);
		KeyValues highCardinalityKeyValues = convention.getHighCardinalityKeyValues(context);
		String contextualName = convention.getContextualName(context);
		String name = convention.getName();

		// then
		assertThat(lowCardinalityKeyValues.stream())
				.anyMatch(kv -> kv.getKey().equals("messaging.system") && kv.getValue().equals("test"))
				.anyMatch(kv -> kv.getKey().equals("messaging.operation") && kv.getValue().equals("publish"))
				.anyMatch(
						kv -> kv.getKey().equals("messaging.destination.name") && kv.getValue().equals(destinationName))
				.anyMatch(kv -> kv.getKey().equals("messaging.destination.kind") && kv.getValue().equals("queue"));

		assertThat(highCardinalityKeyValues).isEmpty();

		assertThat(contextualName).isEqualTo(destinationName + " send");
		assertThat(name).isEqualTo("spring.aws.test.template");
	}

	@Test
	void specificsShouldProvideExpectedValues() {
		// given
		TestSpecifics specifics = new TestSpecifics();
		String destinationName = "test-destination";
		Message<String> message = MessageBuilder.withPayload("test-payload").build();

		// when
		AbstractTemplateObservation.Context context = specifics.createContext(message, destinationName);
		ObservationConvention<AbstractTemplateObservation.Context> convention = specifics.getDefaultConvention();
		AbstractTemplateObservation.Documentation documentationContext = specifics.getDocumentation();

		// then
		assertThat(context.getDestinationName()).isEqualTo(destinationName);
		assertThat(convention).isInstanceOf(TestConvention.class);
		assertThat(documentationContext).isInstanceOf(TestDocumentation.class);
	}

	@Test
	void shouldWhitelistAndBlockKeys() {
		// given
		String destinationName = "test-destination";
		Message<String> message = MessageBuilder.withPayload("test-payload").build();
		AbstractTemplateObservation.Context context = new AbstractTemplateObservation.Context(message, destinationName) {};

		// when - test allowed keys
		context.getSetter().set(context.getCarrier(), "baggage", "baggage-value");
		context.getSetter().set(context.getCarrier(), "traceparent", "traceparent-value");
		context.getSetter().set(context.getCarrier(), "tracestate", "tracestate-value");
		context.getSetter().set(context.getCarrier(), "b3", "b3-value");

		// when - test blocked keys
		context.getSetter().set(context.getCarrier(), "blocked-key", "blocked-value");

		// then
		Map<String, Object> carrier = context.getCarrier();
		assertThat(carrier)
			.containsEntry("baggage", "baggage-value")
			.containsEntry("traceparent", "traceparent-value")
			.containsEntry("tracestate", "tracestate-value")
			.containsEntry("b3", "b3-value")
			.doesNotContainKey("blocked-key");
	}

	private static class TestConvention
			extends AbstractTemplateObservation.Convention<AbstractTemplateObservation.Context> {

		@Override
		protected String getMessagingSystem() {
			return "test";
		}

		@Override
		public boolean supportsContext(Observation.Context context) {
			return true;
		}

		@Override
		public String getName() {
			return "spring.aws.test.template";
		}

		@Override
		protected String getSourceKind() {
			return "queue";
		}
	}

	private static class TestDocumentation extends AbstractTemplateObservation.Documentation {

		@Override
		protected Class<? extends ObservationConvention<? extends Observation.Context>> getSpecificDefaultConvention() {
			return TestConvention.class;
		}

		@Override
		public String getName() {
			return "spring.aws.test.template";
		}
	}

	private static class TestSpecifics
			implements AbstractTemplateObservation.Specifics<AbstractTemplateObservation.Context> {
		private static final TestDocumentation DOCUMENTATION = new TestDocumentation();
		private static final TestConvention CONVENTION = new TestConvention();

		@Override
		public AbstractTemplateObservation.Documentation getDocumentation() {
			return DOCUMENTATION;
		}

		@Override
		public AbstractTemplateObservation.Context createContext(Message<?> message, String destinationName) {
			return new AbstractTemplateObservation.Context(message, destinationName) {
			};
		}

		@Override
		public AbstractTemplateObservation.Convention<AbstractTemplateObservation.Context> getDefaultConvention() {
			return CONVENTION;
		}
	}
}
