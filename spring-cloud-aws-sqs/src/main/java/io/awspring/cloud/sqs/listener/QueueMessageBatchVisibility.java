/*
 * Copyright 2013-2019 the original author or authors.
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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * {@link BatchVisibility} implementation for SQS messages.
 *
 * @author Clement Denis
 * @since 3.3
 */
public class QueueMessageBatchVisibility<T> implements BatchVisibility<T> {

	private final Collection<Message<T>> messages;

	/**
	 * Create an instance for changing the visibility in batch for the provided queue.
	 *
	 * @param messages the messages in the batch
	 */
	public QueueMessageBatchVisibility(Collection<Message<T>> messages) {
		this.messages = messages;
	}

	@Override
	public CompletableFuture<Void> changeToAsync(int seconds) {
		return changeToAsync(messages, seconds);
	}

	@Override
	public CompletableFuture<Void> changeToAsync(Collection<Message<T>> messages, int seconds) {
		QueueMessageVisibility first = QueueMessageVisibility.fromMessage(messages.iterator().next());
		return first.changeToAsyncBatch(seconds, messages);
	}
}
