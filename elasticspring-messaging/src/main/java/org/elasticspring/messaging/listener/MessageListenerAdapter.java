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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;

import java.lang.reflect.InvocationTargetException;

/**
 * {@link org.springframework.messaging.MessageHandler} implementation that calls any method through reflection on the
 * target object. This instance
 * will also convert the messages from a {@link org.springframework.messaging.Message} instance into a listener method
 * specific object. This message
 * listener implementation uses a {@link MessageConverter} to convert from the messaging specific message into the
 * method parameter. The method parameter of the particular method inside the instance must match the converted object
 * through the MessageConverter.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class MessageListenerAdapter implements MessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageListenerAdapter.class);
	private final MessageConverter messageConverter;
	private final Object delegate;
	private final String listenerMethod;

	/**
	 * Creates a new instance of the MessageListenerAdapter with the mandatory dependencies in order to process the
	 * messages.
	 *
	 * @param messageConverter
	 * 		- the message converter used to convert from the Amazon SDK specific message into the method parameter object.
	 * 		The
	 * 		method parameter might be a standard java type (e.g. java.lang.String) or any complex object.
	 * @param delegate
	 * 		- the actual target instance that contains the method that will be called. The method of the instance (and the
	 * 		instance itself) must be thread safe because this implementation might call the method on the instance in a
	 * 		parallel way.
	 * @param listenerMethod
	 * 		- the name of the public, non static method on the delegate that will be called by this MessageHandler
	 */
	public MessageListenerAdapter(MessageConverter messageConverter, Object delegate, String listenerMethod) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		Assert.notNull(delegate, "delegate must not be null");
		Assert.notNull(listenerMethod, "listenerMethod must not be null");
		this.messageConverter = messageConverter;
		this.delegate = delegate;
		this.listenerMethod = listenerMethod;
	}

	/**
	 * Returns the delegate object for subclasses of this implementation.
	 *
	 * @return - the delegate object that contains the target method
	 */
	protected Object getDelegate() {
		return this.delegate;
	}

	/**
	 * Return the listener method for subclasses of this implementation
	 *
	 * @return - the public, non static method that should be called on the target object
	 */
	protected String getListenerMethod() {
		return this.listenerMethod;
	}

	/**
	 * Template method that will be called by the {@link #handleMessage(org.springframework.messaging.Message)} method to
	 * prepare the arguments. Can be overridden by subclasses if the method arguments should be changed or extended
	 * (e.g.
	 * extract values from the payload and add method argument to the invocation).
	 *
	 * @param methodInvoker
	 * 		- the configured method invoker with the target object and the target method name
	 * @param payload
	 * 		-  the payload that has been produced by the MessageConverter based on the Amazon SQS message
	 */
	protected void prepareArguments(MethodInvoker methodInvoker, Object payload) {
		methodInvoker.setArguments(new Object[]{payload});
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		MethodInvoker methodInvoker = new MethodInvoker();
		methodInvoker.setTargetObject(getDelegate());
		methodInvoker.setTargetMethod(getListenerMethod());
		Object param = this.messageConverter.fromMessage(message, Object.class);

		prepareArguments(methodInvoker, param);

		try {
			LOGGER.debug("Preparing method invoker for object {} and method {} with argument(s) {}", getDelegate(), getListenerMethod(), methodInvoker.getArguments());
			methodInvoker.prepare();
		} catch (ClassNotFoundException e) {
			throw new MessageHandlingException(message, e);
		} catch (NoSuchMethodException e) {
			throw new MessageHandlingException(message, e);
		}

		try {
			methodInvoker.invoke();
		} catch (InvocationTargetException e) {
			throw new MessageHandlingException(message, e.getTargetException());
		} catch (IllegalAccessException e) {
			throw new MessageHandlingException(message, e.getCause());
		}
	}
}