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
package io.awspring.cloud.sqs.listener.interceptor;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.Visibility;
import io.awspring.cloud.sqs.listener.SqsMessageHeaders;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link AsyncMessageInterceptor} implementation for automatically extending the message's visibility in case it's less
 * than the minimum required for processing.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageVisibilityExtenderInterceptor<T> implements AsyncMessageInterceptor<T> {

	private static final Logger logger = LoggerFactory.getLogger(MessageVisibilityExtenderInterceptor.class);

	private static final int DEFAULT_MIN_VISIBILITY = 30;

	private static final int DEFAULT_QUEUE_VISIBILITY = 30;

	private int minimumVisibility = DEFAULT_MIN_VISIBILITY;

	@Override
	public CompletableFuture<Message<T>> intercept(Message<T> message) {
		Object receivedAtObject = message.getHeaders().get(SqsMessageHeaders.RECEIVED_AT);
		Object queueVisibilityObject = message.getHeaders().get(SqsMessageHeaders.QUEUE_VISIBILITY);
		if (receivedAtObject == null) {
			logger.warn("Header {} needs to be present to extend visibility. Skipping.", SqsMessageHeaders.RECEIVED_AT);
			return forwardMessage(message);
		}
		Instant receivedAt = (Instant) receivedAtObject;
		int queueVisibility = queueVisibilityObject != null ? (int) queueVisibilityObject : DEFAULT_QUEUE_VISIBILITY;

		return Instant.now().plusSeconds(this.minimumVisibility).isAfter(receivedAt.plusSeconds(queueVisibility))
				? doChangeVisibility(message)
				: forwardMessage(message);
	}

	private CompletableFuture<Message<T>> doChangeVisibility(Message<T> message) {
		logger.debug("Changing visibility of message {} to {}", MessageHeaderUtils.getId(message),
				this.minimumVisibility);
		return ((Visibility) Objects.requireNonNull(message.getHeaders().get(SqsMessageHeaders.VISIBILITY),
				"No Visibility found in " + message)).changeTo(this.minimumVisibility).thenApply(res -> message);
	}

	private CompletableFuture<Message<T>> forwardMessage(Message<T> message) {
		return CompletableFuture.completedFuture(message);
	}

	public void setMinimumVisibility(int minimumVisibility) {
		Assert.isTrue(minimumVisibility > 0, "minTimeToProcessMessage cannot be < 0");
		Assert.isTrue(minimumVisibility < 43200, "minTimeToProcessMessage cannot be > 43200");
		this.minimumVisibility = minimumVisibility;
	}
}
