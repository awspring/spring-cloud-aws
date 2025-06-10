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

import io.awspring.cloud.sqs.MessageExecutionThreadFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.AsyncAcknowledgementResultCallback;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AsyncComponentAdapters}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings("unchecked")
class AsyncComponentAdaptersTests {

	@Test
	void shouldExecuteRunnableInNewThread() {
		TaskExecutor executor = mock(TaskExecutor.class);
		Runnable runnable = mock(Runnable.class);
		doAnswer(invocation -> {
			((Runnable) invocation.getArguments()[0]).run();
			return null;
		}).when(executor).execute(any());
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(executor);
		threadingComponentAdapter.execute(runnable).join();
		then(executor).should().execute(any());
	}

	@Test
	void shouldExecuteRunnableInSameThread() throws Exception {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageExecutionThreadFactory threadFactory = new MessageExecutionThreadFactory();
		executor.setThreadFactory(threadFactory);
		Runnable runnable = mock(Runnable.class);
		TaskExecutor adapterExecutor = mock(TaskExecutor.class);
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(adapterExecutor);
		executor.submit(() -> threadingComponentAdapter.execute(runnable).join()).get();
		then(runnable).should().run();
		then(adapterExecutor).should(never()).execute(any());
	}

	@Test
	void shouldExecuteSupplierInNewThread() {
		TaskExecutor executor = mock(TaskExecutor.class);
		Supplier<Object> supplier = mock(Supplier.class);
		doAnswer(invocation -> {
			((Runnable) invocation.getArguments()[0]).run();
			return null;
		}).when(executor).execute(any());
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(executor);
		threadingComponentAdapter.execute(supplier).join();
		then(supplier).should().get();
		then(executor).should().execute(any());
	}

	@Test
	void shouldExecuteSupplierInSameThread() throws Exception {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageExecutionThreadFactory threadFactory = new MessageExecutionThreadFactory();
		executor.setThreadFactory(threadFactory);
		Supplier<Object> supplier = mock(Supplier.class);
		TaskExecutor adapterExecutor = mock(TaskExecutor.class);
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(adapterExecutor);
		executor.submit(() -> threadingComponentAdapter.execute(supplier).join()).get();
		then(supplier).should().get();
		then(adapterExecutor).should(never()).execute(any());
	}

	@Test
	void shouldWrapThrowableFromRunnableInNewThread() {
		TaskExecutor executor = mock(TaskExecutor.class);
		Runnable runnable = mock(Runnable.class);
		RuntimeException expectedException = new RuntimeException(
				"Expected exception from shouldHandleThrowableFromRunnableInNewThread");
		doThrow(expectedException).when(runnable).run();
		doAnswer(invocation -> {
			((Runnable) invocation.getArguments()[0]).run();
			return null;
		}).when(executor).execute(any());
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(executor);
		assertThatThrownBy(() -> threadingComponentAdapter.execute(runnable).join())
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(AsyncAdapterBlockingExecutionFailedException.class).extracting(Throwable::getCause)
				.isEqualTo(expectedException);
		then(runnable).should().run();
		then(executor).should().execute(any());
	}

	@Test
	void shouldWrapThrowableFromRejectedRunnableInNewThread() {
		TaskExecutor executor = mock(TaskExecutor.class);
		Runnable runnable = mock(Runnable.class);
		TaskRejectedException expectedException = new TaskRejectedException(
				"Expected exception from shouldHandleThrowableFromRunnableInNewThread");
		doThrow(expectedException).when(executor).execute(any());
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(executor);
		assertThatThrownBy(() -> threadingComponentAdapter.execute(runnable).join())
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(AsyncAdapterBlockingExecutionFailedException.class).extracting(Throwable::getCause)
				.isEqualTo(expectedException);
		then(runnable).should(never()).run();
		then(executor).should().execute(any());
	}

	@Test
	void shouldWrapThrowableFromRunnableInSameThread() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageExecutionThreadFactory threadFactory = new MessageExecutionThreadFactory();
		executor.setThreadFactory(threadFactory);
		Runnable runnable = mock(Runnable.class);
		RuntimeException expectedException = new RuntimeException(
				"Expected exception from shouldHandleThrowableFromRunnableInSameThread");
		doThrow(expectedException).when(runnable).run();
		TaskExecutor adapterExecutor = mock(TaskExecutor.class);
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(adapterExecutor);
		assertThatThrownBy(() -> executor.submit(() -> threadingComponentAdapter.execute(runnable).join()).get())
				.isInstanceOf(ExecutionException.class).extracting(Throwable::getCause)
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(AsyncAdapterBlockingExecutionFailedException.class).extracting(Throwable::getCause)
				.isEqualTo(expectedException);

		then(runnable).should().run();
		then(adapterExecutor).should(never()).execute(any());
	}

	@Test
	void shouldWrapThrowableFromSupplierInNewThread() {
		TaskExecutor executor = mock(TaskExecutor.class);
		Supplier<Object> supplier = mock(Supplier.class);
		RuntimeException expectedException = new RuntimeException(
				"Expected exception from shouldHandleThrowableFromRunnableInNewThread");
		doThrow(expectedException).when(supplier).get();
		doAnswer(invocation -> {
			((Runnable) invocation.getArguments()[0]).run();
			return null;
		}).when(executor).execute(any());
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(executor);
		assertThatThrownBy(() -> threadingComponentAdapter.execute(supplier).join())
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(AsyncAdapterBlockingExecutionFailedException.class).extracting(Throwable::getCause)
				.isEqualTo(expectedException);
		then(supplier).should().get();
		then(executor).should().execute(any());
	}

	@Test
	void shouldWrapThrowableFromRejectedSupplierInNewThread() {
		TaskExecutor executor = mock(TaskExecutor.class);
		Supplier<Object> supplier = mock(Supplier.class);
		TaskRejectedException expectedException = new TaskRejectedException(
				"Expected exception from shouldHandleThrowableFromRunnableInNewThread");
		doThrow(expectedException).when(executor).execute(any());
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(executor);
		assertThatThrownBy(() -> threadingComponentAdapter.execute(supplier).join())
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(AsyncAdapterBlockingExecutionFailedException.class).extracting(Throwable::getCause)
				.isEqualTo(expectedException);
		then(supplier).should(never()).get();
		then(executor).should().execute(any());
	}

	@Test
	void shouldWrapThrowableFromSupplierInSameThread() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageExecutionThreadFactory threadFactory = new MessageExecutionThreadFactory();
		executor.setThreadFactory(threadFactory);
		Supplier<Object> supplier = mock(Supplier.class);
		RuntimeException expectedException = new RuntimeException(
				"Expected exception from shouldHandleThrowableFromRunnableInSameThread");
		doThrow(expectedException).when(supplier).get();
		TaskExecutor adapterExecutor = mock(TaskExecutor.class);
		AsyncComponentAdapters.AbstractThreadingComponentAdapter threadingComponentAdapter = new AsyncComponentAdapters.AbstractThreadingComponentAdapter() {
		};
		threadingComponentAdapter.setTaskExecutor(adapterExecutor);
		assertThatThrownBy(() -> executor.submit(() -> threadingComponentAdapter.execute(supplier).join()).get())
				.isInstanceOf(ExecutionException.class).extracting(Throwable::getCause)
				.isInstanceOf(CompletionException.class).extracting(Throwable::getCause)
				.isInstanceOf(AsyncAdapterBlockingExecutionFailedException.class).extracting(Throwable::getCause)
				.isEqualTo(expectedException);

		then(supplier).should().get();
		then(adapterExecutor).should(never()).execute(any());
	}

	@Test
	void shouldThrowIfTaskExecutorNotSetRunnable() {
		MessageListener<Object> listener = mock(MessageListener.class);
		Message<Object> message = mock(Message.class);
		AsyncMessageListener<Object> asyncListener = AsyncComponentAdapters.adapt(listener);
		assertThatThrownBy(() -> asyncListener.onMessage(message)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldThrowIfTaskExecutorNotSetSupplier() {
		MessageInterceptor<Object> interceptor = mock(MessageInterceptor.class);
		Message<Object> message = mock(Message.class);
		AsyncMessageInterceptor<Object> asyncInterceptor = AsyncComponentAdapters.adapt(interceptor);
		assertThatThrownBy(() -> asyncInterceptor.intercept(message)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldAdaptMessageListenerSingleMessage() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageListener<Object> listener = mock(MessageListener.class);
		Message<Object> message = mock(Message.class);
		MessageHeaders headers = mock(MessageHeaders.class);
		given(message.getHeaders()).willReturn(headers);
		AsyncMessageListener<Object> asyncListener = AsyncComponentAdapters.adapt(listener);
		((TaskExecutorAware) asyncListener).setTaskExecutor(executor);
		asyncListener.onMessage(message).join();
		then(listener).should().onMessage(message);
	}

	@Test
	void shouldAdaptMessageListenerBatch() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageListener<Object> listener = mock(MessageListener.class);
		Message<Object> message = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message2, message3);
		AsyncMessageListener<Object> asyncListener = AsyncComponentAdapters.adapt(listener);
		((TaskExecutorAware) asyncListener).setTaskExecutor(executor);
		asyncListener.onMessage(messages).join();
		ArgumentCaptor<Collection<Message<Object>>> messagesCaptor = ArgumentCaptor.forClass(Collection.class);
		then(listener).should().onMessage(messagesCaptor.capture());
		assertThat(messagesCaptor.getValue()).isEqualTo(messages);
	}

	@Test
	void shouldAdaptMessageInterceptorSingleMessage() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageInterceptor<Object> interceptor = mock(MessageInterceptor.class);
		Message<Object> message = mock(Message.class);
		MessageHeaders headers = mock(MessageHeaders.class);
		given(message.getHeaders()).willReturn(headers);
		AsyncMessageInterceptor<Object> asyncInterceptor = AsyncComponentAdapters.adapt(interceptor);
		((TaskExecutorAware) asyncInterceptor).setTaskExecutor(executor);
		asyncInterceptor.intercept(message).join();
		then(interceptor).should().intercept(message);
	}

	@Test
	void shouldAdaptMessageInterceptorBatch() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageInterceptor<Object> intercept = mock(MessageInterceptor.class);
		Message<Object> message = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message2, message3);
		AsyncMessageInterceptor<Object> asyncInterceptor = AsyncComponentAdapters.adapt(intercept);
		((TaskExecutorAware) asyncInterceptor).setTaskExecutor(executor);
		asyncInterceptor.intercept(messages).join();
		ArgumentCaptor<Collection<Message<Object>>> messagesCaptor = ArgumentCaptor.forClass(Collection.class);
		then(intercept).should().intercept(messagesCaptor.capture());
		assertThat(messagesCaptor.getValue()).isEqualTo(messages);
	}

	@Test
	void shouldAdaptErrorHandlerSingleMessage() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		ErrorHandler<Object> errorHandler = mock(ErrorHandler.class);
		Message<Object> message = mock(Message.class);
		MessageHeaders headers = mock(MessageHeaders.class);
		given(message.getHeaders()).willReturn(headers);
		AsyncErrorHandler<Object> asyncErrorHandler = AsyncComponentAdapters.adapt(errorHandler);
		((TaskExecutorAware) asyncErrorHandler).setTaskExecutor(executor);
		Throwable t = mock(Throwable.class);
		asyncErrorHandler.handle(message, t).join();
		then(errorHandler).should().handle(message, t);
	}

	@Test
	void shouldAdaptErrorHandlerBatch() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		ErrorHandler<Object> errorHandler = mock(ErrorHandler.class);
		Message<Object> message = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message2, message3);
		AsyncErrorHandler<Object> asyncErrorHandler = AsyncComponentAdapters.adapt(errorHandler);
		((TaskExecutorAware) asyncErrorHandler).setTaskExecutor(executor);
		Throwable throwable = mock(Throwable.class);
		asyncErrorHandler.handle(messages, throwable).join();
		ArgumentCaptor<Collection<Message<Object>>> messagesCaptor = ArgumentCaptor.forClass(Collection.class);
		then(errorHandler).should().handle(messagesCaptor.capture(), eq(throwable));
		assertThat(messagesCaptor.getValue()).isEqualTo(messages);
	}

	@Test
	void shouldAdaptAcknowledgementCallbackOnSuccess() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		AcknowledgementResultCallback<Object> callback = mock(AcknowledgementResultCallback.class);
		Message<Object> message = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message2, message3);
		AsyncAcknowledgementResultCallback<Object> asyncCallback = AsyncComponentAdapters.adapt(callback);
		((TaskExecutorAware) asyncCallback).setTaskExecutor(executor);
		asyncCallback.onSuccess(messages).join();
		then(callback).should().onSuccess(messages);
	}

	@Test
	void shouldAdaptAcknowledgementCallbackOnFailure() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		AcknowledgementResultCallback<Object> callback = mock(AcknowledgementResultCallback.class);
		Message<Object> message = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message2, message3);
		AsyncAcknowledgementResultCallback<Object> asyncCallback = AsyncComponentAdapters.adapt(callback);
		((TaskExecutorAware) asyncCallback).setTaskExecutor(executor);
		Throwable throwable = mock(Throwable.class);
		asyncCallback.onFailure(messages, throwable).join();
		then(callback).should().onFailure(messages, throwable);
	}

	@Test
	void shouldAdaptAcknowledgementCallbackBatch() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		ErrorHandler<Object> errorHandler = mock(ErrorHandler.class);
		Message<Object> message = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> messages = Arrays.asList(message, message2, message3);
		AsyncErrorHandler<Object> asyncErrorHandler = AsyncComponentAdapters.adapt(errorHandler);
		((TaskExecutorAware) asyncErrorHandler).setTaskExecutor(executor);
		Throwable throwable = mock(Throwable.class);
		asyncErrorHandler.handle(messages, throwable).join();
		ArgumentCaptor<Collection<Message<Object>>> messagesCaptor = ArgumentCaptor.forClass(Collection.class);
		then(errorHandler).should().handle(messagesCaptor.capture(), eq(throwable));
		assertThat(messagesCaptor.getValue()).isEqualTo(messages);
	}
	
	@Test
	void shouldPropagateObservationContext() {

		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		final Object[] capturedThreadLocalContext = new Object[1];

		ObservationThreadLocalAccessor accessor = new ObservationThreadLocalAccessor();

		// Create a listener that will verify the thread local during execution
		MessageListener<String> listener = new MessageListener<String>() {
			@Override
			public void onMessage(Message<String> message) {
				// Capture the observation from thread local
				capturedThreadLocalContext[0] = accessor.getValue();
			}
		};

		ObservationRegistry observationRegistry = TestObservationRegistry.create();
		Observation observation = Observation.start("test-observation", observationRegistry);
		
		Map<String, Object> headers = new HashMap<>();
		headers.put(ObservationThreadLocalAccessor.KEY, observation);
		Message<String> message = MessageBuilder.createMessage("payload", new MessageHeaders(headers));
		
		MessageListener<String> spyListener = spy(listener);
		
		AsyncMessageListener<String> asyncListener = AsyncComponentAdapters.adapt(spyListener);
		((TaskExecutorAware) asyncListener).setTaskExecutor(executor);
		
		try {
			asyncListener.onMessage(message).join();
			
			ArgumentCaptor<Message<String>> messageCaptor = ArgumentCaptor.forClass(Message.class);
			then(spyListener).should().onMessage(messageCaptor.capture());
			Message<String> passedMessage = messageCaptor.getValue();
			assertThat(passedMessage.getHeaders().containsKey(ObservationThreadLocalAccessor.KEY)).isFalse();
			
			assertThat(capturedThreadLocalContext[0]).isEqualTo(observation);
		}
		finally {
			observation.stop();
		}
	}
	
	@Test
	void shouldHandleMessageWithoutObservationContext() {

		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		final boolean[] executionSucceeded = new boolean[1];
		
		MessageListener<String> listener = new MessageListener<String>() {
			@Override
			public void onMessage(Message<String> message) {
				// Set flag indicating successful execution
				executionSucceeded[0] = true;
			}
		};
		
		// Create message without observation context in headers
		Message<String> message = MessageBuilder.createMessage("payload", new MessageHeaders(new HashMap<>()));
		
		AsyncMessageListener<String> asyncListener = AsyncComponentAdapters.adapt(listener);
		((TaskExecutorAware) asyncListener).setTaskExecutor(executor);
		
		// Execute
		asyncListener.onMessage(message).join();
		
		// Verify the listener was executed successfully without errors
		assertThat(executionSucceeded[0]).isTrue();
	}
	
	@Test
	void shouldRestoreObservationContextAfterExecution() {

		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageInterceptor<String> interceptor = mock(MessageInterceptor.class);
		
		Observation observationContext = mock(Observation.class);
		
		Map<String, Object> headers = new HashMap<>();
		headers.put(ObservationThreadLocalAccessor.KEY, observationContext);
		Message<String> message = MessageBuilder.createMessage("payload", new MessageHeaders(headers));
		
		Message<String> modifiedMessage = MessageBuilder.createMessage("modified", new MessageHeaders(new HashMap<>()));
		when(interceptor.intercept(any(Message.class))).thenReturn(modifiedMessage);
		
		AsyncMessageInterceptor<String> asyncInterceptor = AsyncComponentAdapters.adapt(interceptor);
		((TaskExecutorAware) asyncInterceptor).setTaskExecutor(executor);
		
		Message<String> result = asyncInterceptor.intercept(message).join();
		
		assertThat(result.getHeaders().get(ObservationThreadLocalAccessor.KEY)).isEqualTo(observationContext);
	}
	
	@Test
	void shouldHandleErrorsWithObservationContext() {

		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		MessageListener<String> listener = mock(MessageListener.class);
		
		Observation observationContext = mock(Observation.class);
		
		Map<String, Object> headers = new HashMap<>();
		headers.put(ObservationThreadLocalAccessor.KEY, observationContext);
		Message<String> message = MessageBuilder.createMessage("payload", new MessageHeaders(headers));
		
		ListenerExecutionFailedException exception = new ListenerExecutionFailedException("Test exception", new RuntimeException(), message);
		doThrow(exception).when(listener).onMessage(any(Message.class));
		
		AsyncMessageListener<String> asyncListener = AsyncComponentAdapters.adapt(listener);
		((TaskExecutorAware) asyncListener).setTaskExecutor(executor);
		
		CompletableFuture<Void> future = asyncListener.onMessage(message);
		
		assertThatThrownBy(future::join)
			.isInstanceOf(CompletionException.class)
			.extracting(Throwable::getCause)
			.isInstanceOf(AsyncAdapterBlockingExecutionFailedException.class)
			.extracting(Throwable::getCause)
			.isInstanceOf(ListenerExecutionFailedException.class)
			.satisfies(ex -> {
				ListenerExecutionFailedException lefe = (ListenerExecutionFailedException) ex;
				Message<?> errorMessage = lefe.getFailedMessage();
				assertThat(errorMessage.getHeaders().get(ObservationThreadLocalAccessor.KEY)).isEqualTo(observationContext);
			});
	}
}
