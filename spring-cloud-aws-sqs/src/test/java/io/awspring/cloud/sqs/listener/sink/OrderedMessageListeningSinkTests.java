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
package io.awspring.cloud.sqs.listener.sink;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import io.awspring.cloud.sqs.support.observation.AbstractListenerObservation;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Tests for {@link OrderedMessageSink}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class OrderedMessageListeningSinkTests {

	@Test
	void shouldEmitInOrder() {
		int numberOfMessagesToEmit = 1000;
		List<Message<Integer>> messagesToEmit = IntStream.range(0, numberOfMessagesToEmit)
				.mapToObj(index -> MessageBuilder.withPayload(index).build()).collect(toList());
		List<Message<Integer>> received = new ArrayList<>(numberOfMessagesToEmit);
		AbstractMessageProcessingPipelineSink<Integer> sink = new OrderedMessageSink<>();

		setupObservationMocks(sink);

		sink.setTaskExecutor(Runnable::run);
		sink.setMessagePipeline(getMessageProcessingPipeline(received));
		sink.start();
		sink.emit(messagesToEmit, MessageProcessingContext.create()).join();
		sink.stop();
		assertThat(received).containsSequence(messagesToEmit);
	}

	private void setupObservationMocks(AbstractMessageProcessingPipelineSink<Integer> sink) {
		AbstractListenerObservation.Specifics specifics = mock(AbstractListenerObservation.Specifics.class);
		ObservationConvention convention = mock(ObservationConvention.class);
		AbstractListenerObservation.Context context = mock(AbstractListenerObservation.Context.class);
		AbstractListenerObservation.Documentation documentation = mock(AbstractListenerObservation.Documentation.class);
		Observation observation = mock(Observation.class);
		given(specifics.getDefaultConvention()).willReturn(convention);
		given(specifics.createContext(any())).willReturn(context);
		given(specifics.getDocumentation()).willReturn(documentation);
		given(documentation.start(any(), any(), any(), any())).willReturn(observation);
		sink.setObservationSpecifics(specifics);
	}

	private MessageProcessingPipeline<Integer> getMessageProcessingPipeline(List<Message<Integer>> received) {
		return new MessageProcessingPipeline<Integer>() {
			@Override
			public CompletableFuture<Message<Integer>> process(Message<Integer> message,
					MessageProcessingContext<Integer> context) {
				Message<Integer> messageWithoutObservation = MessageHeaderUtils.removeHeaderIfPresent(message, ObservationThreadLocalAccessor.KEY);
				received.add(messageWithoutObservation);
				return CompletableFuture.completedFuture(messageWithoutObservation);
			}
		};
	}

}
