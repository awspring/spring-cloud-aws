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
package io.awspring.cloud.sqs.listener.acknowledgement.handler;

import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
class AlwaysAcknowledgementHandlerTests extends AbstractAcknowledgementHandlerTests {

	@Test
	void shouldAckOnSuccess() {
		AcknowledgementHandler<String> handler = new AlwaysAcknowledgementHandler<>();
		CompletableFuture<Void> result = handler.onSuccess(message, callback);
		verify(callback).onAcknowledge(message);
	}

	@Test
	void shouldAckOnError() {
		AcknowledgementHandler<String> handler = new AlwaysAcknowledgementHandler<>();
		CompletableFuture<Void> result = handler.onError(messages, throwable, callback);
		verify(callback).onAcknowledge(messages);
	}

	@Test
	void shouldAckOnSuccessBatch() {
		AcknowledgementHandler<String> handler = new AlwaysAcknowledgementHandler<>();
		CompletableFuture<Void> result = handler.onSuccess(messages, callback);
		verify(callback).onAcknowledge(messages);
	}

	@Test
	void shouldAckOnErrorBatch() {
		AcknowledgementHandler<String> handler = new AlwaysAcknowledgementHandler<>();
		CompletableFuture<Void> result = handler.onError(messages, throwable, callback);
		verify(callback).onAcknowledge(messages);
	}

}
