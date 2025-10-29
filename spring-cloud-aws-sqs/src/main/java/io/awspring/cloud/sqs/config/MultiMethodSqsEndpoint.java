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
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.sqs.annotation.SqsHandler;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.adapter.CompositeInvocableHandler;
import io.awspring.cloud.sqs.listener.adapter.MessagingMessageListenerAdapter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * A {@link SqsEndpoint} extension that supports multiple handler methods annotated with {@link SqsHandler}
 *
 * @author José Iêdo
 * @see SqsHandler
 */
public class MultiMethodSqsEndpoint extends AbstractEndpoint {

	private final Endpoint endpoint;

	private final List<Method> methods;

	private final @Nullable Method defaultMethod;

	protected MultiMethodSqsEndpoint(MultiMethodSqsEndpointBuilder builder) {
		super(builder.queueNames, builder.factoryName, builder.id);
		this.methods = builder.methods;
		this.defaultMethod = builder.defaultMethod;
		this.endpoint = builder.endpoint;
		this.setBean(builder.bean);
	}

	public static MultiMethodSqsEndpointBuilder builder() {
		return new MultiMethodSqsEndpointBuilder();
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public static class MultiMethodSqsEndpointBuilder {

		private List<Method> methods;

		private @Nullable Method defaultMethod;

		private Object bean;

		private String id;

		private Collection<String> queueNames;

		private String factoryName;

		private Endpoint endpoint;

		public MultiMethodSqsEndpointBuilder methods(List<Method> methods) {
			this.methods = methods;
			return this;
		}

		public MultiMethodSqsEndpointBuilder defaultMethod(@Nullable Method defaultMethod) {
			this.defaultMethod = defaultMethod;
			return this;
		}

		public MultiMethodSqsEndpointBuilder bean(Object bean) {
			this.bean = bean;
			return this;
		}

		public MultiMethodSqsEndpointBuilder queueNames(Collection<String> queueNames) {
			this.queueNames = queueNames;
			return this;
		}

		public MultiMethodSqsEndpointBuilder factoryBeanName(String factoryName) {
			this.factoryName = factoryName;
			return this;
		}

		public MultiMethodSqsEndpointBuilder sqsEndpoint(Endpoint sqsEndpoint) {
			this.endpoint = sqsEndpoint;
			return this;
		}

		public MultiMethodSqsEndpointBuilder id(String id) {
			this.id = id;
			return this;
		}

		public MultiMethodSqsEndpoint build() {
			return new MultiMethodSqsEndpoint(this);
		}
	}

	public List<Method> getMethods() {
		return methods;
	}

	@Override
	public <T> void setupContainer(MessageListenerContainer<T> container) {
		List<InvocableHandlerMethod> invocableHandlerMethods = new ArrayList<>();
		InvocableHandlerMethod defaultHandler = null;

		for (Method method : methods) {
			MessageHandlerMethodFactory messageHandlerMethodFactory = getMessageHandlerMethodFactory();
			if (messageHandlerMethodFactory != null) {
				InvocableHandlerMethod invocableHandlerMethod = messageHandlerMethodFactory
						.createInvocableHandlerMethod(getBean(), method);
				invocableHandlerMethods.add(invocableHandlerMethod);
				if (method.equals(defaultMethod)) {
					defaultHandler = invocableHandlerMethod;
				}
			}
		}

		CompositeInvocableHandler compositeInvocableHandler = new CompositeInvocableHandler(invocableHandlerMethods,
				defaultHandler);
		container.setMessageListener(createMessageListenerInstance(compositeInvocableHandler));
	}

	protected <T> MessageListener<T> createMessageListenerInstance(
			CompositeInvocableHandler compositeInvocableHandler) {
		return new MessagingMessageListenerAdapter<>(compositeInvocableHandler);
	}
}
