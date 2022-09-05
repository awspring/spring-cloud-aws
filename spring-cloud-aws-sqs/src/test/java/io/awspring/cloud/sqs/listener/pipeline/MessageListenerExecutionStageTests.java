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
package io.awspring.cloud.sqs.listener.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementHandler;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class MessageListenerExecutionStageTests {

	@Test
	void shouldForwardMessageOnSuccess() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message = mock(Message.class);
		AsyncMessageListener<Object> messageListener = mock(AsyncMessageListener.class);

		given(message.getHeaders()).willReturn(headers);
		given(messageListener.onMessage(message)).willReturn(CompletableFuture.completedFuture(null));

		MessageProcessingConfiguration<Object> configuration = MessageProcessingConfiguration.builder()
				.interceptors(Collections.emptyList()).ackHandler(mock(AcknowledgementHandler.class))
				.errorHandler(mock(AsyncErrorHandler.class)).messageListener(messageListener).build();
		MessageProcessingContext<Object> context = MessageProcessingContext.create();
		MessageProcessingPipeline<Object> stage = new MessageListenerExecutionStage<>(configuration);
		CompletableFuture<Message<Object>> result = stage.process(message, context);
		assertThat(result).isCompletedWithValue(message);
		verify(messageListener).onMessage(message);

	}

	@Test
	void shouldForwardMessageOnSuccessBatch() {
		MessageHeaders headers = new MessageHeaders(null);
		Message<Object> message1 = mock(Message.class);
		Message<Object> message2 = mock(Message.class);
		Message<Object> message3 = mock(Message.class);
		List<Message<Object>> batch = Arrays.asList(message1, message2, message3);

		AsyncMessageListener<Object> messageListener = mock(AsyncMessageListener.class);
		given(message1.getHeaders()).willReturn(headers);
		given(message2.getHeaders()).willReturn(headers);
		given(message3.getHeaders()).willReturn(headers);

		given(messageListener.onMessage(batch)).willReturn(CompletableFuture.completedFuture(null));

		MessageProcessingConfiguration<Object> configuration = MessageProcessingConfiguration.builder()
				.interceptors(Collections.emptyList()).ackHandler(mock(AcknowledgementHandler.class))
				.errorHandler(mock(AsyncErrorHandler.class)).messageListener(messageListener).build();
		MessageProcessingContext<Object> context = MessageProcessingContext.create();
		MessageProcessingPipeline<Object> stage = new MessageListenerExecutionStage<>(configuration);
		CompletableFuture<Collection<Message<Object>>> result = stage.process(batch, context);
		assertThat(result).isCompletedWithValue(batch);
		verify(messageListener).onMessage(batch);

	}

}
