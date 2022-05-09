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
import io.awspring.cloud.sqs.MessageHeaderUtils;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * Default {@link AsyncErrorHandler} implementation that logs errors.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class LoggingErrorHandler<T> implements AsyncErrorHandler<T> {

	private static final Logger logger = LoggerFactory.getLogger(LoggingErrorHandler.class);

	@Override
	public CompletableFuture<Void> handleError(Message<T> message, Throwable t) {
		logger.error("Error processing message {}", MessageHeaderUtils.getId(message), t);
		return CompletableFutures.failedFuture(t);
	}

	@Override
	public CompletableFuture<Void> handleError(Collection<Message<T>> messages, Throwable t) {
		logger.error("Error processing {} messages", messages.size(), t);
		return CompletableFutures.failedFuture(t);
	}
}
