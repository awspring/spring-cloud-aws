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

package io.awspring.cloud.v3.messaging.listener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.awspring.cloud.v3.core.support.documentation.RuntimeUse;
import io.awspring.cloud.v3.messaging.core.MessageAttributeDataTypes;
import io.awspring.cloud.v3.messaging.listener.annotation.SqsListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MimeType;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.OverLimitException;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Mete Alpaslan Katırcıoğlu
 * @since 1.0
 */
class SimpleMessageListenerContainerTest {

	@Captor
	private ArgumentCaptor<org.springframework.messaging.Message<String>> stringMessageCaptor;

	private static Level disableLogging() {
		LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger logger = logContext.getLogger(SimpleMessageListenerContainer.class);
		Level previous = logger.getLevel();
		logger.setLevel(Level.OFF);
		return previous;
	}

	private static void setLogLevel(Level level) {
		LoggerContext logContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		logContext.getLogger(SimpleMessageListenerContainer.class).setLevel(level);
	}

	private static void mockReceiveMessage(SqsClient sqs, String queueUrl, String messageContent,
										   String receiptHandle) {
		when(sqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl(queueUrl)
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.maxNumberOfMessages(10)
				.waitTimeSeconds(20)
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Message.builder()
					.body(messageContent)
					.receiptHandle(receiptHandle)
					.build())
				.build(),
				ReceiveMessageResponse.builder().build());
	}

	private static void mockGetQueueAttributesWithRedrivePolicy(SqsClient sqs, String queueUrl) {
		when(sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
				.queueUrl(queueUrl)
				.attributeNames(QueueAttributeName.REDRIVE_POLICY)
			.build()))
			.thenReturn(GetQueueAttributesResponse.builder()
				.attributes(Collections.singletonMap(QueueAttributeName.REDRIVE_POLICY, "{\"some\": \"JSON\"}"))
				.build());
	}

	private static void mockGetQueueAttributesWithEmptyResult(SqsClient sqs, String queueUrl) {
		when(sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
				.queueUrl(queueUrl)
				.attributeNames(QueueAttributeName.REDRIVE_POLICY)
			.build()))
			.thenReturn(GetQueueAttributesResponse.builder().build());
	}

	private static void mockGetQueueUrl(SqsClient sqs, String queueName, String queueUrl) {
		when(sqs.getQueueUrl(GetQueueUrlRequest.builder()
				.queueName(queueName)
			.build()))
			.thenReturn(GetQueueUrlResponse.builder().queueUrl(queueName).build());
	}

	private static Message fifoMessage(final String messageGroupId, final String content) {
		Map<String, MessageAttributeValue> headers = new HashMap<>();
		headers.put(MessageSystemAttributeName.MESSAGE_GROUP_ID.toString(), MessageAttributeValue.builder()
				.dataType(MessageAttributeDataTypes.STRING)
				.stringValue(messageGroupId)
			.build());
		return Message.builder()
			.body(content)
			.messageAttributes(headers)
			.build();
	}

	@BeforeEach
	void setUp() {
		initMocks(this);
	}

	@Test
	void testWithDefaultTaskExecutorAndNoBeanName() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		container.setAmazonSqs(mock(SqsClient.class, withSettings().stubOnly()));
		container.setMessageHandler(mock(QueueMessageHandler.class));

		container.afterPropertiesSet();

		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) container.getTaskExecutor();
		assertThat(taskExecutor).isNotNull();
		assertThat(taskExecutor.getThreadNamePrefix())
				.isEqualTo(SimpleMessageListenerContainer.class.getSimpleName() + "-");
	}

	@Test
	void testWithDefaultTaskExecutorAndBeanName() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		container.setAmazonSqs(mock(SqsClient.class, withSettings().stubOnly()));
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.setBeanName("testContainerName");
		container.afterPropertiesSet();

		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) container.getTaskExecutor();
		assertThat(taskExecutor).isNotNull();
		assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("testContainerName-");
	}

	@Test
	void testWithDefaultTaskExecutorAndOneHandler() throws Exception {
		int testedMaxNumberOfMessages = 10;

		Map<QueueMessageHandler.MappingInformation, HandlerMethod> messageHandlerMethods = Collections
				.singletonMap(new QueueMessageHandler.MappingInformation(Collections.singleton("testQueue"),
						SqsMessageDeletionPolicy.ALWAYS), null);

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		QueueMessageHandler mockedHandler = mock(QueueMessageHandler.class);
		SqsClient mockedSqs = mock(SqsClient.class, withSettings().stubOnly());

		when(mockedSqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
				.thenReturn(GetQueueAttributesResponse.builder().build());
		when(mockedSqs.getQueueUrl(any(GetQueueUrlRequest.class)))
				.thenReturn(GetQueueUrlResponse.builder().queueUrl("testQueueUrl").build());
		when(mockedHandler.getHandlerMethods()).thenReturn(messageHandlerMethods);

		container.setMaxNumberOfMessages(testedMaxNumberOfMessages);
		container.setAmazonSqs(mockedSqs);
		container.setMessageHandler(mockedHandler);

		container.afterPropertiesSet();

		int expectedPoolMaxSize = messageHandlerMethods.size() * (testedMaxNumberOfMessages + 1);

		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) container.getTaskExecutor();
		assertThat(taskExecutor).isNotNull();
		assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(expectedPoolMaxSize);
	}

	@Test
	void testCustomTaskExecutor() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		container.setAmazonSqs(mock(SqsClient.class, withSettings().stubOnly()));
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.setBeanName("testContainerName");
		container.afterPropertiesSet();

		assertThat(container.getTaskExecutor()).isEqualTo(taskExecutor);
	}

	@Test
	void testSimpleReceiveMessage() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);

		CountDownLatch countDownLatch = new CountDownLatch(1);
		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				countDownLatch.countDown();
				assertThat(message.getPayload()).isEqualTo("messageContent");
			}
		};
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		messageHandler.setApplicationContext(applicationContext);
		container.setBeanName("testContainerName");
		messageHandler.afterPropertiesSet();

		mockGetQueueUrl(sqs, "testQueue", "http://testSimpleReceiveMessage.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testSimpleReceiveMessage.amazonaws.com");

		container.afterPropertiesSet();

		when(sqs.receiveMessage(ReceiveMessageRequest.builder()
				.queueUrl("http://testSimpleReceiveMessage.amazonaws.com")
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.maxNumberOfMessages(10)
				.waitTimeSeconds(20)
			.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Message.builder()
							.body("messageContent")
							.build(),
						Message.builder()
							.body("messageContent")
							.build())
				.build(), ReceiveMessageResponse.builder().build()
			);
		when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
			.thenReturn(GetQueueAttributesResponse.builder().build());

		container.start();

		assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();

		container.stop();
	}

	@Test
	void testReceiveMessagesFromFifoQueue() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);

		CountDownLatch countDownLatch = new CountDownLatch(10);
		List<String> actualHandledMessages = new ArrayList<>();
		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				assertThat(message.getPayload()).isInstanceOf(String.class);
				actualHandledMessages.add((String) message.getPayload());
				countDownLatch.countDown();
			}
		};
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("fifoTestMessageListener", FifoTestMessageListener.class);
		messageHandler.setApplicationContext(applicationContext);
		container.setBeanName("testContainerName");
		messageHandler.afterPropertiesSet();

		mockGetQueueUrl(sqs, "testQueue.fifo", "http://testSimpleReceiveMessage.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testSimpleReceiveMessage.amazonaws.com");

		container.afterPropertiesSet();

		final Message group1Msg1 = fifoMessage("1", "group1Msg1");
		final Message group1Msg2 = fifoMessage("1", "group1Msg2");
		final Message group1Msg3 = fifoMessage("1", "group1Msg3");
		final Message group1Msg4 = fifoMessage("1", "group1Msg4");
		final Message group1Msg5 = fifoMessage("1", "group1Msg5");
		final Message group1Msg6 = fifoMessage("1", "group1Msg6");
		final Message group1Msg7 = fifoMessage("1", "group1Msg7");
		final Message group2Msg1 = fifoMessage("2", "group2Msg1");
		final Message group2Msg2 = fifoMessage("2", "group2Msg2");
		final Message group3Msg1 = fifoMessage("3", "group3Msg1");

		when(sqs.receiveMessage(
			ReceiveMessageRequest.builder()
				    .queueUrl("http://testSimpleReceiveMessage.amazonaws.com")
					.attributeNames(QueueAttributeName.ALL)
					.messageAttributeNames("All")
					.maxNumberOfMessages(10)
					.waitTimeSeconds(20)
			.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(group1Msg1,group1Msg2, group1Msg3,
					group1Msg4, group1Msg5, group1Msg6, group1Msg7, group2Msg1, group2Msg2,
					group3Msg1)
				.build())
			.thenReturn(ReceiveMessageResponse.builder().build());
		when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
			.thenReturn(GetQueueAttributesResponse.builder().build());

		container.start();

		assertThat(countDownLatch.await(3, TimeUnit.SECONDS)).isTrue();

		final List<String> actualGroup1Messages = actualHandledMessages.stream().filter(msg -> msg.startsWith("group1"))
				.collect(Collectors.toList());
		final List<String> actualGroup2Messages = actualHandledMessages.stream().filter(msg -> msg.startsWith("group2"))
				.collect(Collectors.toList());
		final List<String> actualGroup3Messages = actualHandledMessages.stream().filter(msg -> msg.startsWith("group3"))
				.collect(Collectors.toList());

		assertThat(actualGroup1Messages).containsExactly("group1Msg1", "group1Msg2", "group1Msg3", "group1Msg4",
				"group1Msg5", "group1Msg6", "group1Msg7");
		assertThat(actualGroup2Messages).containsExactly("group2Msg1", "group2Msg2");
		assertThat(actualGroup3Messages).containsExactly("group3Msg1");

		container.stop();
	}

	@Test
	void testContainerDoesNotProcessMessageAfterBeingStopped() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				fail("Should not have been called");
			}
		};
		container.setMessageHandler(messageHandler);
		container.setBeanName("testContainerName");

		mockGetQueueUrl(sqs, "testQueue", "http://testContainerDoesNotProcessMessageAfterBeingStopped.amazonaws.com");

		container.afterPropertiesSet();

		when(sqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("http://testContainerDoesNotProcessMessageAfterBeingStopped.amazonaws.com")
				.build()))
						.thenAnswer((Answer<ReceiveMessageResponse>) invocation -> {
							container.stop();
							return ReceiveMessageResponse.builder()
								.messages(Message.builder().body("test").build())
								.build();
						});

		container.start();
	}

	@Test
	void listener_withMultipleMessageHandlers_shouldBeCalled() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(2);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};
		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		applicationContext.registerSingleton("anotherTestMessageListener", AnotherTestMessageListener.class);

		mockGetQueueUrl(sqs, "testQueue", "https://listener_withMultipleMessageHandlers_shouldBeCalled.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://listener_withMultipleMessageHandlers_shouldBeCalled.amazonaws.com");
		mockGetQueueUrl(sqs, "anotherTestQueue",
				"https://listener_withMultipleMessageHandlers_shouldBeCalled.another.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://listener_withMultipleMessageHandlers_shouldBeCalled.another.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(sqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("https://listener_withMultipleMessageHandlers_shouldBeCalled.amazonaws.com")
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.maxNumberOfMessages(10)
				.waitTimeSeconds(20)
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Message.builder().body("messageContent").build())
				.build(),
				ReceiveMessageResponse.builder().build());

		when(sqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("https://listener_withMultipleMessageHandlers_shouldBeCalled.another.amazonaws.com")
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.maxNumberOfMessages(10)
				.waitTimeSeconds(20)
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Message.builder().body("anotherMessageContent").build())
				.build())
			.thenReturn(ReceiveMessageResponse.builder().build());

		when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
			.thenReturn(GetQueueAttributesResponse.builder().build());

		container.start();

		assertThat(countDownLatch.await(2L, TimeUnit.SECONDS)).isTrue();
		container.stop();
		assertThat(applicationContext.getBean(TestMessageListener.class).getMessage()).isEqualTo("messageContent");
		assertThat(applicationContext.getBean(AnotherTestMessageListener.class).getMessage())
				.isEqualTo("anotherMessageContent");
	}

	@Test
	void messageExecutor_withMessageWithAttributes_shouldPassThemAsHeaders() throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = spy(new QueueMessageHandler());
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);

		mockGetQueueUrl(sqs, "testQueue",
				"https://messageExecutor_withMessageWithAttributes_shouldPassThemAsHeaders.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://messageExecutor_withMessageWithAttributes_shouldPassThemAsHeaders.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(sqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("https://messageExecutor_withMessageWithAttributes_shouldPassThemAsHeaders.amazonaws.com")
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.maxNumberOfMessages(10)
				.waitTimeSeconds(20)
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Message.builder()
							.body("messageContent")
							.attributesWithStrings(new HashMap<String, String>() {{
								put("SenderId", "ID");
								put("SentTimestamp", "1000");
								put("ApproximateFirstReceiveTimestamp", "2000");
							}})
					.build()
				)
				.build())
			.thenReturn(ReceiveMessageResponse.builder().build());
		when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
			.thenReturn(GetQueueAttributesResponse.builder().build());

		// Act
		container.start();

		// Assert
		assertThat(countDownLatch.await(2L, TimeUnit.SECONDS)).isTrue();
		container.stop();

		verify(messageHandler).handleMessage(this.stringMessageCaptor.capture());
		assertThat(this.stringMessageCaptor.getValue().getHeaders().get("SenderId")).isEqualTo("ID");
		assertThat(this.stringMessageCaptor.getValue().getHeaders().getTimestamp()).isEqualTo(1000L);
		assertThat(this.stringMessageCaptor.getValue().getHeaders().get("ApproximateFirstReceiveTimestamp"))
				.isEqualTo("2000");

	}

	@Test
	void doDestroy_whenContainerCallsDestroy_DestroysDefaultTaskExecutor() throws Exception {
		// Arrange
		SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		simpleMessageListenerContainer.setAmazonSqs(sqs);
		QueueMessageHandler messageHandler = mock(QueueMessageHandler.class);
		simpleMessageListenerContainer.setMessageHandler(messageHandler);
		simpleMessageListenerContainer.afterPropertiesSet();
		simpleMessageListenerContainer.start();

		// Act
		simpleMessageListenerContainer.destroy();

		// Assert
		assertThat(((ThreadPoolTaskExecutor) simpleMessageListenerContainer.getTaskExecutor()).getThreadPoolExecutor()
				.isTerminated()).isTrue();
	}

	@Test
	void afterPropertiesSet_whenCalled_taskExecutorIsActive() throws Exception {
		// Arrange
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);
		QueueMessageHandler messageHandler = mock(QueueMessageHandler.class);
		container.setMessageHandler(messageHandler);

		// Act
		container.afterPropertiesSet();

		// Assert
		assertThat(((ThreadPoolTaskExecutor) container.getTaskExecutor()).getThreadPoolExecutor().isTerminated())
				.isFalse();
		container.stop();
	}

	@Test
	void messageExecutor_messageWithMimeTypeMessageAttribute_shouldSetItAsHeader() throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = spy(new QueueMessageHandler());
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);

		mockGetQueueUrl(sqs, "testQueue",
				"https://messageExecutor_messageWithMimeTypeMessageAttribute_shouldSetItAsHeader.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://messageExecutor_messageWithMimeTypeMessageAttribute_shouldSetItAsHeader.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		MimeType mimeType = new MimeType("text", "plain", StandardCharsets.UTF_8);
		when(sqs.receiveMessage(ReceiveMessageRequest.builder()
				.queueUrl("https://messageExecutor_messageWithMimeTypeMessageAttribute_shouldSetItAsHeader.amazonaws.com")
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.maxNumberOfMessages(10)
				.waitTimeSeconds(20)
			.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Message.builder()
						.body("messageContent")
						.attributesWithStrings(Collections.singletonMap("SenderId", "ID"))
						.messageAttributes(Collections
							.singletonMap(MessageHeaders.CONTENT_TYPE, MessageAttributeValue.builder()
							.dataType("String")
							.stringValue(mimeType.toString())
							.build()))
						.build())
					.build(),
				ReceiveMessageResponse.builder().build());

		when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
			.thenReturn(GetQueueAttributesResponse.builder().build());

		// Act
		container.start();

		// Assert
		assertThat(countDownLatch.await(2L, TimeUnit.SECONDS)).isTrue();
		container.stop();

		verify(messageHandler).handleMessage(this.stringMessageCaptor.capture());
		assertThat(this.stringMessageCaptor.getValue().getHeaders().get(MessageHeaders.CONTENT_TYPE))
				.isEqualTo(mimeType);
	}

	@Test
	void queueMessageHandler_withJavaConfig_shouldScanTheListenerMethods() {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				SqsTestConfig.class, TestMessageListener.class);
		SimpleMessageListenerContainer simpleMessageListenerContainer = applicationContext
				.getBean(SimpleMessageListenerContainer.class);
		QueueMessageHandler queueMessageHandler = (QueueMessageHandler) ReflectionTestUtils
				.getField(simpleMessageListenerContainer, "messageHandler");

		// Assert
		assertThat(queueMessageHandler).isNotNull();
		assertThat(queueMessageHandler.getHandlerMethods()).hasSize(1);
	}

	@Test
	void executeMessage_successfulExecution_shouldRemoveMessageFromQueue() throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		SqsClient sqs = mock(SqsClient.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);

		mockGetQueueUrl(sqs, "testQueue",
				"https://executeMessage_successfulExecution_shouldRemoveMessageFromQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://executeMessage_successfulExecution_shouldRemoveMessageFromQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		mockReceiveMessage(sqs, "https://executeMessage_successfulExecution_shouldRemoveMessageFromQueue.amazonaws.com",
				"messageContent", "ReceiptHandle");

		// Act
		container.start();

		// Assert
		assertThat(countDownLatch.await(2L, TimeUnit.SECONDS)).isTrue();
		container.stop();
		verify(sqs, times(1))
			.deleteMessage(DeleteMessageRequest.builder()
				.queueUrl("https://executeMessage_successfulExecution_shouldRemoveMessageFromQueue.amazonaws.com")
				.receiptHandle("ReceiptHandle")
				.build());
	}

	@Test
	void executeMessage_executionThrowsExceptionAndQueueHasAllDeletionPolicy_shouldRemoveMessageFromQueue()
			throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		SqsClient sqs = mock(SqsClient.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener",
				TestMessageListenerThatThrowsAnExceptionWithAllDeletionPolicy.class);

		mockGetQueueUrl(sqs, "testQueue", "https://executeMessage_executionThrowsExceptionAnd"
				+ "QueueHasAllDeletionPolicy_shouldRemoveMessageFromQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://executeMessage_executionThrowsExceptionAnd"
				+ "QueueHasAllDeletionPolicy_shouldRemoveMessageFromQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(sqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("https://executeMessage_executionThrowsExceptionAnd"
					+ "QueueHasAllDeletionPolicy_shouldRemoveMessageFromQueue.amazonaws.com")
				.attributeNames(QueueAttributeName.ALL)
				.maxNumberOfMessages(10).waitTimeSeconds(20).messageAttributeNames("All")
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
					.messages(Message.builder()
						.body("messageContent")
						.receiptHandle("ReceiptHandle").build())
					.build(),
					ReceiveMessageResponse.builder().build());

		// Act
		container.start();

		// Assert
		assertThat(countDownLatch.await(2L, TimeUnit.SECONDS)).isTrue();
		container.stop();
		verify(sqs, times(1))
			.deleteMessage(DeleteMessageRequest.builder()
				.queueUrl("https://executeMessage_executionThrowsExceptionAnd"
					+ "QueueHasAllDeletionPolicy_shouldRemoveMessageFromQueue.amazonaws.com")
				.receiptHandle("ReceiptHandle")
				.build());
	}

	@Test
	void executeMessage_executionThrowsExceptionAndQueueHasRedrivePolicy_shouldNotRemoveMessageFromQueue()
			throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		SqsClient sqs = mock(SqsClient.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		Class clazz = TestMessageListenerThatThrowsAnExceptionWithAllExceptOnRedriveDeletionPolicy.class;
		applicationContext.registerSingleton("testMessageListener", clazz);

		mockGetQueueUrl(sqs, "testQueue", "https://executeMessage_executionThrowsExceptionAnd"
				+ "QueueHasRedrivePolicy_shouldNotRemoveMessageFromQueue.amazonaws.com");
		mockGetQueueAttributesWithRedrivePolicy(sqs, "https://executeMessage_executionThrowsExceptionAnd"
				+ "QueueHasRedrivePolicy_shouldNotRemoveMessageFromQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(sqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("https://executeMessage_executionThrowsExceptionAnd"
					+ "QueueHasRedrivePolicy_shouldNotRemoveMessageFromQueue.amazonaws.com")
				.attributeNames(QueueAttributeName.ALL)
				.maxNumberOfMessages(10)
				.waitTimeSeconds(20)
				.messageAttributeNames("All")
				.build()))
			.thenReturn(ReceiveMessageResponse.builder()
				.messages(Message.builder()
							.body("messageContent")
							.receiptHandle("ReceiptHandle")
						.build())
					.build(),
				ReceiveMessageResponse.builder().build());

		// Act
		container.start();

		// Assert
		assertThat(countDownLatch.await(2L, TimeUnit.SECONDS)).isTrue();
		container.stop();
		verify(sqs, never())
			.deleteMessage(eq(DeleteMessageRequest.builder()
				.queueUrl("https://executeMessage_executionThrowsExceptionAnd"
					+ "QueueHasRedrivePolicy_shouldNotRemoveMessageFromQueue.amazonaws.com")
				.receiptHandle("ReceiptHandle")
				.build()));
	}

	@Test
	void doStop_containerNotRunning_shouldNotThrowAnException() throws Exception {
		// Arrange
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setAmazonSqs(mock(SqsClient.class, withSettings().stubOnly()));
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.setAutoStartup(false);
		container.afterPropertiesSet();

		// Act & Assert
		container.stop();
	}

	@Test
	void receiveMessage_throwsAnException_operationShouldBeRetried() throws Exception {
		// Arrange
		Level previous = disableLogging();

		SqsClient amazonSqs = mock(SqsClient.class, withSettings().stubOnly());
		when(amazonSqs.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(new RuntimeException("Boom!"))
				.thenReturn(ReceiveMessageResponse.builder()
					.messages(
						Message.builder().body("messageContent").build(),
						Message.builder().body("messageContent").build())
					.build());

		CountDownLatch countDownLatch = new CountDownLatch(1);
		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				countDownLatch.countDown();
				assertThat(message.getPayload()).isEqualTo("messageContent");
			}
		};

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		messageHandler.setApplicationContext(applicationContext);

		mockGetQueueUrl(amazonSqs, "testQueue",
				"https://receiveMessage_throwsAnException_operationShouldBeRetried.amazonaws.com");
		messageHandler.afterPropertiesSet();

		when(amazonSqs.getQueueAttributes(any(GetQueueAttributesRequest.class)))
				.thenReturn(GetQueueAttributesResponse.builder().build());

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setBackOffTime(0);
		container.setAmazonSqs(amazonSqs);
		container.setMessageHandler(messageHandler);
		container.setAutoStartup(false);
		container.afterPropertiesSet();

		// Act
		container.start();

		// Assert
		assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
		container.stop();
		setLogLevel(previous);
	}

	@Test
	void receiveMessage_withMessageListenerMethodAndNeverDeletionPolicy_waitsForAcknowledgmentBeforeDeletion()
			throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		SqsClient sqs = mock(SqsClient.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testListener", TestMessageListenerWithManualDeletionPolicy.class);

		mockGetQueueUrl(sqs, "testQueue", "https://receiveMessage_withMessageListenerMethodAnd"
				+ "NeverDeletionPolicy_waitsForAcknowledgmentBeforeDeletion.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://receiveMessage_withMessageListenerMethodAnd"
				+ "NeverDeletionPolicy_waitsForAcknowledgmentBeforeDeletion.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		mockReceiveMessage(sqs,
				"https://receiveMessage_withMessageListenerMethodAnd"
						+ "NeverDeletionPolicy_waitsForAcknowledgmentBeforeDeletion.amazonaws.com",
				"messageContent", "ReceiptHandle");

		// Act
		container.start();

		// Assert
		countDownLatch.await(1L, TimeUnit.SECONDS);
		verify(sqs, never())
			.deleteMessage(eq(DeleteMessageRequest.builder()
				.queueUrl("https://receiveMessage_withMessageListenerMethodAnd"
					+ "NeverDeletionPolicy_waitsForAcknowledgmentBeforeDeletion.amazonaws.com")
				.receiptHandle("ReceiptHandle")
				.build()));

		TestMessageListenerWithManualDeletionPolicy testMessageListenerWithManualDeletionPolicy = applicationContext
				.getBean(TestMessageListenerWithManualDeletionPolicy.class);
		testMessageListenerWithManualDeletionPolicy.getCountDownLatch().await(1L, TimeUnit.SECONDS);
		testMessageListenerWithManualDeletionPolicy.acknowledge();
		verify(sqs, times(1))
			.deleteMessage(eq(DeleteMessageRequest.builder()
					.queueUrl("https://receiveMessage_withMessageListenerMethodAnd"
						+ "NeverDeletionPolicy_waitsForAcknowledgmentBeforeDeletion.amazonaws.com")
					.receiptHandle("ReceiptHandle")
					.build()));
		container.stop();
	}

	@Test
	void receiveMessage_withMessageListenerMethodAndVisibilityProlonging_callsChangeMessageVisibility()
			throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		SqsClient sqs = mock(SqsClient.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testListener", TestMessageListenerWithVisibilityProlong.class);

		mockGetQueueUrl(sqs, "testQueue", "https://receiveMessage_withMessageListenerMethodAnd"
				+ "VisibilityProlonging_callsChangeMessageVisibility.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://receiveMessage_withMessageListenerMethodAnd"
				+ "VisibilityProlonging_callsChangeMessageVisibility.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		mockReceiveMessage(sqs,
				"https://receiveMessage_withMessageListenerMethodAnd"
						+ "VisibilityProlonging_callsChangeMessageVisibility.amazonaws.com",
				"messageContent", "ReceiptHandle");

		// Act
		container.start();

		// Assert
		countDownLatch.await(1L, TimeUnit.SECONDS);
		verify(sqs, never())
			.changeMessageVisibility(any(ChangeMessageVisibilityRequest.class));
		TestMessageListenerWithVisibilityProlong testMessageListenerWithVisibilityProlong = applicationContext
				.getBean(TestMessageListenerWithVisibilityProlong.class);
		testMessageListenerWithVisibilityProlong.getCountDownLatch().await(1L, TimeUnit.SECONDS);
		testMessageListenerWithVisibilityProlong.extend(5);
		verify(sqs, times(1))
				.changeMessageVisibility(eq(ChangeMessageVisibilityRequest.builder()
						.queueUrl("https://receiveMessage_withMessageListenerMethodAnd"
							+ "VisibilityProlonging_callsChangeMessageVisibility.amazonaws.com")
						.receiptHandle("ReceiptHandle")
						.visibilityTimeout(5)
						.build()));
		container.stop();
	}

	@Test
	void executeMessage_withDifferentDeletionPolicies_shouldActAccordingly() throws Exception {
		// Arrange
		Level previous = disableLogging();

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SqsClient sqs = mock(SqsClient.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testListener", TestMessageListenerWithAllPossibleDeletionPolicies.class);

		mockGetQueueUrl(sqs, "alwaysSuccess", "https://alwaysSuccess.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://alwaysSuccess.amazonaws.com");
		mockGetQueueUrl(sqs, "alwaysError", "https://alwaysError.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://alwaysError.amazonaws.com");
		mockGetQueueUrl(sqs, "onSuccessSuccess", "https://onSuccessSuccess.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://onSuccessSuccess.amazonaws.com");
		mockGetQueueUrl(sqs, "onSuccessError", "https://onSuccessError.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://onSuccessError.amazonaws.com");
		mockGetQueueUrl(sqs, "noRedriveSuccess", "https://noRedriveSuccess.amazonaws.com");
		mockGetQueueAttributesWithRedrivePolicy(sqs, "https://noRedriveSuccess.amazonaws.com");
		mockGetQueueUrl(sqs, "noRedriveError", "https://noRedriveError.amazonaws.com");
		mockGetQueueAttributesWithRedrivePolicy(sqs, "https://noRedriveError.amazonaws.com");
		mockGetQueueUrl(sqs, "neverSuccess", "https://neverSuccess.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://neverSuccess.amazonaws.com");
		mockGetQueueUrl(sqs, "neverError", "https://neverError.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://neverError.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		mockReceiveMessage(sqs, "https://alwaysSuccess.amazonaws.com", "foo", "alwaysSuccess");
		mockReceiveMessage(sqs, "https://alwaysError.amazonaws.com", "foo", "alwaysError");
		mockReceiveMessage(sqs, "https://onSuccessSuccess.amazonaws.com", "foo", "onSuccessSuccess");
		mockReceiveMessage(sqs, "https://onSuccessError.amazonaws.com", "foo", "onSuccessError");
		mockReceiveMessage(sqs, "https://noRedriveSuccess.amazonaws.com", "foo", "noRedriveSuccess");
		mockReceiveMessage(sqs, "https://noRedriveError.amazonaws.com", "foo", "noRedriveError");
		mockReceiveMessage(sqs, "https://neverSuccess.amazonaws.com", "foo", "neverSuccess");
		mockReceiveMessage(sqs, "https://neverError.amazonaws.com", "foo", "neverError");

		// Act
		container.start();

		// Assert
		TestMessageListenerWithAllPossibleDeletionPolicies bean = applicationContext
				.getBean(TestMessageListenerWithAllPossibleDeletionPolicies.class);
		assertThat(bean.getCountdownLatch().await(1L, TimeUnit.SECONDS)).isTrue();
		container.stop();
		verify(sqs, times(1)).deleteMessage(
				eq(DeleteMessageRequest.builder()
					.queueUrl("https://alwaysSuccess.amazonaws.com")
					.receiptHandle("alwaysSuccess")
					.build()));
		verify(sqs, times(1)).deleteMessage(
			eq(DeleteMessageRequest.builder()
				.queueUrl("https://alwaysError.amazonaws.com")
				.receiptHandle("alwaysError")
				.build()));
		verify(sqs, times(1)).deleteMessage(
			eq(DeleteMessageRequest.builder()
				.queueUrl("https://onSuccessSuccess.amazonaws.com")
				.receiptHandle("onSuccessSuccess")
				.build()));
		verify(sqs, never()).deleteMessage(
			eq(DeleteMessageRequest.builder()
				.queueUrl("https://onSuccessError.amazonaws.com")
				.receiptHandle("onSuccessError")
				.build()));
		verify(sqs, times(1)).deleteMessage(
			eq(DeleteMessageRequest.builder()
				.queueUrl("https://noRedriveSuccess.amazonaws.com")
				.receiptHandle("noRedriveSuccess")
				.build()));
		verify(sqs, never()).deleteMessage(
			eq(DeleteMessageRequest.builder()
				.queueUrl("https://noRedriveError.amazonaws.com")
				.receiptHandle("noRedriveError")
				.build()));
		verify(sqs, never()).deleteMessage(
			eq(DeleteMessageRequest.builder()
				.queueUrl("https://neverSuccess.amazonaws.com")
				.receiptHandle("neverSuccess")
				.build()));
		verify(sqs, never()).deleteMessage(
			eq(DeleteMessageRequest.builder()
				.queueUrl("https://neverError.amazonaws.com")
				.receiptHandle("neverError")
				.build()));

		setLogLevel(previous);
	}

	@Test
	void stop_withALogicalQueueName_mustStopOnlyTheSpecifiedQueue() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		applicationContext.registerSingleton("anotherTestMessageListener", AnotherTestMessageListener.class);

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);
		container.setBackOffTime(0);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		messageHandler.setApplicationContext(applicationContext);
		container.setMessageHandler(messageHandler);

		mockGetQueueUrl(sqs, "testQueue",
				"https://stop_withALogicalQueueName_mustStopOnlyTheSpecifiedQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://stop_withALogicalQueueName_mustStopOnlyTheSpecifiedQueue.amazonaws.com");
		mockGetQueueUrl(sqs, "anotherTestQueue",
				"https://stop_withALogicalQueueName_mustStopOnlyTheSpecifiedQueue.another.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://stop_withALogicalQueueName_mustStopOnlyTheSpecifiedQueue.another.amazonaws.com");

		when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder().build());

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();
		container.start();

		assertThat(container.isRunning("testQueue")).isTrue();
		assertThat(container.isRunning("anotherTestQueue")).isTrue();

		// Act
		container.stop("testQueue");

		// Assert
		assertThat(container.isRunning("testQueue")).isFalse();
		assertThat(container.isRunning("anotherTestQueue")).isTrue();

		container.stop();

		assertThat(container.isRunning("testQueue")).isFalse();
		assertThat(container.isRunning("anotherTestQueue")).isFalse();
	}

	@Test
	void stopAndStart_withContainerHavingARunningQueue_shouldRestartTheSpecifiedQueue() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		applicationContext.registerSingleton("anotherTestMessageListener", AnotherTestMessageListener.class);

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SqsClient sqs = mock(SqsClient.class);

		container.setAmazonSqs(sqs);
		container.setBackOffTime(0);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		messageHandler.setApplicationContext(applicationContext);
		container.setMessageHandler(messageHandler);

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();
		container.start();
		container.stop("testQueue");

		assertThat(container.isRunning("testQueue")).isFalse();
		assertThat(container.isRunning("anotherTestQueue")).isTrue();

		// Act
		container.start("testQueue");

		// Assert
		assertThat(container.isRunning("testQueue")).isTrue();
		assertThat(container.isRunning("anotherTestQueue")).isTrue();

		TestMessageListener testMessageListener = applicationContext.getBean(TestMessageListener.class);
		boolean await = testMessageListener.getCountDownLatch().await(1, TimeUnit.SECONDS);
		assertThat(await).isTrue();
		assertThat(testMessageListener.getMessage()).isEqualTo("Hello");
		container.stop();

		assertThat(container.isRunning("testQueue")).isFalse();
		assertThat(container.isRunning("anotherTestQueue")).isFalse();
	}

	@Test
	void stop_withQueueNameThatDoesNotExist_throwsAnException() throws Exception {
		// Arrange
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setAmazonSqs(mock(SqsClient.class, withSettings().stubOnly()));
		container.setMessageHandler(new QueueMessageHandler());
		container.afterPropertiesSet();

		// Assert
		assertThatThrownBy(() -> container.stop("foo")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("foo");
	}

	@Test
	void start_withQueueNameThatDoesNotExist_throwAnException() throws Exception {
		// Arrange
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setAmazonSqs(mock(SqsClient.class, withSettings().stubOnly()));
		container.setMessageHandler(new QueueMessageHandler());

		container.afterPropertiesSet();

		// Assert
		assertThatThrownBy(() -> container.start("bar")).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("bar");
	}

	@Test
	void start_withAQueueNameThatIsAlreadyRunning_shouldNotStartTheQueueAgainAndIgnoreTheCall() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());

		when(sqs.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder().build());

		container.setAmazonSqs(sqs);
		container.setBackOffTime(0);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		messageHandler.setApplicationContext(applicationContext);
		container.setMessageHandler(messageHandler);

		mockGetQueueUrl(sqs, "testQueue",
				"https://start_withAQueueNameThatIsAlreadyRunning_shouldNotStartTheQueueAgainAndIgnoreTheCall.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://start_withAQueueNameThatIsAlreadyRunning_shouldNotStartTheQueueAgainAndIgnoreTheCall.amazonaws.com");

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();
		container.start();

		assertThat(container.isRunning("testQueue")).isTrue();

		// Act
		container.start("testQueue");

		// Assert
		assertThat(container.isRunning("testQueue")).isTrue();

		container.stop();
	}

	@Test
	void stop_withAQueueNameThatIsNotRunning_shouldNotStopTheQueueAgainAndIgnoreTheCall() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);
		container.setBackOffTime(0);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		messageHandler.setApplicationContext(applicationContext);
		container.setMessageHandler(messageHandler);

		mockGetQueueUrl(sqs, "testQueue",
				"https://stop_withAQueueNameThatIsNotRunning_shouldNotStopTheQueueAgainAndIgnoreTheCall.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://stop_withAQueueNameThatIsNotRunning_shouldNotStopTheQueueAgainAndIgnoreTheCall.amazonaws.com");

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();
		container.start();

		container.stop("testQueue");
		assertThat(container.isRunning("testQueue")).isFalse();

		// Act
		container.stop("testQueue");

		// Assert
		assertThat(container.isRunning("testQueue")).isFalse();
	}

	@Test
	void setQueueStopTimeout_withNotDefaultTimeout_mustBeUsedWhenStoppingAQueue() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("longRunningListenerMethod", LongRunningListenerMethod.class);

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);
		container.setBackOffTime(0);
		container.setQueueStopTimeout(100);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		messageHandler.setApplicationContext(applicationContext);
		container.setMessageHandler(messageHandler);

		mockGetQueueUrl(sqs, "longRunningQueueMessage",
				"https://setQueueStopTimeout_withNotDefaultTimeout_mustBeUsedWhenStoppingAQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs,
				"https://setQueueStopTimeout_withNotDefaultTimeout_mustBeUsedWhenStoppingAQueue.amazonaws.com");
		mockReceiveMessage(sqs,
				"https://setQueueStopTimeout_withNotDefaultTimeout_mustBeUsedWhenStoppingAQueue.amazonaws.com", "Hello",
				"ReceiptHandle");

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();
		container.start();
		applicationContext.getBean(LongRunningListenerMethod.class).getCountDownLatch().await(1, TimeUnit.SECONDS);
		StopWatch stopWatch = new StopWatch();

		// Act
		stopWatch.start();
		container.stop("longRunningQueueMessage");
		stopWatch.stop();

		// Assert
		assertThat(container.getQueueStopTimeout()).isEqualTo(100);
		assertThat(stopWatch.getTotalTimeMillis() >= container.getQueueStopTimeout())
				.as("stop must last at least the defined queue stop timeout (> 100ms)").isTrue();
		assertThat(stopWatch.getTotalTimeMillis() < LongRunningListenerMethod.LISTENER_METHOD_WAIT_TIME)
				.as("stop must last less than the listener method (< 20000ms)").isTrue();
		container.stop();
	}

	@Test
	void stop_withContainerHavingMultipleQueuesRunning_shouldStopQueuesInParallel() throws Exception {
		// Arrange
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		applicationContext.registerSingleton("anotherTestMessageListener", AnotherTestMessageListener.class);

		CountDownLatch testQueueCountdownLatch = new CountDownLatch(1);
		CountDownLatch anotherTestQueueCountdownLatch = new CountDownLatch(1);
		CountDownLatch spinningThreadsStarted = new CountDownLatch(2);

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			public void stopQueue(String logicalQueueName) {
				if ("testQueue".equals(logicalQueueName)) {
					testQueueCountdownLatch.countDown();
				}
				else if ("anotherTestQueue".equals(logicalQueueName)) {
					anotherTestQueueCountdownLatch.countDown();
				}

				super.stopQueue(logicalQueueName);
			}
		};

		SqsClient sqs = mock(SqsClient.class, withSettings().stubOnly());
		container.setAmazonSqs(sqs);
		container.setBackOffTime(100);
		container.setQueueStopTimeout(5000);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		messageHandler.setApplicationContext(applicationContext);
		container.setMessageHandler(messageHandler);

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");
		mockGetQueueUrl(sqs, "anotherTestQueue", "https://anotherTestQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "https://anotherTestQueue.amazonaws.com");

		when(sqs.receiveMessage(
			ReceiveMessageRequest.builder()
				.queueUrl("http://testQueue.amazonaws.com")
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.maxNumberOfMessages(10)
				.build()))
			.thenAnswer((Answer<ReceiveMessageResponse>) invocation -> {
							spinningThreadsStarted.countDown();
							testQueueCountdownLatch.await(1, TimeUnit.SECONDS);
							throw OverLimitException.create("Boom", null);
						});

		when(sqs.receiveMessage(ReceiveMessageRequest.builder()
				.queueUrl("https://anotherTestQueue.amazonaws.com")
				.attributeNames(QueueAttributeName.ALL)
				.messageAttributeNames("All")
				.maxNumberOfMessages(10)
			.build()))
			.thenAnswer((Answer<ReceiveMessageResponse>) invocation -> {
							spinningThreadsStarted.countDown();
							anotherTestQueueCountdownLatch.await(1, TimeUnit.SECONDS);
							throw OverLimitException.create("Boom", null);
						});

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();
		container.start();
		spinningThreadsStarted.await(1, TimeUnit.SECONDS);
		StopWatch stopWatch = new StopWatch();

		// Act
		stopWatch.start();
		container.stop();
		stopWatch.stop();

		// Assert
		assertThat(stopWatch.getTotalTimeMillis() < 200)
				.as("Stop time must be shorter than stopping one queue after the other").isTrue();
	}

	private static class TestMessageListener {

		private final CountDownLatch countDownLatch = new CountDownLatch(1);

		private String message;

		@RuntimeUse
		@SqsListener("testQueue")
		private void handleMessage(String message) {
			this.message = message;
			this.countDownLatch.countDown();
		}

		String getMessage() {
			return this.message;
		}

		CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}

	}

	private static class FifoTestMessageListener {

		private String message;

		@RuntimeUse
		@SqsListener("testQueue.fifo")
		private void handleMessage(String message) {
			this.message = message;
		}

		String getMessage() {
			return this.message;
		}

	}

	private static class AnotherTestMessageListener {

		private String message;

		@RuntimeUse
		@SqsListener("anotherTestQueue")
		private void handleMessage(String message) {
			this.message = message;
		}

		String getMessage() {
			return this.message;
		}

	}

	@SuppressWarnings("NonExceptionNameEndsWithException")
	private static class TestMessageListenerThatThrowsAnExceptionWithAllDeletionPolicy {

		@SuppressWarnings("UnusedDeclaration")
		@SqsListener("testQueue")
		private void handleMessage(String message) {
			throw new RuntimeException();
		}

		@RuntimeUse
		@MessageExceptionHandler(RuntimeException.class)
		void handle() {
			// Empty body just to avoid unnecessary log output because no exception
			// handler was found.
		}

	}

	@SuppressWarnings("NonExceptionNameEndsWithException")
	private static class TestMessageListenerThatThrowsAnExceptionWithAllExceptOnRedriveDeletionPolicy {

		@SuppressWarnings("UnusedDeclaration")
		@SqsListener(value = "testQueue", deletionPolicy = SqsMessageDeletionPolicy.NO_REDRIVE)
		private void handleMessage(String message) {
			throw new RuntimeException();
		}

		@RuntimeUse
		@MessageExceptionHandler(RuntimeException.class)
		void handle() {
			// Empty body just to avoid unnecessary log output because no exception
			// handler was found.
		}

	}

	@Configuration
	protected static class SqsTestConfig {

		@Bean
		SqsClient amazonSQS() {
			SqsClient mockAmazonSQS = mock(SqsClient.class, withSettings().stubOnly());
			mockGetQueueUrl(mockAmazonSQS, "testQueue", "http://testQueue.amazonaws.com");
			when(mockAmazonSQS.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(ReceiveMessageResponse.builder().build());
			when(mockAmazonSQS.getQueueAttributes(any(GetQueueAttributesRequest.class)))
					.thenReturn(GetQueueAttributesResponse.builder().build());
			return mockAmazonSQS;
		}

	}

	private static class TestMessageListenerWithVisibilityProlong {

		private final CountDownLatch countDownLatch = new CountDownLatch(1);

		private Visibility visibility;

		@RuntimeUse
		@SqsListener(value = "testQueue", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
		private void manualSuccess(String message, Visibility visibility) {
			this.visibility = visibility;
			this.countDownLatch.countDown();
		}

		void extend(int seconds) {
			this.visibility.extend(seconds);
		}

		CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}

	}

	private static class TestMessageListenerWithManualDeletionPolicy {

		private final CountDownLatch countDownLatch = new CountDownLatch(1);

		private Acknowledgment acknowledgment;

		@RuntimeUse
		@SqsListener(value = "testQueue", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
		private void manualSuccess(String message, Acknowledgment acknowledgment) {
			this.acknowledgment = acknowledgment;
			this.countDownLatch.countDown();
		}

		void acknowledge() {
			this.acknowledgment.acknowledge();
		}

		CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}

	}

	private static class TestMessageListenerWithAllPossibleDeletionPolicies {

		private final CountDownLatch countdownLatch = new CountDownLatch(8);

		@RuntimeUse
		@SqsListener(value = "alwaysSuccess", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
		private void alwaysSuccess(String message) {
			this.countdownLatch.countDown();
		}

		@RuntimeUse
		@SqsListener(value = "alwaysError", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
		private void alwaysError(String message) {
			this.countdownLatch.countDown();
			throw new RuntimeException("BOOM!");
		}

		@RuntimeUse
		@SqsListener(value = "onSuccessSuccess", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
		private void onSuccessSuccess(String message) {
			this.countdownLatch.countDown();
		}

		@RuntimeUse
		@SqsListener(value = "onSuccessError", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
		private void onSuccessError(String message) {
			this.countdownLatch.countDown();
			throw new RuntimeException("BOOM!");
		}

		@RuntimeUse
		@SqsListener(value = "noRedriveSuccess", deletionPolicy = SqsMessageDeletionPolicy.NO_REDRIVE)
		private void noRedriveSuccess(String message) {
			this.countdownLatch.countDown();
		}

		@RuntimeUse
		@SqsListener(value = "noRedriveError", deletionPolicy = SqsMessageDeletionPolicy.NO_REDRIVE)
		private void noRedriveError(String message) {
			this.countdownLatch.countDown();
			throw new RuntimeException("BOOM!");
		}

		@RuntimeUse
		@SqsListener(value = "neverSuccess", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
		private void neverSuccess(String message, Acknowledgment acknowledgment) {
			this.countdownLatch.countDown();
		}

		@RuntimeUse
		@SqsListener(value = "neverError", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
		private void neverError(String message, Acknowledgment acknowledgment) {
			this.countdownLatch.countDown();
			throw new RuntimeException("BOOM!");
		}

		CountDownLatch getCountdownLatch() {
			return this.countdownLatch;
		}

		@RuntimeUse
		@MessageExceptionHandler(RuntimeException.class)
		private void swallowExceptions() {
		}

	}

	private static class LongRunningListenerMethod {

		private static final int LISTENER_METHOD_WAIT_TIME = 20000;

		private final CountDownLatch countDownLatch = new CountDownLatch(1);

		@RuntimeUse
		@SqsListener("longRunningQueueMessage")
		private void handleMessage(String message) throws InterruptedException {
			this.countDownLatch.countDown();
			Thread.sleep(LISTENER_METHOD_WAIT_TIME);
		}

		CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}

	}

}
