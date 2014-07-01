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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticspring.core.support.documentation.RuntimeUse;
import org.elasticspring.messaging.config.annotation.NotificationMessage;
import org.elasticspring.messaging.config.annotation.NotificationSubject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class QueueMessageHandlerTest {

	@Mock
	private DestinationResolvingMessageSendingOperations<?> messageTemplate;

	@Test
	public void receiveMessage_methodAnnotatedWithMessageMappingAnnotation_methodInvokedForIncomingMessage() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler", IncomingMessageHandler.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		messageHandler.handleMessage(MessageBuilder.withPayload("testContent").setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "receive").build());

		IncomingMessageHandler messageListener = applicationContext.getBean(IncomingMessageHandler.class);
		assertEquals("testContent", messageListener.getLastReceivedMessage());
	}

	@Test
	public void receiveMessage_methodWithCustomObjectAsParameter_parameterIsConverted() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler", IncomingMessageHandlerWithCustomParameter.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		DummyKeyValueHolder messagePayload = new DummyKeyValueHolder("myKey", "A value");
		MappingJackson2MessageConverter jsonMapper = new MappingJackson2MessageConverter();
		Message<?> message = jsonMapper.toMessage(messagePayload, new MessageHeaders(Collections.<String, Object>singletonMap(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "testQueue")));
		messageHandler.handleMessage(message);

		IncomingMessageHandlerWithCustomParameter messageListener = applicationContext.getBean(IncomingMessageHandlerWithCustomParameter.class);
		assertNotNull(messageListener.getLastReceivedMessage());
		assertEquals("myKey", messageListener.getLastReceivedMessage().getKey());
		assertEquals("A value", messageListener.getLastReceivedMessage().getValue());
	}

	@Test
	public void receiveAndReplyMessage_methodAnnotatedWithMessageMappingAnnotation_methodInvokedForIncomingMessageAndReplySentBackToSendToDestination() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler", IncomingMessageHandler.class);
		applicationContext.registerBeanDefinition("queueMessageHandler", getQueueMessageHandlerBeanDefinition());
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		messageHandler.handleMessage(MessageBuilder.withPayload("testContent").setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "receiveAndReply").build());

		IncomingMessageHandler messageListener = applicationContext.getBean(IncomingMessageHandler.class);
		assertEquals("testContent", messageListener.getLastReceivedMessage());
		verify(this.messageTemplate).convertAndSend(eq("sendTo"), eq("TESTCONTENT"));
	}

	private AbstractBeanDefinition getQueueMessageHandlerBeanDefinition() {
		BeanDefinitionBuilder queueMessageHandlerBeanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(QueueMessageHandler.class);
		queueMessageHandlerBeanDefinitionBuilder.addPropertyValue("defaultReturnValueHandler", new SendToHandlerMethodReturnValueHandler(this.messageTemplate));
		return queueMessageHandlerBeanDefinitionBuilder.getBeanDefinition();
	}

	@Test
	public void receiveMessage_methodAnnotatedWithMessageMappingContainingMultipleQueueNames_methodInvokedForEachQueueName() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandlerWithMultipleQueueNames", IncomingMessageHandlerWithMultipleQueueNames.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);
		IncomingMessageHandlerWithMultipleQueueNames incomingMessageHandler = applicationContext.getBean(IncomingMessageHandlerWithMultipleQueueNames.class);

		queueMessageHandler.handleMessage(MessageBuilder.withPayload("Hello from queue one!").setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "queueOne").build());
		assertEquals("Hello from queue one!", incomingMessageHandler.getLastReceivedMessage());

		queueMessageHandler.handleMessage(MessageBuilder.withPayload("Hello from queue two!").setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "queueTwo").build());
		assertEquals("Hello from queue two!", incomingMessageHandler.getLastReceivedMessage());
	}

	@Test
	public void receiveMessage_withHeaderAnnotationAsArgument_shouldReceiveRequestedHeader() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("messageHandlerWithHeaderAnnotation", MessageReceiverWithHeaderAnnotation.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);
		MessageReceiverWithHeaderAnnotation messageReceiver = applicationContext.getBean(MessageReceiverWithHeaderAnnotation.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder.withPayload("Hello from a sender").setHeader("SenderId", "elsUnitTest")
				.setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "testQueue").build());

		// Assert
		assertEquals("Hello from a sender", messageReceiver.getPayload());
		assertEquals("elsUnitTest", messageReceiver.getSenderId());
	}

	@Test
	public void receiveMessage_withWrongHeaderAnnotationValueAsArgument_shouldReceiveNullAsHeaderValue() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("messageHandlerWithHeaderAnnotation", MessageReceiverWithHeaderAnnotation.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);
		MessageReceiverWithHeaderAnnotation messageReceiver = applicationContext.getBean(MessageReceiverWithHeaderAnnotation.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder.withPayload("Hello from a sender")
				.setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "testQueue").build());

		// Assert
		assertEquals("Hello from a sender", messageReceiver.getPayload());
		assertNull(messageReceiver.getSenderId());
	}

	@Test
	public void receiveMessage_withHeadersAsArgumentAnnotation_shouldReceiveAllHeaders() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("messageHandlerWithHeadersAnnotation", MessageReceiverWithHeadersAnnotation.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);
		MessageReceiverWithHeadersAnnotation messageReceiver = applicationContext.getBean(MessageReceiverWithHeadersAnnotation.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder.withPayload("Hello from a sender")
				.setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "testQueue").setHeader("SenderId", "ID").build());

		// Assert
		assertNotNull(messageReceiver.getHeaders());
		assertEquals("ID", messageReceiver.getHeaders().get("SenderId"));
		assertEquals("testQueue", messageReceiver.getHeaders().get(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY));
	}

	@Test
	public void receiveMessage_withCustomArgumentResolvers_shouldCallThemBeforeTheDefaultOnes() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler", IncomingMessageHandler.class);

		HandlerMethodArgumentResolver handlerMethodArgumentResolver = mock(HandlerMethodArgumentResolver.class);
		when(handlerMethodArgumentResolver.supportsParameter(any(MethodParameter.class))).thenReturn(true);
		when(handlerMethodArgumentResolver.resolveArgument(any(MethodParameter.class), any(Message.class))).thenReturn("Hello from a sender");
		MutablePropertyValues properties = new MutablePropertyValues(
				Collections.singletonList(new PropertyValue("customArgumentResolvers", handlerMethodArgumentResolver)));
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class, properties);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder.withPayload("Hello from a sender")
				.setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "receive").build());

		// Assert
		verify(handlerMethodArgumentResolver, times(1)).resolveArgument(any(MethodParameter.class), any(Message.class));
	}

	@Test
	public void receiveMessage_withCustomReturnValueHandlers_shouldCallThemBeforeTheDefaultOnes() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler", IncomingMessageHandler.class);

		HandlerMethodReturnValueHandler handlerMethodReturnValueHandler = mock(HandlerMethodReturnValueHandler.class);
		when(handlerMethodReturnValueHandler.supportsReturnType(any(MethodParameter.class))).thenReturn(true);
		MutablePropertyValues properties = new MutablePropertyValues(
				Collections.singletonList(new PropertyValue("customReturnValueHandlers", handlerMethodReturnValueHandler)));
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class, properties);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder.withPayload("Hello from a sender")
				.setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "receiveAndReply").build());

		// Assert
		verify(handlerMethodReturnValueHandler, times(1)).handleReturnValue(any(Object.class), any(MethodParameter.class), any(Message.class));

	}

	@Test
	public void receiveMessage_withNotificationMessageAndSubject_shouldResolveThem() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("notificationMessageReceiver", NotificationMessageReceiver.class);
		applicationContext.registerSingleton("queueMessageHandler", QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext.getBean(QueueMessageHandler.class);
		NotificationMessageReceiver notificationMessageReceiver = applicationContext.getBean(NotificationMessageReceiver.class);

		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Subject", "Hi!");
		jsonObject.put("Message", "Hello World!");
		String payload = jsonObject.toString();

		// Act
		queueMessageHandler.handleMessage(MessageBuilder.withPayload(payload)
				.setHeader(QueueMessageHeaders.LOGICAL_RESOURCE_ID_MESSAGE_HEADER_KEY, "testQueue").build());

		// Assert
		assertEquals("Hi!", notificationMessageReceiver.getSubject());
		assertEquals("Hello World!", notificationMessageReceiver.getMessage());
	}

	@SuppressWarnings("UnusedDeclaration")
	private static class IncomingMessageHandler {

		private String lastReceivedMessage;

		@MessageMapping("receive")
		public void receive(@Payload String value) {
			this.lastReceivedMessage = value;
		}

		@MessageMapping("receiveAndReply")
		@SendTo("sendTo")
		public String receiveAndReply(String value) {
			this.lastReceivedMessage = value;
			return value.toUpperCase();
		}

		private String getLastReceivedMessage() {
			return this.lastReceivedMessage;
		}
	}

	private static class IncomingMessageHandlerWithMultipleQueueNames {

		private String lastReceivedMessage;

		public String getLastReceivedMessage() {
			return this.lastReceivedMessage;
		}

		@RuntimeUse
		@MessageMapping({"queueOne", "queueTwo"})
		public void receive(String value) {
			this.lastReceivedMessage = value;
		}
	}

	public static class DummyKeyValueHolder {

		private final String key;
		private final String value;

		public DummyKeyValueHolder(@JsonProperty("key") String key, @JsonProperty("value") String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return this.key;
		}

		public String getValue() {
			return this.value;
		}
	}

	private static class IncomingMessageHandlerWithCustomParameter {

		private DummyKeyValueHolder lastReceivedMessage;

		public DummyKeyValueHolder getLastReceivedMessage() {
			return this.lastReceivedMessage;
		}

		@RuntimeUse
		@MessageMapping("testQueue")
		public void receive(DummyKeyValueHolder value) {
			this.lastReceivedMessage = value;
		}
	}

	private static class MessageReceiverWithHeaderAnnotation {

		private String senderId;
		private String payload;

		public String getSenderId() {
			return this.senderId;
		}

		public String getPayload() {
			return this.payload;
		}

		@RuntimeUse
		@MessageMapping("testQueue")
		public void receive(@Payload String payload, @Header(value = "SenderId", required = false) String senderId) {
			this.senderId = senderId;
			this.payload = payload;
		}

	}

	private static class MessageReceiverWithHeadersAnnotation {

		private String payload;
		private Map<String, String> headers;

		@RuntimeUse
		public String getPayload() {
			return this.payload;
		}

		public Map<String, String> getHeaders() {
			return this.headers;
		}

		@RuntimeUse
		@MessageMapping("testQueue")
		public void receive(@Payload String payload, @Headers Map<String, String> headers) {
			this.payload = payload;
			this.headers = headers;
		}

	}

	private static class NotificationMessageReceiver {

		private String subject;
		private String message;

		@RuntimeUse
		@MessageMapping("testQueue")
		public void receive(@NotificationSubject String subject, @NotificationMessage String message) {
			this.subject = subject;
			this.message = message;
		}

		public String getSubject() {
			return this.subject;
		}

		public String getMessage() {
			return this.message;
		}
	}
}
