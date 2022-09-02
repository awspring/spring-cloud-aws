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
package io.awspring.cloud.sqs.listener.sink.adapter;

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.listener.MessageProcessingContext;
import io.awspring.cloud.sqs.listener.sink.MessageSink;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link AbstractDelegatingMessageListeningSinkAdapter} implementation that groups the received batch according to the
 * provided grouping function and emits each sub batch to the delegate separately.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see io.awspring.cloud.sqs.listener.FifoSqsComponentFactory
 */
public class MessageGroupingSinkAdapter<T> extends AbstractDelegatingMessageListeningSinkAdapter<T> {

	private static final Logger logger = LoggerFactory.getLogger(MessageGroupingSinkAdapter.class);

	private final Function<Message<T>, String> groupingFunction;

	public MessageGroupingSinkAdapter(MessageSink<T> delegate, Function<Message<T>, String> groupingFunction) {
		super(delegate);
		Assert.notNull(groupingFunction, "groupingFunction cannot be null.");
		this.groupingFunction = groupingFunction;
	}

	// @formatter:off
	@Override
	public CompletableFuture<Void> emit(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		logger.trace("Emitting messages {}", MessageHeaderUtils.getId(messages));
		return CompletableFuture.allOf(messages.stream().collect(Collectors.groupingBy(this.groupingFunction))
			.values().stream()
			.map(messageBatch -> getDelegate().emit(messageBatch, context))
			.toArray(CompletableFuture[]::new));
	}
	// @formatter:on

}
