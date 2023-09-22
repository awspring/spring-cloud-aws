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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NeverAcknowledgementHandler}.
 *
 * @author Tomaz Fernandes
 */
class NeverAcknowledgementHandlerTests extends AbstractAcknowledgementHandlerTests {

	@Test
	void shouldNotAckOnSuccess() {
		AcknowledgementHandler<String> handler = new NeverAcknowledgementHandler<>();
		CompletableFuture<Void> result = handler.onSuccess(message, callback);
		verify(callback, never()).onAcknowledge(message);
	}

	@Test
	void shouldNotAckOnError() {
		AcknowledgementHandler<String> handler = new NeverAcknowledgementHandler<>();
		CompletableFuture<Void> result = handler.onError(messages, throwable, callback);
		verify(callback, never()).onAcknowledge(messages);
	}

	@Test
	void shouldNotAckOnSuccessBatch() {
		AcknowledgementHandler<String> handler = new NeverAcknowledgementHandler<>();
		CompletableFuture<Void> result = handler.onSuccess(messages, callback);
		verify(callback, never()).onAcknowledge(messages);
	}

	@Test
	void shouldNotAckOnErrorBatch() {
		AcknowledgementHandler<String> handler = new NeverAcknowledgementHandler<>();
		CompletableFuture<Void> result = handler.onError(messages, throwable, callback);
		verify(callback, never()).onAcknowledge(messages);
	}

}
