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
package io.awspring.cloud.sqs.support.converter;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * A {@link MessagingMessageConverter} specialization that enables receving a {@link MessageConversionContext} that can
 * be used to add context specific properties to the converted message.
 * @author Tomaz Fernandes
 * @since 3.0
 * @see ContextAwareHeaderMapper
 */
public interface ContextAwareMessagingMessageConverter<S> extends MessagingMessageConverter<S> {

	@Override
	default Message<?> toMessagingMessage(S source) {
		return toMessagingMessage(source, null);
	}

	@Override
	default S fromMessagingMessage(Message<?> message) {
		return fromMessagingMessage(message, null);
	}

	/**
	 * Convert a source message from a specific messaging system to a {@link Message} with an optional context.
	 * @param source the source message.
	 * @param context an optional context with information to be used in the conversion process.
	 * @return the converted message.
	 */
	Message<?> toMessagingMessage(S source, @Nullable MessageConversionContext context);

	/**
	 * Convert a {@link Message} to a message from a specific messaging system.
	 * @param message the message from which to convert.
	 * @param context an optional context with information to be used in the conversion process.
	 * @return the system specific message.
	 */
	S fromMessagingMessage(Message<?> message, @Nullable MessageConversionContext context);


	/**
	 * An optional context to be used in the conversion process.
	 * @return the context.
	 */
	@Nullable
	MessageConversionContext createMessageConversionContext();

}
