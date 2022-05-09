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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.awspring.cloud.sqs.CompletableFutures;
import org.springframework.messaging.Message;

/**
 * Interface to process incoming {@link Message}s in a non-blocking fashion.
 *
 * The non-blocking approach enables higher throughput for the application.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
@FunctionalInterface
public interface AsyncMessageListener<T> {

	/**
	 * Process the provided message.
	 * @param message the message.
	 * @return a completable future.
	 */
	CompletableFuture<Void> onMessage(Message<T> message);

	default CompletableFuture<Void> onMessage(Collection<Message<T>> messages) {
		return CompletableFutures
			.failedFuture(new UnsupportedOperationException("Batch not implemented for this ErrorHandler"));
	}

}
