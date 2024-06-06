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
package io.awspring.cloud.sqs.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.ContainerComponentFactory;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.SqsContainerOptions;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AsyncAcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Tests for {@link SqsMessageListenerContainerFactory}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class SqsMessageListenerContainerFactoryTests {

	@Test
	void shouldCreateContainerFromEndpointWithOptionsDefaults() {
		List<String> queueNames = Collections.singletonList("test-queue");
		String id = "test-id";
		SqsAsyncClient client = mock(SqsAsyncClient.class);
		SqsEndpoint endpoint = mock(SqsEndpoint.class);
		given(endpoint.getMaxConcurrentMessages()).willReturn(null);
		given(endpoint.getMessageVisibility()).willReturn(null);
		given(endpoint.getMaxMessagesPerPoll()).willReturn(null);
		given(endpoint.getPollTimeout()).willReturn(null);
		given(endpoint.getLogicalNames()).willReturn(queueNames);
		given(endpoint.getId()).willReturn(id);

		SqsMessageListenerContainerFactory<Object> factory = new SqsMessageListenerContainerFactory<>();
		factory.setSqsAsyncClient(client);
		SqsMessageListenerContainer<Object> container = factory.createContainer(endpoint);

		assertThat(container.getContainerOptions()).isInstanceOfSatisfying(SqsContainerOptions.class, options -> {
			assertThat(options.getMaxConcurrentMessages()).isEqualTo(10);
			assertThat(options.getMessageVisibility()).isNull();
			assertThat(options.getPollTimeout()).isEqualTo(Duration.ofSeconds(10));
			assertThat(options.getMaxMessagesPerPoll()).isEqualTo(10);
		});

		assertThat(container.getId()).isEqualTo(id);
		assertThat(container.getQueueNames()).containsExactlyElementsOf(queueNames);
		assertThat(container.getObservationRegistry()).isEqualTo(ObservationRegistry.NOOP);

	}

	@Test
	void shouldCreateContainerFromEndpointOverridingOptions() {
		List<String> queueNames = Collections.singletonList("test-queue");
		String id = "test-id";
		SqsAsyncClient client = mock(SqsAsyncClient.class);
		SqsEndpoint endpoint = mock(SqsEndpoint.class);
		TestObservationRegistry observationRegistry = TestObservationRegistry.create();
		int inflight = 9;
		int messagesPerPoll = 7;
		Duration pollTimeout = Duration.ofSeconds(6);
		Duration visibility = Duration.ofSeconds(8);
		given(endpoint.getMaxConcurrentMessages()).willReturn(inflight);
		given(endpoint.getMessageVisibility()).willReturn(visibility);
		given(endpoint.getMaxMessagesPerPoll()).willReturn(messagesPerPoll);
		given(endpoint.getPollTimeout()).willReturn(pollTimeout);
		given(endpoint.getLogicalNames()).willReturn(queueNames);
		given(endpoint.getId()).willReturn(id);

		SqsMessageListenerContainerFactory<Object> factory = new SqsMessageListenerContainerFactory<>();
		factory.setSqsAsyncClient(client);
		factory.setObservationRegistry(observationRegistry);
		SqsMessageListenerContainer<Object> container = factory.createContainer(endpoint);

		assertThat(container.getContainerOptions()).isInstanceOfSatisfying(SqsContainerOptions.class, options -> {
			assertThat(options.getMaxConcurrentMessages()).isEqualTo(inflight);
			assertThat(options.getMessageVisibility()).isEqualTo(visibility);
			assertThat(options.getPollTimeout()).isEqualTo(pollTimeout);
			assertThat(options.getMaxMessagesPerPoll()).isEqualTo(messagesPerPoll);
		});

		assertThat(container.getId()).isEqualTo(id);
		assertThat(container.getQueueNames()).containsExactlyElementsOf(queueNames);
		assertThat(container.getObservationRegistry()).isEqualTo(observationRegistry);

	}

	@Test
	void shouldCreateFromBuilderWithBlockingComponents() {
		SqsAsyncClient client = mock(SqsAsyncClient.class);
		MessageListener<Object> listener = mock(MessageListener.class);
		ErrorHandler<Object> errorHandler = mock(ErrorHandler.class);
		MessageInterceptor<Object> interceptor1 = mock(MessageInterceptor.class);
		MessageInterceptor<Object> interceptor2 = mock(MessageInterceptor.class);
		AcknowledgementResultCallback<Object> callback = mock(AcknowledgementResultCallback.class);
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = mock(ContainerComponentFactory.class);
		TestObservationRegistry observationRegistry = TestObservationRegistry.create();
		List<ContainerComponentFactory<Object, SqsContainerOptions>> componentFactories = Collections
				.singletonList(componentFactory);

		SqsMessageListenerContainerFactory<Object> factory = SqsMessageListenerContainerFactory.builder()
				.messageListener(listener).sqsAsyncClient(client).errorHandler(errorHandler)
				.containerComponentFactories(componentFactories).acknowledgementResultCallback(callback)
				.messageInterceptor(interceptor1).messageInterceptor(interceptor2)
				.observationRegistry(observationRegistry).build();

		assertThat(factory).extracting("messageListener").isEqualTo(listener);
		assertThat(factory).extracting("errorHandler").isEqualTo(errorHandler);
		assertThat(factory).extracting("acknowledgementResultCallback").isEqualTo(callback);
		assertThat(factory).extracting("messageInterceptors").asInstanceOf(collection(MessageInterceptor.class))
				.containsExactly(interceptor1, interceptor2);
		assertThat(factory).extracting("containerComponentFactories").isEqualTo(componentFactories);
		assertThat(factory).extracting("sqsAsyncClientSupplier").asInstanceOf(type(Supplier.class))
				.extracting(Supplier::get).isEqualTo(client);
		assertThat(factory).extracting("observationRegistry").isEqualTo(observationRegistry);
	}

	@Test
	void shouldCreateFromBuilderWithAsyncComponents() {
		SqsAsyncClient client = mock(SqsAsyncClient.class);
		AsyncMessageListener<Object> listener = mock(AsyncMessageListener.class);
		AsyncErrorHandler<Object> errorHandler = mock(AsyncErrorHandler.class);
		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncAcknowledgementResultCallback<Object> callback = mock(AsyncAcknowledgementResultCallback.class);
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = mock(ContainerComponentFactory.class);
		TestObservationRegistry observationRegistry = TestObservationRegistry.create();
		List<ContainerComponentFactory<Object, SqsContainerOptions>> componentFactories = Collections
				.singletonList(componentFactory);

		SqsMessageListenerContainerFactory<Object> container = SqsMessageListenerContainerFactory.builder()
				.asyncMessageListener(listener).sqsAsyncClient(client).errorHandler(errorHandler)
				.containerComponentFactories(componentFactories).acknowledgementResultCallback(callback)
				.messageInterceptor(interceptor1).messageInterceptor(interceptor2)
				.observationRegistry(observationRegistry).build();

		assertThat(container).extracting("asyncMessageListener").isEqualTo(listener);
		assertThat(container).extracting("asyncErrorHandler").isEqualTo(errorHandler);
		assertThat(container).extracting("asyncAcknowledgementResultCallback").isEqualTo(callback);
		assertThat(container).extracting("asyncMessageInterceptors")
				.asInstanceOf(collection(AsyncMessageInterceptor.class)).containsExactly(interceptor1, interceptor2);
		assertThat(container).extracting("observationRegistry").isEqualTo(observationRegistry);
	}

}
