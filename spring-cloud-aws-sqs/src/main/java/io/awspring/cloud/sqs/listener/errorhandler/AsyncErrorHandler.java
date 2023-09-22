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
package io.awspring.cloud.sqs.listener.errorhandler;

import io.awspring.cloud.sqs.CompletableFutures;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * Interface for handling message processing errors async. If the error handler completes normally, the message or
 * messages will be considered recovered for further processing purposes. If the message should not be considered
 * recovered, an exception must be returned from the error handler.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface AsyncErrorHandler<T> {

	/**
	 * Asynchronously handle the errors thrown processing the given {@link Message}.
	 * @param message the message.
	 * @param t the thrown exception.
	 * @return a completable future.
	 */
	default CompletableFuture<Void> handle(Message<T> message, Throwable t) {
		return CompletableFutures
				.failedFuture(new UnsupportedOperationException("Single message error handling not implemented"));
	}

	/**
	 * Asynchronously handle the errors thrown processing the given {@link Message} instances.
	 * @param messages the messages.
	 * @param t the thrown exception.
	 * @return a completable future.
	 */
	default CompletableFuture<Void> handle(Collection<Message<T>> messages, Throwable t) {
		return CompletableFutures
				.failedFuture(new UnsupportedOperationException("Batch error handling not implemented"));
	}

}
