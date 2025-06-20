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
package io.awspring.cloud.sqs.listener.adapter;

import io.awspring.cloud.sqs.listener.MessageListener;
import java.util.Collection;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * {@link io.awspring.cloud.sqs.listener.MessageListener} implementation to handle a message by invoking a method
 * handler.
 *
 * @param <T> the {@link Message} payload type.
 *
 * @author Tomaz Fernandes
 * @author José Iêdo
 * @since 3.0
 */
public class MessagingMessageListenerAdapter<T> extends AbstractMethodInvokingListenerAdapter<T>
		implements MessageListener<T> {

	public MessagingMessageListenerAdapter(CompositeInvocableHandler compositeInvocableHandler) {
		super(compositeInvocableHandler);
	}

	public MessagingMessageListenerAdapter(InvocableHandlerMethod handlerMethod) {
		super(handlerMethod);
	}

	@Override
	public void onMessage(Message<T> message) {
		super.invokeHandler(message);
	}

	@Override
	public void onMessage(Collection<Message<T>> messages) {
		super.invokeHandler(messages);
	}
}
