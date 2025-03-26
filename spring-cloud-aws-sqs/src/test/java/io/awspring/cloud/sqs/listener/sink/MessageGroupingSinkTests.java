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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline;
import io.awspring.cloud.sqs.listener.sink.adapter.AbstractDelegatingMessageListeningSinkAdapter;
import io.awspring.cloud.sqs.listener.sink.adapter.MessageGroupingSinkAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import io.awspring.cloud.sqs.support.observation.AbstractListenerObservation;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Tests for {@link MessageGroupingSinkAdapter}.
 *
 * @author Tomaz Fernandes
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class MessageGroupingSinkTests {

	@Test
	void maintainsOrderWithinEachGroup() {
		String header = SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER;
		String firstMessageGroupId = UUID.randomUUID().toString();
		String secondMessageGroupId = UUID.randomUUID().toString();
		String thirdMessageGroupId = UUID.randomUUID().toString();
		List<Message<Integer>> firstMessageGroupMessages = IntStream.range(0, 10)
				.mapToObj(index -> createMessage(index, header, firstMessageGroupId)).collect(toList());
		List<Message<Integer>> secondMessageGroupMessages = IntStream.range(0, 10)
				.mapToObj(index -> createMessage(index, header, secondMessageGroupId)).collect(toList());
		List<Message<Integer>> thirdMessageGroupMessages = IntStream.range(0, 10)
				.mapToObj(index -> createMessage(index, header, thirdMessageGroupId)).collect(toList());
		List<Message<Integer>> messagesToEmit = new ArrayList<>();
		messagesToEmit.addAll(firstMessageGroupMessages);
		messagesToEmit.addAll(secondMessageGroupMessages);
		messagesToEmit.addAll(thirdMessageGroupMessages);

		List<Message<Integer>> received = Collections.synchronizedList(new ArrayList<>());

		MessageGroupingSinkAdapter<Integer> sinkAdapter = new MessageGroupingSinkAdapter<>(new OrderedMessageSink<>(),
				message -> message.getHeaders().get(header, String.class));
		sinkAdapter.setTaskExecutor(new SimpleAsyncTaskExecutor());
		sinkAdapter.setMessagePipeline(new MessageProcessingPipeline<Integer>() {
			@Override
			public CompletableFuture<Message<Integer>> process(Message<Integer> message,
					MessageProcessingContext<Integer> context) {
				try {
					Thread.sleep(new Random().nextInt(1000));
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				Message<Integer> messageWithoutObservation = MessageHeaderUtils.removeHeaderIfPresent(message, ObservationThreadLocalAccessor.KEY);
				received.add(messageWithoutObservation);
				return CompletableFuture.completedFuture(messageWithoutObservation);
			}
		});
		setupObservationMocks(sinkAdapter);

		sinkAdapter.start();
		sinkAdapter.emit(messagesToEmit, MessageProcessingContext.create()).join();
		Map<String, List<Message<Integer>>> receivedMessages = received.stream()
				.collect(groupingBy(message -> (String) message.getHeaders().get(header)));
		assertThat(receivedMessages.get(firstMessageGroupId)).containsExactlyElementsOf(firstMessageGroupMessages);
		assertThat(receivedMessages.get(secondMessageGroupId)).containsExactlyElementsOf(secondMessageGroupMessages);
		assertThat(receivedMessages.get(thirdMessageGroupId)).containsExactlyElementsOf(thirdMessageGroupMessages);
	}

	@NotNull
	private Message<Integer> createMessage(int index, String header, String thirdMessageGroupId) {
		return MessageBuilder.withPayload(index).setHeader(header, thirdMessageGroupId).build();
	}

	private void setupObservationMocks(AbstractDelegatingMessageListeningSinkAdapter<Integer> sink) {
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



}
