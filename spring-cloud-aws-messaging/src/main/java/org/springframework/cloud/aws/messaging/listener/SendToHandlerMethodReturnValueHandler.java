/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.messaging.listener;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.Assert;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 */
public class SendToHandlerMethodReturnValueHandler
		implements HandlerMethodReturnValueHandler, BeanFactoryAware {

	private final DestinationResolvingMessageSendingOperations<?> messageTemplate;

	private BeanFactory beanFactory;

	public SendToHandlerMethodReturnValueHandler(
			DestinationResolvingMessageSendingOperations<?> messageTemplate) {
		this.messageTemplate = messageTemplate;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return returnType.getMethodAnnotation(SendTo.class) != null;
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			Message<?> message) throws Exception {
		Assert.state(this.messageTemplate != null,
				"A messageTemplate must be set to handle the return value.");

		if (returnValue != null) {
			if (getDestinationName(returnType) != null) {
				this.messageTemplate.convertAndSend(getDestinationName(returnType),
						returnValue);
			}
			else {
				this.messageTemplate.convertAndSend(returnValue);
			}
		}
	}

	private String getDestinationName(MethodParameter returnType) {
		String[] destination = returnType.getMethodAnnotation(SendTo.class).value();
		return destination.length > 0 ? resolveName(destination[0]) : null;
	}

	private String resolveName(String name) {
		if (!(this.beanFactory instanceof ConfigurableBeanFactory)) {
			return name;
		}

		ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) this.beanFactory;

		String placeholdersResolved = configurableBeanFactory.resolveEmbeddedValue(name);
		BeanExpressionResolver exprResolver = configurableBeanFactory
				.getBeanExpressionResolver();
		if (exprResolver == null) {
			return name;
		}
		Object result = exprResolver.evaluate(placeholdersResolved,
				new BeanExpressionContext(configurableBeanFactory, null));
		return result != null ? result.toString() : name;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
