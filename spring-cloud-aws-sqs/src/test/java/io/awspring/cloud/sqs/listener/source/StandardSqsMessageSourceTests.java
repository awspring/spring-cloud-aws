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
package io.awspring.cloud.sqs.listener.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * Tests for {@link StandardSqsMessageSource}.
 *
 * @author Tomaz Fernandes
 */
class StandardSqsMessageSourceTests {

	@Test
	void shouldNotAddReceiveRequestIdParameter() {
		List<Message> batch = IntStream.range(0, 10).mapToObj(
				index -> Message.builder().body(String.valueOf(index)).messageId(UUID.randomUUID().toString()).build())
				.collect(Collectors.toList());
		SqsAsyncClient client = mock(SqsAsyncClient.class);
		ReceiveMessageResponse response = ReceiveMessageResponse.builder().messages(batch).build();
		given(client.receiveMessage(any(ReceiveMessageRequest.class)))
				.willReturn(CompletableFuture.completedFuture(response));
		StandardSqsMessageSource<Object> source = new StandardSqsMessageSource<>();
		source.setSqsAsyncClient(client);
		source.doPollForMessages(10);
		ArgumentCaptor<ReceiveMessageRequest> captor = ArgumentCaptor.forClass(ReceiveMessageRequest.class);
		verify(client).receiveMessage(captor.capture());
		assertThat(captor.getValue()).extracting(ReceiveMessageRequest::receiveRequestAttemptId).isNull();
	}

}
