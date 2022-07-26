/*
 * Copyright 2022 the original author or authors.
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
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class MessageGroupingSinkAdapter<T> extends AbstractDelegatingMessageListeningSinkAdapter<T> {

	private final Function<Message<T>, String> groupingFunction;

	public MessageGroupingSinkAdapter(MessageSink<T> delegate, Function<Message<T>, String> groupingFunction) {
		super(delegate);
		Assert.notNull(groupingFunction, "groupingFunction cannot be null.");
		this.groupingFunction = groupingFunction;
	}

	@Override
	public CompletableFuture<Void> emit(Collection<Message<T>> messages, MessageProcessingContext<T> context) {
		return CompletableFuture.allOf(messages.stream().collect(Collectors.groupingBy(this.groupingFunction))
			.values().stream()
			.map(messageBatch -> getDelegate().emit(messageBatch, context))
			.toArray(CompletableFuture[]::new));
	}

}
