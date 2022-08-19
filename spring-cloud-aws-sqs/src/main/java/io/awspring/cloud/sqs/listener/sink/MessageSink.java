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

import io.awspring.cloud.sqs.listener.AsyncMessageListener;
import io.awspring.cloud.sqs.listener.ConfigurableContainerComponent;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * Component that handles the flow of {@link Message}s.
 *
 * This interface is non-opinionated regarding strategies or the output to which messages will be emitted to.
 *
 * A {@link MessageProcessingContext} can be used to pass additional state to the implementation.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
@FunctionalInterface
public interface MessageSink<T> extends ConfigurableContainerComponent {

	/**
	 * Emit the provided {@link Message} instances to the provided {@link AsyncMessageListener}.
	 * @param messages the messages to emit.
	 * @return a collection of {@link CompletableFuture} instances, each representing the completion signal of a single
	 * message processing.
	 */
	CompletableFuture<Void> emit(Collection<Message<T>> messages, MessageProcessingContext<T> context);

}
