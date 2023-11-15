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
import static org.mockito.Mockito.mock;

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
 * Tests for {@link AbstractMessageListenerContainer}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class AbstractMessageListenerContainerTests {

	@Test
	void shouldAdaptBlockingComponents() {
		SqsContainerOptions options = SqsContainerOptions.builder().build();
		AbstractMessageListenerContainer<Object, SqsContainerOptions, SqsContainerOptionsBuilder> container = new AbstractMessageListenerContainer<>(
				options) {
		};

		MessageListener<Object> listener = mock(MessageListener.class);
		ErrorHandler<Object> errorHandler = mock(ErrorHandler.class);
		MessageInterceptor<Object> interceptor = mock(MessageInterceptor.class);
		AcknowledgementResultCallback<Object> callback = mock(AcknowledgementResultCallback.class);
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = mock(ContainerComponentFactory.class);
		List<ContainerComponentFactory<Object, SqsContainerOptions>> componentFactories = Collections
				.singletonList(componentFactory);

		container.setMessageListener(listener);
		container.setErrorHandler(errorHandler);
		container.setComponentFactories(componentFactories);
		container.addMessageInterceptor(interceptor);
		container.setAcknowledgementResultCallback(callback);

		assertThat(container.getMessageListener())
				.isInstanceOf(AsyncComponentAdapters.AbstractThreadingComponentAdapter.class)
				.extracting("blockingMessageListener").isEqualTo(listener);

		assertThat(container.getErrorHandler())
				.isInstanceOf(AsyncComponentAdapters.AbstractThreadingComponentAdapter.class)
				.extracting("blockingErrorHandler").isEqualTo(errorHandler);

		assertThat(container.getAcknowledgementResultCallback())
				.isInstanceOf(AsyncComponentAdapters.AbstractThreadingComponentAdapter.class)
				.extracting("blockingAcknowledgementResultCallback").isEqualTo(callback);

		assertThat(container.getMessageInterceptors()).element(0)
				.isInstanceOf(AsyncComponentAdapters.AbstractThreadingComponentAdapter.class)
				.extracting("blockingMessageInterceptor").isEqualTo(interceptor);

		assertThat(container.getPhase()).isEqualTo(MessageListenerContainer.DEFAULT_PHASE);
	}

	@Test
	void shouldSetAsyncComponents() {
		SqsContainerOptions options = SqsContainerOptions.builder().build();
		AbstractMessageListenerContainer<Object, SqsContainerOptions, SqsContainerOptionsBuilder> container = new AbstractMessageListenerContainer<>(
				options) {
		};

		AsyncMessageListener<Object> listener = mock(AsyncMessageListener.class);
		AsyncErrorHandler<Object> errorHandler = mock(AsyncErrorHandler.class);
		AsyncMessageInterceptor<Object> interceptor = mock(AsyncMessageInterceptor.class);
		AsyncAcknowledgementResultCallback<Object> callback = mock(AsyncAcknowledgementResultCallback.class);
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = mock(ContainerComponentFactory.class);
		List<ContainerComponentFactory<Object, SqsContainerOptions>> componentFactories = Collections
				.singletonList(componentFactory);

		container.setAsyncMessageListener(listener);
		container.setErrorHandler(errorHandler);
		container.setComponentFactories(componentFactories);
		container.addMessageInterceptor(interceptor);
		container.setAcknowledgementResultCallback(callback);

		assertThat(container.getMessageListener()).isEqualTo(listener);
		assertThat(container.getErrorHandler()).isEqualTo(errorHandler);
		assertThat(container.getAcknowledgementResultCallback()).isEqualTo(callback);
		assertThat(container.getContainerComponentFactories()).containsExactlyElementsOf(componentFactories);
		assertThat(container.getMessageInterceptors()).containsExactly(interceptor);
		assertThat(container.getPhase()).isEqualTo(MessageListenerContainer.DEFAULT_PHASE);

	}

}
