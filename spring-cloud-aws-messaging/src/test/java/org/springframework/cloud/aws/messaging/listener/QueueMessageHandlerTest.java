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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationMessage;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationSubject;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.MapPropertySource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class QueueMessageHandlerTest {

	@Mock
	private DestinationResolvingMessageSendingOperations<?> messageTemplate;

	@Before
	public void setUp() {
		// noinspection RedundantArrayCreation to avoid unchecked generic array creation
		// for varargs parameter with Java 8.
		reset(new DestinationResolvingMessageSendingOperations<?>[] {
				this.messageTemplate });
	}

	@Test
	public void receiveMessage_methodAnnotatedWithSqsListenerAnnotation_methodInvokedForIncomingMessage() {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler",
				IncomingMessageHandler.class);
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class);
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		messageHandler.handleMessage(MessageBuilder.withPayload("testContent")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "receive").build());

		IncomingMessageHandler messageListener = applicationContext
				.getBean(IncomingMessageHandler.class);
		assertThat(messageListener.getLastReceivedMessage()).isEqualTo("testContent");
	}

	@Test
	public void receiveMessage_methodWithCustomObjectAsParameter_parameterIsConverted() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				QueueMessageHandlerWithJacksonConfiguration.class);

		DummyKeyValueHolder messagePayload = new DummyKeyValueHolder("myKey", "A value");
		MappingJackson2MessageConverter jsonMapper = applicationContext
				.getBean(MappingJackson2MessageConverter.class);
		Message<?> message = jsonMapper.toMessage(messagePayload,
				new MessageHeaders(Collections.singletonMap(
						QueueMessageHandler.LOGICAL_RESOURCE_ID, "testQueue")));

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		messageHandler.handleMessage(message);

		IncomingMessageHandlerWithCustomParameter messageListener = applicationContext
				.getBean(IncomingMessageHandlerWithCustomParameter.class);
		assertThat(messageListener.getLastReceivedMessage()).isNotNull();
		assertThat(messageListener.getLastReceivedMessage().getKey()).isEqualTo("myKey");
		assertThat(messageListener.getLastReceivedMessage().getValue())
				.isEqualTo("A value");
	}

	// @checkstyle:off
	@Test
	public void receiveAndReplyMessage_methodAnnotatedWithSqsListenerAnnotation_methodInvokedForIncomingMessageAndReplySentBackToSendToDestination() {
		// @checkstyle:on
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler",
				IncomingMessageHandler.class);
		applicationContext.registerBeanDefinition("queueMessageHandler",
				getQueueMessageHandlerBeanDefinition());
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		messageHandler.handleMessage(MessageBuilder.withPayload("testContent")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "receiveAndReply")
				.build());

		IncomingMessageHandler messageListener = applicationContext
				.getBean(IncomingMessageHandler.class);
		assertThat(messageListener.getLastReceivedMessage()).isEqualTo("testContent");
		verify(this.messageTemplate).convertAndSend(eq("sendTo"), eq("TESTCONTENT"));
	}

	private AbstractBeanDefinition getQueueMessageHandlerBeanDefinition() {
		BeanDefinitionBuilder queueMessageHandlerBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(QueueMessageHandler.class);
		ManagedList<HandlerMethodReturnValueHandler> returnValueHandlers = new ManagedList<>(
				1);
		returnValueHandlers
				.add(new SendToHandlerMethodReturnValueHandler(this.messageTemplate));
		queueMessageHandlerBeanDefinitionBuilder.addPropertyValue("returnValueHandlers",
				returnValueHandlers);
		return queueMessageHandlerBeanDefinitionBuilder.getBeanDefinition();
	}

	@Test
	public void receiveAndReplayMessage_withExceptionThrownInSendTo_shouldCallExceptionHandler() {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler",
				IncomingMessageHandler.class);
		applicationContext.registerBeanDefinition("queueMessageHandler",
				getQueueMessageHandlerBeanDefinition());
		applicationContext.refresh();

		MessageHandler messageHandler = applicationContext.getBean(MessageHandler.class);
		doThrow(new RuntimeException()).when(this.messageTemplate)
				.convertAndSend(anyString(), Optional.ofNullable(any()));
		IncomingMessageHandler messageListener = applicationContext
				.getBean(IncomingMessageHandler.class);
		messageListener.setExceptionHandlerCalled(false);

		// Act
		try {
			messageHandler.handleMessage(MessageBuilder.withPayload("testContent")
					.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "receiveAndReply")
					.build());
		}
		catch (MessagingException e) {
			// ignore
		}

		// Assert
		assertThat(messageListener.isExceptionHandlerCalled()).isTrue();
	}

	@Test
	public void receiveMessage_methodAnnotatedWithSqsListenerContainingMultipleQueueNames_methodInvokedForEachQueueName() {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton(
				"incomingMessageHandlerWithMultipleQueueNames",
				IncomingMessageHandlerWithMultipleQueueNames.class);
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);
		IncomingMessageHandlerWithMultipleQueueNames incomingMessageHandler = applicationContext
				.getBean(IncomingMessageHandlerWithMultipleQueueNames.class);

		queueMessageHandler.handleMessage(MessageBuilder
				.withPayload("Hello from queue one!")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "queueOne").build());
		assertThat(incomingMessageHandler.getLastReceivedMessage())
				.isEqualTo("Hello from queue one!");

		queueMessageHandler.handleMessage(MessageBuilder
				.withPayload("Hello from queue two!")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "queueTwo").build());
		assertThat(incomingMessageHandler.getLastReceivedMessage())
				.isEqualTo("Hello from queue two!");
	}

	@Test
	public void receiveMessage_methodAnnotatedWithSqsListenerContainingExpression_methodInvokedOnResolvedExpression() {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("test",
						Collections.singletonMap("myQueue", "resolvedQueue")));
		applicationContext.registerSingleton(
				"incomingMessageHandlerWithMultipleQueueNames",
				IncomingMessageHandlerWithExpressionName.class);
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder
				.withPayload("Hello from resolved queue!")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "resolvedQueue")
				.build());

		// Assert
		IncomingMessageHandlerWithExpressionName incomingMessageHandler = applicationContext
				.getBean(IncomingMessageHandlerWithExpressionName.class);
		assertThat(incomingMessageHandler.getLastReceivedMessage())
				.isEqualTo("Hello from resolved queue!");
	}

	@Test
	public void receiveMessage_methodAnnotatedWithSqsListenerContainingPlaceholder_methodInvokedOnResolvedPlaceholder() {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.getEnvironment().getPropertySources()
				.addLast(new MapPropertySource("test",
						Collections.singletonMap("custom.queueName", "resolvedQueue")));

		applicationContext.registerSingleton("ppc",
				PropertySourcesPlaceholderConfigurer.class);
		applicationContext.registerSingleton(
				"incomingMessageHandlerWithMultipleQueueNames",
				IncomingMessageHandlerWithPlaceholderName.class);
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder
				.withPayload("Hello from resolved queue!")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "resolvedQueue")
				.build());

		// Assert
		IncomingMessageHandlerWithPlaceholderName incomingMessageHandler = applicationContext
				.getBean(IncomingMessageHandlerWithPlaceholderName.class);
		assertThat(incomingMessageHandler.getLastReceivedMessage())
				.isEqualTo("Hello from resolved queue!");
	}

	@Test
	public void receiveMessage_withHeaderAnnotationAsArgument_shouldReceiveRequestedHeader() {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("messageHandlerWithHeaderAnnotation",
				MessageReceiverWithHeaderAnnotation.class);
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);
		MessageReceiverWithHeaderAnnotation messageReceiver = applicationContext
				.getBean(MessageReceiverWithHeaderAnnotation.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder
				.withPayload("Hello from a sender").setHeader("SenderId", "elsUnitTest")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "testQueue").build());

		// Assert
		assertThat(messageReceiver.getPayload()).isEqualTo("Hello from a sender");
		assertThat(messageReceiver.getSenderId()).isEqualTo("elsUnitTest");
	}

	@Test
	public void receiveMessage_withWrongHeaderAnnotationValueAsArgument_shouldReceiveNullAsHeaderValue() {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("messageHandlerWithHeaderAnnotation",
				MessageReceiverWithHeaderAnnotation.class);
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);
		MessageReceiverWithHeaderAnnotation messageReceiver = applicationContext
				.getBean(MessageReceiverWithHeaderAnnotation.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder
				.withPayload("Hello from a sender")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "testQueue").build());

		// Assert
		assertThat(messageReceiver.getPayload()).isEqualTo("Hello from a sender");
		assertThat(messageReceiver.getSenderId()).isNull();
	}

	@Test
	public void receiveMessage_withHeadersAsArgumentAnnotation_shouldReceiveAllHeaders() {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("messageHandlerWithHeadersAnnotation",
				MessageReceiverWithHeadersAnnotation.class);
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);
		MessageReceiverWithHeadersAnnotation messageReceiver = applicationContext
				.getBean(MessageReceiverWithHeadersAnnotation.class);

		// Act
		queueMessageHandler
				.handleMessage(MessageBuilder.withPayload("Hello from a sender")
						.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "testQueue")
						.setHeader("SenderId", "ID").build());

		// Assert
		assertThat(messageReceiver.getHeaders()).isNotNull();
		assertThat(messageReceiver.getHeaders().get("SenderId")).isEqualTo("ID");
		assertThat(
				messageReceiver.getHeaders().get(QueueMessageHandler.LOGICAL_RESOURCE_ID))
						.isEqualTo("testQueue");
	}

	@Test
	public void receiveMessage_withCustomArgumentResolvers_shouldCallThemBeforeTheDefaultOnes()
			throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler",
				IncomingMessageHandler.class);

		HandlerMethodArgumentResolver handlerMethodArgumentResolver = mock(
				HandlerMethodArgumentResolver.class);
		when(handlerMethodArgumentResolver.supportsParameter(any(MethodParameter.class)))
				.thenReturn(true);
		when(handlerMethodArgumentResolver.resolveArgument(any(MethodParameter.class),
				any(Message.class))).thenReturn("Hello from a sender");
		MutablePropertyValues properties = new MutablePropertyValues(
				Collections.singletonList(new PropertyValue("customArgumentResolvers",
						handlerMethodArgumentResolver)));
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class, properties);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder
				.withPayload("Hello from a sender")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "receive").build());

		// Assert
		verify(handlerMethodArgumentResolver, times(1))
				.resolveArgument(any(MethodParameter.class), any(Message.class));
	}

	@Test
	public void receiveMessage_withCustomReturnValueHandlers_shouldCallThemBeforeTheDefaultOnes()
			throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("incomingMessageHandler",
				IncomingMessageHandler.class);

		HandlerMethodReturnValueHandler handlerMethodReturnValueHandler = mock(
				HandlerMethodReturnValueHandler.class);
		when(handlerMethodReturnValueHandler
				.supportsReturnType(any(MethodParameter.class))).thenReturn(true);
		MutablePropertyValues properties = new MutablePropertyValues(
				Collections.singletonList(new PropertyValue("customReturnValueHandlers",
						handlerMethodReturnValueHandler)));
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class, properties);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);

		// Act
		queueMessageHandler.handleMessage(MessageBuilder
				.withPayload("Hello from a sender")
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "receiveAndReply")
				.build());

		// Assert
		verify(handlerMethodReturnValueHandler, times(1)).handleReturnValue(
				any(Object.class), any(MethodParameter.class), any(Message.class));

	}

	@Test
	public void receiveMessage_withNotificationMessageAndSubject_shouldResolveThem() {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("notificationMessageReceiver",
				NotificationMessageReceiver.class);
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class);
		applicationContext.refresh();

		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);
		NotificationMessageReceiver notificationMessageReceiver = applicationContext
				.getBean(NotificationMessageReceiver.class);

		ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();
		jsonObject.put("Type", "Notification");
		jsonObject.put("Subject", "Hi!");
		jsonObject.put("Message", "Hello World!");
		String payload = jsonObject.toString();

		// Act
		queueMessageHandler.handleMessage(MessageBuilder.withPayload(payload)
				.setHeader(QueueMessageHandler.LOGICAL_RESOURCE_ID, "testQueue").build());

		// Assert
		assertThat(notificationMessageReceiver.getSubject()).isEqualTo("Hi!");
		assertThat(notificationMessageReceiver.getMessage()).isEqualTo("Hello World!");
	}

	@Test
	public void getMappingForMethod_methodWithEmptySqsListenerValue_shouldReturnNull()
			throws Exception {
		// Arrange
		QueueMessageHandler queueMessageHandler = new QueueMessageHandler();
		Method receiveMethod = SqsListenerAnnotationWithEmptyValue.class
				.getMethod("receive");

		// Act
		QueueMessageHandler.MappingInformation mappingInformation = queueMessageHandler
				.getMappingForMethod(receiveMethod, null);

		// Assert
		assertThat(mappingInformation).isNull();
	}

	@Test
	public void getMappingForMethod_methodWithMessageMappingAnnotation_shouldReturnMappingInformation()
			throws Exception {
		// Arrange
		QueueMessageHandler queueMessageHandler = new QueueMessageHandler();
		Method receiveMethod = MessageMappingAnnotationStillSupported.class
				.getMethod("receive", String.class);

		// Act
		QueueMessageHandler.MappingInformation mappingInformation = queueMessageHandler
				.getMappingForMethod(receiveMethod, null);

		// Assert
		assertThat(mappingInformation.getLogicalResourceIds().contains("testQueue"))
				.isTrue();
		assertThat(mappingInformation.getDeletionPolicy())
				.isEqualTo(SqsMessageDeletionPolicy.NO_REDRIVE);
	}

	@Test
	public void getMappingForMethod_methodWithDeletionPolicyNeverWithoutParameterTypeAcknowledgment_warningMustBeLogged()
			throws Exception {
		// Arrange
		QueueMessageHandler queueMessageHandler = new QueueMessageHandler();
		Method receiveMethod = SqsListenerDeletionPolicyNeverNoAcknowledgment.class
				.getMethod("receive", String.class);

		LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();

		Logger log = logContext.getLogger(QueueMessageHandler.class);
		log.setLevel(Level.WARN);
		log.addAppender(appender);
		appender.setContext(log.getLoggerContext());

		// Act
		queueMessageHandler.getMappingForMethod(receiveMethod, null);

		// Assert
		ILoggingEvent loggingEvent = appender.list.get(0);

		assertThat(loggingEvent.getLevel()).isSameAs(Level.WARN);
		assertThat(loggingEvent.getMessage().contains("receive")).isTrue();
		assertThat(loggingEvent.getMessage().contains(
				"org.springframework.cloud.aws.messaging.listener.QueueMessageHandlerTest$SqsListener"
						+ "DeletionPolicyNeverNoAcknowledgment")).isTrue();
	}

	// @checkstyle:off
	@Test
	public void getMappingForMethod_methodWithExpressionProducingMultipleQueueNames_shouldMapMethodForEveryQueueNameReturnedByExpression()
			throws Exception {
		// @checkstyle:on
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("queueMessageHandler",
				QueueMessageHandler.class);
		applicationContext.refresh();

		Method receiveMethod = SqsListenerWithExpressionProducingMultipleQueueNames.class
				.getMethod("receive", String.class);
		QueueMessageHandler queueMessageHandler = applicationContext
				.getBean(QueueMessageHandler.class);

		// Act
		QueueMessageHandler.MappingInformation mappingInformation = queueMessageHandler
				.getMappingForMethod(receiveMethod, null);

		// Assert

		assertThat(mappingInformation.getLogicalResourceIds().size()).isEqualTo(2);
		assertThat(mappingInformation.getLogicalResourceIds()
				.containsAll(Arrays.asList("queueOne", "queueTwo"))).isTrue();
	}

	@SuppressWarnings("UnusedDeclaration")
	private static class IncomingMessageHandler {

		private String lastReceivedMessage;

		private boolean exceptionHandlerCalled;

		public boolean isExceptionHandlerCalled() {
			return this.exceptionHandlerCalled;
		}

		public void setExceptionHandlerCalled(boolean exceptionHandlerCalled) {
			this.exceptionHandlerCalled = exceptionHandlerCalled;
		}

		@SqsListener("receive")
		public void receive(@Payload String value) {
			this.lastReceivedMessage = value;
		}

		@SqsListener("receiveAndReply")
		@SendTo("sendTo")
		public String receiveAndReply(String value) {
			this.lastReceivedMessage = value;
			return value.toUpperCase();
		}

		@MessageExceptionHandler(RuntimeException.class)
		public void handleException() {
			this.exceptionHandlerCalled = true;
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
		@SqsListener({ "queueOne", "queueTwo" })
		public void receive(String value) {
			this.lastReceivedMessage = value;
		}

	}

	private static class IncomingMessageHandlerWithExpressionName {

		private String lastReceivedMessage;

		public String getLastReceivedMessage() {
			return this.lastReceivedMessage;
		}

		@RuntimeUse
		@SqsListener("#{environment.myQueue}")
		public void receive(String value) {
			this.lastReceivedMessage = value;
		}

	}

	private static class IncomingMessageHandlerWithPlaceholderName {

		private String lastReceivedMessage;

		public String getLastReceivedMessage() {
			return this.lastReceivedMessage;
		}

		@RuntimeUse
		@SqsListener("${custom.queueName}")
		public void receive(String value) {
			this.lastReceivedMessage = value;
		}

	}

	public static class DummyKeyValueHolder {

		private final String key;

		private final String value;

		public DummyKeyValueHolder(@JsonProperty("key") String key,
				@JsonProperty("value") String value) {
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
		@SqsListener("testQueue")
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
		@SqsListener("testQueue")
		public void receive(@Payload String payload,
				@Header(value = "SenderId", required = false) String senderId) {
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
		@SqsListener("testQueue")
		public void receive(@Payload String payload,
				@Headers Map<String, String> headers) {
			this.payload = payload;
			this.headers = headers;
		}

	}

	private static class NotificationMessageReceiver {

		private String subject;

		private String message;

		@RuntimeUse
		@SqsListener("testQueue")
		public void receive(@NotificationSubject String subject,
				@NotificationMessage String message) {
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

	private static class SqsListenerAnnotationWithEmptyValue {

		@RuntimeUse
		@SqsListener
		public void receive() {

		}

	}

	private static class MessageMappingAnnotationStillSupported {

		@RuntimeUse
		@SqsListener("testQueue")
		public void receive(String message) {
		}

	}

	private static class SqsListenerDeletionPolicyNeverNoAcknowledgment {

		@RuntimeUse
		@SqsListener(value = "testQueue", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
		public void receive(String message) {
		}

	}

	private static class SqsListenerWithExpressionProducingMultipleQueueNames {

		@RuntimeUse
		@SqsListener("#{'queueOne,queueTwo'.split(',')}")
		public void receive(String message) {
		}

	}

	@TestConfiguration
	static class QueueMessageHandlerWithJacksonConfiguration {

		@Bean
		QueueMessageHandler queueMessageHandler() {
			return new QueueMessageHandler(
					Arrays.asList(mappingJackson2MessageConverter()));
		}

		@Bean
		IncomingMessageHandlerWithCustomParameter incomingMessageHandlerWithCustomParameter() {
			return new IncomingMessageHandlerWithCustomParameter();
		}

		@Bean
		MappingJackson2MessageConverter mappingJackson2MessageConverter() {
			return new MappingJackson2MessageConverter();
		}

	}

}
