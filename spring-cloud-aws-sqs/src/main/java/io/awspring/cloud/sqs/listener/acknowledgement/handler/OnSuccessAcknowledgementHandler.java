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

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * {@link AcknowledgementHandler} implementation that only acknowledges on success.
 * @author Tomaz Fernandes
 * @since 3.0
 * @see AcknowledgementMode#ON_SUCCESS
 */
public class OnSuccessAcknowledgementHandler<T> implements AcknowledgementHandler<T> {

	private static final Logger logger = LoggerFactory.getLogger(OnSuccessAcknowledgementHandler.class);

	@Override
	public CompletableFuture<Void> onSuccess(Message<T> message, AcknowledgementCallback<T> callback) {
		logger.trace("Acknowledging message {}", MessageHeaderUtils.getId(message));
		return callback.onAcknowledge(message);
	}

	@Override
	public CompletableFuture<Void> onSuccess(Collection<Message<T>> messages, AcknowledgementCallback<T> callback) {
		logger.trace("Acknowledging messages {}", MessageHeaderUtils.getId(messages));
		return callback.onAcknowledge(messages);
	}

	@Override
	public CompletableFuture<Void> onError(Message<T> message, Throwable t, AcknowledgementCallback<T> callback) {
		logger.trace("Skipping ack for message {}", MessageHeaderUtils.getId(message));
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Void> onError(Collection<Message<T>> messages, Throwable t,
			AcknowledgementCallback<T> callback) {
		logger.trace("Skipping acks for messages {}", MessageHeaderUtils.getId(messages));
		return CompletableFuture.completedFuture(null);
	}

}
