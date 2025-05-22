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
package io.awspring.cloud.sqs.listener.sink.adapter;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.ObservableComponent;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import io.awspring.cloud.sqs.support.observation.AbstractListenerObservation;
import io.awspring.cloud.sqs.support.observation.SqsListenerObservation;
import io.micrometer.observation.tck.TestObservationRegistry;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;

/**
 * Tests for observability support in {@link AbstractDelegatingMessageListeningSinkAdapter}.
 *
 * @author Tomaz Fernandes
 */
class AbstractDelegatingMessageListeningSinkAdapterObservationTest {

	private TestDelegatingMessageListeningSinkAdapter adapter;
	private ObservableComponent observableDelegate;
	private TestObservationRegistry observationRegistry;
	private AbstractListenerObservation.Specifics<?> observationSpecifics;

	@BeforeEach
	void setUp() {
		observationRegistry = TestObservationRegistry.create();
		observableDelegate = mock(ObservableComponent.class, Mockito.withSettings().extraInterfaces(MessageSink.class));
		observationSpecifics = new SqsListenerObservation.SqsSpecifics();

		adapter = new TestDelegatingMessageListeningSinkAdapter((MessageSink<String>) observableDelegate);
	}

	@Test
	void shouldDelegateObservationSpecifics() {
		// given the adapter has been set up

		// when
		adapter.setObservationSpecifics(observationSpecifics);

		// then
		then(observableDelegate).should().setObservationSpecifics(observationSpecifics);
	}

	@Test
	void shouldDelegateContainerOptionsWithObservationRegistry() {
		// given
		SqsContainerOptions options = SqsContainerOptions.builder().observationRegistry(observationRegistry).build();

		// when
		adapter.configure(options);

		// then
		then((MessageSink<String>) observableDelegate).should().configure(options);
	}

	/**
	 * Test implementation of AbstractDelegatingMessageListeningSinkAdapter
	 */
	private static class TestDelegatingMessageListeningSinkAdapter
			extends AbstractDelegatingMessageListeningSinkAdapter<String> {
		protected TestDelegatingMessageListeningSinkAdapter(MessageSink<String> delegate) {
			super(delegate);
		}

		@Override
		public CompletableFuture<Void> emit(Collection<Message<String>> messages,
				MessageProcessingContext<String> context) {
			return CompletableFuture.completedFuture(null);
		}
	}
}
