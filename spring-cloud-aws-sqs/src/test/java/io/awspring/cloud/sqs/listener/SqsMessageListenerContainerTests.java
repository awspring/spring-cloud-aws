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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class SqsMessageListenerContainerTests {

	@Test
	void shouldCreateFromBuilderWithBlockingComponents() {
		SqsAsyncClient client = mock(SqsAsyncClient.class);
		MessageListener<Object> listener = mock(MessageListener.class);
		ErrorHandler<Object> errorHandler = mock(ErrorHandler.class);
		MessageInterceptor<Object> interceptor1 = mock(MessageInterceptor.class);
		MessageInterceptor<Object> interceptor2 = mock(MessageInterceptor.class);
		AcknowledgementResultCallback<Object> callback = mock(AcknowledgementResultCallback.class);
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = mock(ContainerComponentFactory.class);
		List<ContainerComponentFactory<Object, SqsContainerOptions>> componentFactories = Collections
				.singletonList(componentFactory);
		List<String> queueNames = Arrays.asList("test-queue-name-1", "test-queue-name-2");

		SqsMessageListenerContainer<Object> container = SqsMessageListenerContainer.builder().messageListener(listener)
				.sqsAsyncClient(client).errorHandler(errorHandler).componentFactories(componentFactories)
				.acknowledgementResultCallback(callback).messageInterceptor(interceptor1)
				.messageInterceptor(interceptor2).queueNames(queueNames).build();

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
				.extracting("blockingMessageInterceptor").isEqualTo(interceptor1);

		assertThat(container.getMessageInterceptors()).element(1)
				.isInstanceOf(AsyncComponentAdapters.AbstractThreadingComponentAdapter.class)
				.extracting("blockingMessageInterceptor").isEqualTo(interceptor2);

		assertThat(container).extracting("sqsAsyncClient").isEqualTo(client);

		assertThat(container.getQueueNames()).containsExactlyElementsOf(queueNames);
	}

	@Test
	void shouldCreateFromBuilderWithAsyncComponents() {
		String queueName = "test-queue-name";
		String id = "test-id";
		SqsAsyncClient client = mock(SqsAsyncClient.class);
		AsyncMessageListener<Object> listener = mock(AsyncMessageListener.class);
		AsyncErrorHandler<Object> errorHandler = mock(AsyncErrorHandler.class);
		AsyncMessageInterceptor<Object> interceptor1 = mock(AsyncMessageInterceptor.class);
		AsyncMessageInterceptor<Object> interceptor2 = mock(AsyncMessageInterceptor.class);
		AsyncAcknowledgementResultCallback<Object> callback = mock(AsyncAcknowledgementResultCallback.class);
		ContainerComponentFactory<Object, SqsContainerOptions> componentFactory = mock(ContainerComponentFactory.class);
		List<ContainerComponentFactory<Object, SqsContainerOptions>> componentFactories = Collections
				.singletonList(componentFactory);
		SqsMessageListenerContainer<Object> container = SqsMessageListenerContainer.builder()
				.asyncMessageListener(listener).sqsAsyncClient(client).errorHandler(errorHandler)
				.componentFactories(componentFactories).acknowledgementResultCallback(callback)
				.messageInterceptor(interceptor1).messageInterceptor(interceptor2).queueNames(queueName).id(id).build();

		assertThat(container.getMessageListener()).isEqualTo(listener);
		assertThat(container.getErrorHandler()).isEqualTo(errorHandler);
		assertThat(container.getAcknowledgementResultCallback()).isEqualTo(callback);
		assertThat(container.getMessageInterceptors()).containsExactly(interceptor1, interceptor2);
	}

}
