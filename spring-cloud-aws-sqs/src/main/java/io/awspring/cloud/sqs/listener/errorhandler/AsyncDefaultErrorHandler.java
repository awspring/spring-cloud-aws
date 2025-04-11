/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.listener.errorhandler;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.QueueMessageVisibility;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.listener.Visibility;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * A default error handler implementation for asynchronous message processing.
 *
 * <p>
 * This error handler attempts to set the SQS message visibility timeout to zero whenever an exception occurs,
 * effectively making the message immediately available for reprocessing.
 *
 * <p>
 * Returning a failed future ensures that the message is <strong>not acknowledged</strong>.
 * If a successful future were returned, the message would be considered
 * successfully recovered and acknowledged.
 * @author Bruno Garcia
 * @author Rafael Pavarini
 */
public class AsyncDefaultErrorHandler<T> implements AsyncErrorHandler<T> {

	@Override
	public CompletableFuture<Void> handle(Message<T> message, Throwable t) {
		changeTimeoutToZero(message);
		return CompletableFuture.failedFuture(t);
	}

	@Override
	public CompletableFuture<Void> handle(Collection<Message<T>> messages, Throwable t) {
		changeTimeoutToZero(messages);
		return CompletableFuture.failedFuture(t);
	}

	private void changeTimeoutToZero(Message<T> message) {
		Visibility visibilityTimeout = getVisibilityTimeout(message);
		visibilityTimeout.changeToAsync(0);
	}

	private void changeTimeoutToZero(Collection<Message<T>> messages) {
		QueueMessageVisibility firstVisibilityMessage = (QueueMessageVisibility) getVisibilityTimeout(messages.iterator().next());

		Collection<Message<?>> castedMessages = messages.stream()
			.map(m -> (Message<?>) m)
			.collect(Collectors.toList());

		firstVisibilityMessage.toBatchVisibility(castedMessages).changeToAsync(0);
	}

	private Visibility getVisibilityTimeout(Message<T> message) {
		return MessageHeaderUtils.getHeader(message, SqsHeaders.SQS_VISIBILITY_TIMEOUT_HEADER, Visibility.class);
	}
}
