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
import static org.mockito.BDDMockito.then;
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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class AbstractMessageListenerContainerFactoryTests {

	@Test
	void shouldSetBlockingComponents() {
		SqsMessageListenerContainer<Object> container = mock(SqsMessageListenerContainer.class);

		AbstractMessageListenerContainerFactory<Object, SqsMessageListenerContainer<Object>, SqsContainerOptions, SqsContainerOptions.Builder> factory = new AbstractMessageListenerContainerFactory<>(
				SqsContainerOptions.builder().build()) {

			@Override
			protected void configureContainerOptions(Endpoint endpoint, SqsContainerOptions.Builder containerOptions) {

			}

			@Override
			protected SqsMessageListenerContainer<Object> createContainerInstance(Endpoint endpoint,
					SqsContainerOptions containerOptions) {
				return container;
			}
		};
		MessageListener<Object> listener = mock(MessageListener.class);
		ErrorHandler<Object> errorHandler = mock(ErrorHandler.class);
		MessageInterceptor<Object> interceptor = mock(MessageInterceptor.class);
		AcknowledgementResultCallback<Object> callback = mock(AcknowledgementResultCallback.class);
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = mock(ContainerComponentFactory.class);
		List<ContainerComponentFactory<Object, SqsContainerOptions>> componentFactories = Collections
				.singletonList(componentFactory);

		factory.setMessageListener(listener);
		factory.setErrorHandler(errorHandler);
		factory.setContainerComponentFactories(componentFactories);
		factory.addMessageInterceptor(interceptor);
		factory.setAcknowledgementResultCallback(callback);

		SqsMessageListenerContainer<Object> createdContainer = factory.createContainer("test-queue");
		assertThat(createdContainer).isEqualTo(container);
		then(container).should().setMessageListener(listener);
		then(container).should().setErrorHandler(errorHandler);
		then(container).should().setComponentFactories(componentFactories);
		then(container).should().setAcknowledgementResultCallback(callback);
		then(container).should().addMessageInterceptor(interceptor);

	}

	@Test
	void shouldSetAsyncComponents() {
		SqsMessageListenerContainer<Object> container = mock(SqsMessageListenerContainer.class);
		AbstractMessageListenerContainerFactory<Object, SqsMessageListenerContainer<Object>, SqsContainerOptions, SqsContainerOptions.Builder> factory = new AbstractMessageListenerContainerFactory<>(
				SqsContainerOptions.builder().build()) {
			@Override
			protected void configureContainerOptions(Endpoint endpoint, SqsContainerOptions.Builder containerOptions) {

			}

			@Override
			protected SqsMessageListenerContainer<Object> createContainerInstance(Endpoint endpoint,
					SqsContainerOptions containerOptions) {
				return container;
			}
		};
		AsyncMessageListener<Object> listener = mock(AsyncMessageListener.class);
		AsyncErrorHandler<Object> errorHandler = mock(AsyncErrorHandler.class);
		AsyncMessageInterceptor<Object> interceptor = mock(AsyncMessageInterceptor.class);
		AsyncAcknowledgementResultCallback<Object> callback = mock(AsyncAcknowledgementResultCallback.class);
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = mock(ContainerComponentFactory.class);
		List<ContainerComponentFactory<Object, SqsContainerOptions>> componentFactories = Collections
				.singletonList(componentFactory);

		factory.setAsyncMessageListener(listener);
		factory.setErrorHandler(errorHandler);
		factory.setContainerComponentFactories(componentFactories);
		factory.addMessageInterceptor(interceptor);
		factory.setAcknowledgementResultCallback(callback);

		SqsMessageListenerContainer<Object> createdContainer = factory.createContainer("test-queue");
		assertThat(createdContainer).isEqualTo(container);
		then(container).should().setAsyncMessageListener(listener);
		then(container).should().setErrorHandler(errorHandler);
		then(container).should().setComponentFactories(componentFactories);
		then(container).should().setAcknowledgementResultCallback(callback);
		then(container).should().addMessageInterceptor(interceptor);

	}

}
