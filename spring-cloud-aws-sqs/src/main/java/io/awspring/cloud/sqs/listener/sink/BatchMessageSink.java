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

import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * {@link MessageSink} implementation that emits the whole received batch of messages to the configured
 * {@link io.awspring.cloud.sqs.listener.pipeline.MessageProcessingPipeline}.
 *
 * @author Tomaz Fernandes
 * @author Mariusz Sondecki
 * @since 3.0
 */
public class BatchMessageSink<T> extends AbstractMessageProcessingPipelineSink<T> {

	@Override
	protected CompletableFuture<Void> doEmit(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		return tryObservedCompletableFuture(() -> execute(messages, context).exceptionally(t -> logError(t, messages)),
				messages);
	}

}
