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

package org.springframework.cloud.aws.messaging.listener;

import org.elasticspring.core.support.documentation.RuntimeUse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alain Sahli
 */
@RunWith(MockitoJUnitRunner.class)
public class SendToHandlerMethodReturnValueHandlerTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	@Mock
	private DestinationResolvingMessageSendingOperations<?> messageTemplate;

	@Test
	public void supportsReturnType_methodAnnotatedWithSendTo_trueIsReturned() throws Exception {
		// Arrange
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(null);
		Method validSendToMethod = this.getClass().getDeclaredMethod("validSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, 0);

		// Act
		boolean supports = sendToHandlerMethodReturnValueHandler.supportsReturnType(methodParameter);

		// Assert
		assertTrue(supports);
	}

	@Test
	public void supportsReturnType_methodWithoutSendToAnnotation_falseIsReturned() throws Exception {
		// Arrange
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(null);
		Method invalidSendToMethod = this.getClass().getDeclaredMethod("invalidSendToMethod");
		MethodParameter methodParameter = new MethodParameter(invalidSendToMethod, 0);

		// Act
		boolean supports = sendToHandlerMethodReturnValueHandler.supportsReturnType(methodParameter);

		// Assert
		assertFalse(supports);
	}

	@Test
	public void supportsReturnType_methodWithSendToAnnotationWithoutValue_trueIsReturned() throws Exception {
		// Arrange
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(null);
		Method validSendToMethod = this.getClass().getDeclaredMethod("anotherValidSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, 0);

		// Act
		boolean supports = sendToHandlerMethodReturnValueHandler.supportsReturnType(methodParameter);

		// Assert
		assertTrue(supports);
	}

	@Test
	public void handleReturnValue_withNullMessageTemplate_exceptionIsThrown() throws Exception {
		// Arrange
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("A messageTemplate must be set to handle the return value.");

		Method validSendToMethod = this.getClass().getDeclaredMethod("validSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, 0);
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(null);

		// Act
		sendToHandlerMethodReturnValueHandler.handleReturnValue("Return me!", methodParameter, MessageBuilder.withPayload("Nothing").build());
	}

	@Test
	public void handleReturnValue_withAMessageTemplateAndAValidMethodWithDestination_templateIsCalled() throws Exception {
		// Arrange
		Method validSendToMethod = this.getClass().getDeclaredMethod("validSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, 0);
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(this.messageTemplate);

		// Act
		sendToHandlerMethodReturnValueHandler.handleReturnValue("Elastic Hello!", methodParameter, MessageBuilder.withPayload("Nothing").build());

		// Assert
		verify(this.messageTemplate, times(1)).convertAndSend(eq("testQueue"), eq("Elastic Hello!"));
	}

	@Test
	public void handleReturnValue_withAMessageTemplateAndAValidMethodWithoutDestination_templateIsCalled() throws Exception {
		// Arrange
		Method validSendToMethod = this.getClass().getDeclaredMethod("anotherValidSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, 0);
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(this.messageTemplate);

		// Act
		sendToHandlerMethodReturnValueHandler.handleReturnValue("Another Elastic Hello!", methodParameter, MessageBuilder.withPayload("Nothing").build());

		// Assert
		verify(this.messageTemplate, times(1)).convertAndSend(eq("Another Elastic Hello!"));
	}


	@SuppressWarnings("SameReturnValue")
	@RuntimeUse
	@SendTo("testQueue")
	private String validSendToMethod() {
		return "Elastic Hello!";
	}

	@SuppressWarnings("SameReturnValue")
	@RuntimeUse
	@SendTo
	private String anotherValidSendToMethod() {
		return "Another Elastic Hello!";
	}

	@SuppressWarnings("SameReturnValue")
	@RuntimeUse
	private String invalidSendToMethod() {
		return "Just Hello!";
	}
}
