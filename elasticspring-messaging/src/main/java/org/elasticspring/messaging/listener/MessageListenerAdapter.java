/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.listener;

import org.elasticspring.messaging.Message;
import org.elasticspring.messaging.support.converter.MessageConverter;
import org.springframework.util.MethodInvoker;

import java.lang.reflect.InvocationTargetException;

public class MessageListenerAdapter implements MessageListener<String> {

	private final MessageConverter messageConverter;
	private final Object delegate;
	private final String listenerMethod;

	public MessageListenerAdapter(MessageConverter messageConverter, Object delegate, String listenerMethod) {
		this.messageConverter = messageConverter;
		this.delegate = delegate;
		this.listenerMethod = listenerMethod;
	}

	@Override
	public void onMessage(Message<String> message) {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(this.delegate);
		methodInvoker.setTargetMethod(this.listenerMethod);
		Object param = this.messageConverter.fromMessage(message);
		methodInvoker.setArguments(new Object[]{param});

		try {
			methodInvoker.prepare();
		} catch (ClassNotFoundException e) {
			throw new ListenerExecutionFailedException(e.getCause());
		} catch (NoSuchMethodException e) {
			throw new ListenerExecutionFailedException(e.getCause());
		}

		try {
			methodInvoker.invoke();
		} catch (InvocationTargetException e) {
			throw new ListenerExecutionFailedException(e.getTargetException());
		} catch (IllegalAccessException e) {
			throw new ListenerExecutionFailedException(e.getCause());
		}
	}
}
