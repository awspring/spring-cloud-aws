/*
 * Copyright 2013-2014 the original author or authors.
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

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.util.Assert;

/**
 * @author Alain Sahli
 */
public class SendToHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private final DestinationResolvingMessageSendingOperations<?> messageTemplate;

	public SendToHandlerMethodReturnValueHandler(DestinationResolvingMessageSendingOperations<?> messageTemplate) {
		this.messageTemplate = messageTemplate;
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return returnType.getMethodAnnotation(SendTo.class) != null;
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) throws Exception {
		Assert.state(this.messageTemplate != null, "A messageTemplate must be set to handle the return value.");

		if (getDestinationName(returnType) != null) {
			this.messageTemplate.convertAndSend(getDestinationName(returnType), returnValue);
		} else {
			this.messageTemplate.convertAndSend(returnValue);
		}
	}

	private String getDestinationName(MethodParameter returnType) {
		String[] destination = returnType.getMethodAnnotation(SendTo.class).value();
		return destination.length > 0 ? destination[0] : null;
	}
}
