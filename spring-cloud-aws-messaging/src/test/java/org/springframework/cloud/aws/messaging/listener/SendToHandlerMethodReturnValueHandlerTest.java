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

import java.lang.reflect.Method;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 */
@RunWith(MockitoJUnitRunner.class)
public class SendToHandlerMethodReturnValueHandlerTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private DestinationResolvingMessageSendingOperations<?> messageTemplate;

	@Test
	public void supportsReturnType_methodAnnotatedWithSendTo_trueIsReturned()
			throws Exception {
		// Arrange
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
				null);
		Method validSendToMethod = this.getClass().getDeclaredMethod("validSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, -1);

		// Act
		boolean supports = sendToHandlerMethodReturnValueHandler
				.supportsReturnType(methodParameter);

		// Assert
		assertThat(supports).isTrue();
	}

	@Test
	public void supportsReturnType_methodWithoutSendToAnnotation_falseIsReturned()
			throws Exception {
		// Arrange
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
				null);
		Method invalidSendToMethod = this.getClass()
				.getDeclaredMethod("invalidSendToMethod");
		MethodParameter methodParameter = new MethodParameter(invalidSendToMethod, -1);

		// Act
		boolean supports = sendToHandlerMethodReturnValueHandler
				.supportsReturnType(methodParameter);

		// Assert
		assertThat(supports).isFalse();
	}

	@Test
	public void supportsReturnType_methodWithSendToAnnotationWithoutValue_trueIsReturned()
			throws Exception {
		// Arrange
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
				null);
		Method validSendToMethod = this.getClass()
				.getDeclaredMethod("anotherValidSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, -1);

		// Act
		boolean supports = sendToHandlerMethodReturnValueHandler
				.supportsReturnType(methodParameter);

		// Assert
		assertThat(supports).isTrue();
	}

	@Test
	public void handleReturnValue_withNullMessageTemplate_exceptionIsThrown()
			throws Exception {
		// Arrange
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage(
				"A messageTemplate must be set to handle the return value.");

		Method validSendToMethod = this.getClass().getDeclaredMethod("validSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, -1);
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
				null);

		// Act
		sendToHandlerMethodReturnValueHandler.handleReturnValue("Return me!",
				methodParameter, MessageBuilder.withPayload("Nothing").build());
	}

	@Test
	public void handleReturnValue_withNullReturnValue_NoMessageTemplateIsCalled()
			throws Exception {
		// Arrange
		Method validSendToMethod = this.getClass().getDeclaredMethod("validSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, -1);
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
				this.messageTemplate);

		// Act
		sendToHandlerMethodReturnValueHandler.handleReturnValue(null, methodParameter,
				MessageBuilder.withPayload("Nothing").build());

		// Assert
		verify(this.messageTemplate, times(0)).convertAndSend(anyString(), anyString());
	}

	@Test
	public void handleReturnValue_withAMessageTemplateAndAValidMethodWithDestination_templateIsCalled()
			throws Exception {
		// Arrange
		Method validSendToMethod = this.getClass().getDeclaredMethod("validSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, -1);
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
				this.messageTemplate);

		// Act
		sendToHandlerMethodReturnValueHandler.handleReturnValue("Elastic Hello!",
				methodParameter, MessageBuilder.withPayload("Nothing").build());

		// Assert
		verify(this.messageTemplate, times(1)).convertAndSend(eq("testQueue"),
				eq("Elastic Hello!"));
	}

	@Test
	public void handleReturnValue_withExpressionInSendToName_templateIsCalled()
			throws Exception {
		// Arrange
		Method validSendToMethod = this.getClass().getDeclaredMethod("expressionMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, -1);

		GenericApplicationContext applicationContext = new GenericApplicationContext();
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("queueName", "myTestQueue");

		applicationContext.getEnvironment().getPropertySources().addLast(propertySource);
		applicationContext.refresh();

		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
				this.messageTemplate);
		sendToHandlerMethodReturnValueHandler
				.setBeanFactory(applicationContext.getAutowireCapableBeanFactory());

		// Act
		sendToHandlerMethodReturnValueHandler.handleReturnValue("expression method",
				methodParameter, MessageBuilder.withPayload("Nothing").build());

		// Assert
		verify(this.messageTemplate, times(1)).convertAndSend(eq("myTestQueue"),
				eq("expression method"));
	}

	@Test
	public void handleReturnValue_withPlaceHolderInSendToName_templateIsCalled()
			throws Exception {
		// Arrange
		Method validSendToMethod = this.getClass().getDeclaredMethod("placeHolderMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, -1);

		GenericApplicationContext applicationContext = new GenericApplicationContext();
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("placeholderQueueName", "myTestQueue");

		applicationContext.getEnvironment().getPropertySources().addLast(propertySource);
		applicationContext.registerBeanDefinition("resolver",
				BeanDefinitionBuilder
						.genericBeanDefinition(PropertySourcesPlaceholderConfigurer.class)
						.getBeanDefinition());

		applicationContext.refresh();

		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
				this.messageTemplate);
		sendToHandlerMethodReturnValueHandler
				.setBeanFactory(applicationContext.getAutowireCapableBeanFactory());

		// Act
		sendToHandlerMethodReturnValueHandler.handleReturnValue("placeholder method",
				methodParameter, MessageBuilder.withPayload("Nothing").build());

		// Assert
		verify(this.messageTemplate, times(1)).convertAndSend(eq("myTestQueue"),
				eq("placeholder method"));
	}

	// @checkstyle:off
	@Test
	public void handleReturnValue_withAMessageTemplateAndAValidMethodWithoutDestination_templateIsCalled()
			throws Exception {
		// @checkstyle:on
		// Arrange
		Method validSendToMethod = this.getClass()
				.getDeclaredMethod("anotherValidSendToMethod");
		MethodParameter methodParameter = new MethodParameter(validSendToMethod, -1);
		SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler;
		sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
				this.messageTemplate);

		// Act
		sendToHandlerMethodReturnValueHandler.handleReturnValue("Another Elastic Hello!",
				methodParameter, MessageBuilder.withPayload("Nothing").build());

		// Assert
		verify(this.messageTemplate, times(1))
				.convertAndSend(eq("Another Elastic Hello!"));
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

	@SuppressWarnings("SameReturnValue")
	@RuntimeUse
	@SendTo("#{environment.queueName}")
	private String expressionMethod() {
		return "expression method";
	}

	@SuppressWarnings("SameReturnValue")
	@RuntimeUse
	@SendTo("${placeholderQueueName}")
	private String placeHolderMethod() {
		return "placeholder method";
	}

}
