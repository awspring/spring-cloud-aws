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

import io.awspring.cloud.messaging.support.listener.AsyncMessageInterceptor;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageVisibilityExtenderInterceptor<T> implements AsyncMessageInterceptor<T> {

	private static final Logger logger = LoggerFactory.getLogger(MessageVisibilityExtenderInterceptor.class);

	private static final int DEFAULT_MIN_VISIBILITY = 30;

	private static final int DEFAULT_QUEUE_VISIBILITY = 30;

	private final SqsAsyncClient asyncClient;

	private int minTimeToProcessMessage = DEFAULT_MIN_VISIBILITY;

	public MessageVisibilityExtenderInterceptor(SqsAsyncClient asyncClient) {
		this.asyncClient = asyncClient;
	}

	@Override
	public CompletableFuture<Message<T>> intercept(Message<T> message) {
		Object receivedAtObject = message.getHeaders().get(SqsMessageHeaders.RECEIVED_AT);
		Object queueVisibilityObject = message.getHeaders().get(SqsMessageHeaders.QUEUE_VISIBILITY);
		if (receivedAtObject == null) {
			logger.warn("Header {} needs to be present to extend visibility. Skipping.", SqsMessageHeaders.RECEIVED_AT);
			return forwardMessage(message);
		}
		int queueVisibility = queueVisibilityObject != null ? (int) queueVisibilityObject : DEFAULT_QUEUE_VISIBILITY;

		return Instant.now().plusSeconds(this.minTimeToProcessMessage)
				.isAfter(((Instant) receivedAtObject).plusSeconds(queueVisibility)) ? doChangeVisibility(message)
						: forwardMessage(message);
	}

	private CompletableFuture<Message<T>> doChangeVisibility(Message<T> message) {
		return ((Visibility) Objects.requireNonNull(message.getHeaders().get(SqsMessageHeaders.VISIBILITY),
				"No Visibility found in " + message)).changeTo(this.minTimeToProcessMessage).thenApply(res -> message);
	}

	private CompletableFuture<Message<T>> forwardMessage(Message<T> message) {
		return CompletableFuture.supplyAsync(() -> message);
	}

	public void setMinTimeToProcessMessage(int minTimeToProcessMessage) {
		Assert.isTrue(minTimeToProcessMessage > 0, "minTimeToProcessMessage cannot be < 0");
		Assert.isTrue(minTimeToProcessMessage < 43200, "minTimeToProcessMessage cannot be > 43200");
		this.minTimeToProcessMessage = minTimeToProcessMessage;
	}
}
