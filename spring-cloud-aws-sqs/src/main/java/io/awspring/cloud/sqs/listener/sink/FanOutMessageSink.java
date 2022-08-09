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
package io.awspring.cloud.sqs.listener.sink;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * {@link MessageProcessingPipelineSink} implementation that processes all messages from the provided batch in parallel.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class FanOutMessageSink<T> extends AbstractMessageProcessingPipelineSink<T> {

	Logger logger = LoggerFactory.getLogger(FanOutMessageSink.class);

	@Override
	protected CompletableFuture<Void> doEmit(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		logger.trace("Emitting messages {}", MessageHeaderUtils.getId(messages));
		return CompletableFuture
				.allOf(messages.stream().map(msg -> execute(msg, context)).toArray(CompletableFuture[]::new));
	}
}
