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

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MimeType;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class SimpleMessageListenerContainerTest {

	@Captor
	private ArgumentCaptor<org.springframework.messaging.Message<String>> stringMessageCaptor;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
	}

	@Test
	public void testWithDefaultTaskExecutorAndNoBeanName() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class));
		container.setMessageHandler(mock(QueueMessageHandler.class));

		container.afterPropertiesSet();

		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) container.getTaskExecutor();
		assertNotNull(taskExecutor);
		assertEquals(SimpleMessageListenerContainer.class.getSimpleName() + "-", taskExecutor.getThreadNamePrefix());
	}

	@Test
	public void testWithDefaultTaskExecutorAndBeanName() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class));
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.setBeanName("testContainerName");
		container.afterPropertiesSet();

		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) container.getTaskExecutor();
		assertNotNull(taskExecutor);
		assertEquals("testContainerName-", taskExecutor.getThreadNamePrefix());
	}

	@Test
	public void testCustomTaskExecutor() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		container.setAmazonSqs(mock(AmazonSQSAsync.class));
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.setBeanName("testContainerName");
		container.afterPropertiesSet();

		assertEquals(taskExecutor, container.getTaskExecutor());
	}

	@Test
	public void testSimpleReceiveMessage() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				countDownLatch.countDown();
				assertEquals("messageContent", message.getPayload());
			}
		};
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		messageHandler.setApplicationContext(applicationContext);
		container.setBeanName("testContainerName");
		messageHandler.afterPropertiesSet();

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testQueue.amazonaws.com");

		container.afterPropertiesSet();

		when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com").withAttributeNames("All")
				.withMessageAttributeNames("All")
				.withMaxNumberOfMessages(10)))
				.thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent"),
						new Message().withBody("messageContent")))
				.thenReturn(new ReceiveMessageResult());
		when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(new GetQueueAttributesResult());

		container.start();

		assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));

		container.stop();
	}

	@Test
	public void testContainerDoesNotProcessMessageAfterBeingStopped() throws Exception {
		final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				fail("Should not have been called");
			}
		};
		container.setMessageHandler(messageHandler);
		container.setBeanName("testContainerName");

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");

		container.afterPropertiesSet();

		when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com"))).
				thenAnswer(new Answer<ReceiveMessageResult>() {

					@Override
					public ReceiveMessageResult answer(InvocationOnMock invocation) throws Throwable {
						container.stop();
						return new ReceiveMessageResult().withMessages(new Message().withBody("test"));
					}
				});

		container.start();
	}

	@Test
	public void listener_withMultipleMessageHandlers_shouldBeCalled() throws Exception {
		final CountDownLatch countDownLatch = new CountDownLatch(2);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};
		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		applicationContext.registerSingleton("anotherTestMessageListener", AnotherTestMessageListener.class);

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testQueue.amazonaws.com");
		mockGetQueueUrl(sqs, "anotherTestQueue", "http://anotherTestQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://anotherTestQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com").withAttributeNames("All")
				.withMessageAttributeNames("All")
				.withMaxNumberOfMessages(10)))
				.thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent")))
				.thenReturn(new ReceiveMessageResult());
		when(sqs.receiveMessage(new ReceiveMessageRequest("http://anotherTestQueue.amazonaws.com").withAttributeNames("All")
				.withMessageAttributeNames("All")
				.withMaxNumberOfMessages(10)))
				.thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("anotherMessageContent")))
				.thenReturn(new ReceiveMessageResult());
		when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(new GetQueueAttributesResult());

		container.start();

		assertTrue(countDownLatch.await(2L, TimeUnit.SECONDS));
		container.stop();
		assertEquals("messageContent", applicationContext.getBean(TestMessageListener.class).getMessage());
		assertEquals("anotherMessageContent", applicationContext.getBean(AnotherTestMessageListener.class).getMessage());
	}

	@Test
	public void messageExecutor_withMessageWithAttributes_shouldPassThemAsHeaders() throws Exception {
		// Arrange
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = spy(new QueueMessageHandler());
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com").withAttributeNames("All")
				.withMessageAttributeNames("All")
				.withMaxNumberOfMessages(10)))
				.thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent").withAttributes(Collections.singletonMap("SenderId", "ID"))))
				.thenReturn(new ReceiveMessageResult());
		when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(new GetQueueAttributesResult());

		// Act
		container.start();

		// Assert
		assertTrue(countDownLatch.await(2L, TimeUnit.SECONDS));
		container.stop();

		verify(messageHandler).handleMessage(this.stringMessageCaptor.capture());
		assertEquals("ID", this.stringMessageCaptor.getValue().getHeaders().get("SenderId"));

	}

	@Test
	public void doDestroy_whenContainerCallsDestroy_DestroysDefaultTaskExecutor() throws Exception {
		// Arrange
		SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		simpleMessageListenerContainer.setAmazonSqs(sqs);
		QueueMessageHandler messageHandler = mock(QueueMessageHandler.class);
		simpleMessageListenerContainer.setMessageHandler(messageHandler);
		simpleMessageListenerContainer.afterPropertiesSet();
		simpleMessageListenerContainer.start();

		// Act
		simpleMessageListenerContainer.destroy();

		// Assert
		assertTrue(((ThreadPoolTaskExecutor) simpleMessageListenerContainer.getTaskExecutor()).getThreadPoolExecutor().isTerminated());
	}

	@Test
	public void afterPropertiesSet_whenCalled_taskExecutorIsActive() throws Exception {
		// Arrange
		SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		simpleMessageListenerContainer.setAmazonSqs(sqs);
		QueueMessageHandler messageHandler = mock(QueueMessageHandler.class);
		simpleMessageListenerContainer.setMessageHandler(messageHandler);

		// Act
		simpleMessageListenerContainer.afterPropertiesSet();

		// Assert
		assertFalse(((ThreadPoolTaskExecutor) simpleMessageListenerContainer.getTaskExecutor()).getThreadPoolExecutor().isTerminated());
	}

	@Test
	public void messageExecutor_messageWithMimeTypeMessageAttribute_shouldSetItAsHeader() throws Exception {
		// Arrange
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = spy(new QueueMessageHandler());
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		MimeType mimeType = new MimeType("text", "plain", Charset.forName("UTF-8"));
		when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com").withAttributeNames("All")
				.withMessageAttributeNames("All")
				.withMaxNumberOfMessages(10)))
				.thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent")
						.withAttributes(Collections.singletonMap("SenderId", "ID"))
						.withMessageAttributes(Collections.singletonMap(MessageHeaders.CONTENT_TYPE, new MessageAttributeValue().withDataType("String")
								.withStringValue(mimeType.toString())))))
				.thenReturn(new ReceiveMessageResult());
		when(sqs.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(new GetQueueAttributesResult());

		// Act
		container.start();

		// Assert
		assertTrue(countDownLatch.await(2L, TimeUnit.SECONDS));
		container.stop();

		verify(messageHandler).handleMessage(this.stringMessageCaptor.capture());
		assertEquals(mimeType, this.stringMessageCaptor.getValue().getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void queueMessageHandler_withJavaConfig_shouldScanTheListenerMethods() throws Exception {
		// Arrange & Act
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(SqsTestConfig.class, TestMessageListener.class);
		SimpleMessageListenerContainer simpleMessageListenerContainer = applicationContext.getBean(SimpleMessageListenerContainer.class);
		QueueMessageHandler queueMessageHandler = (QueueMessageHandler) ReflectionTestUtils.getField(simpleMessageListenerContainer, "messageHandler");

		// Assert
		assertEquals(1, queueMessageHandler.getHandlerMethods().size());
	}

	@Test
	public void executeMessage_successfulExecution_shouldRemoveMessageFromQueue() throws Exception {
		// Arrange
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		mockReceiveMessage(sqs, "http://testQueue.amazonaws.com", "messageContent", "ReceiptHandle");

		// Act
		container.start();

		// Assert
		assertTrue(countDownLatch.await(2L, TimeUnit.SECONDS));
		container.stop();
		verify(sqs, times(1)).deleteMessageAsync(eq(new DeleteMessageRequest("http://testQueue.amazonaws.com", "ReceiptHandle")));
	}

	@Test
	public void executeMessage_executionThrowsExceptionAndQueueHasAllDeletionPolicy_shouldRemoveMessageFromQueue() throws Exception {
		// Arrange
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListenerThatThrowsAnExceptionWithAllDeletionPolicy.class);

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com").withAttributeNames("All").withMaxNumberOfMessages(10).withMessageAttributeNames("All"))).
				thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent").withReceiptHandle("ReceiptHandle")),
						new ReceiveMessageResult());

		// Act
		container.start();

		// Assert
		assertTrue(countDownLatch.await(2L, TimeUnit.SECONDS));
		container.stop();
		verify(sqs, times(1)).deleteMessageAsync(eq(new DeleteMessageRequest("http://testQueue.amazonaws.com", "ReceiptHandle")));
	}

	@Test
	public void executeMessage_executionThrowsExceptionAndQueueHasRedrivePolicy_shouldNotRemoveMessageFromQueue() throws Exception {
		// Arrange
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListenerThatThrowsAnExceptionWithAllExceptOnRedriveDeletionPolicy.class);

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");
		mockGetQueueAttributesWithRedrivePolicy(sqs, "http://testQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com").withAttributeNames("All")
				.withMaxNumberOfMessages(10)
				.withMessageAttributeNames("All")))
				.thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent").withReceiptHandle("ReceiptHandle")),
						new ReceiveMessageResult());

		// Act
		container.start();

		// Assert
		assertTrue(countDownLatch.await(2L, TimeUnit.SECONDS));
		container.stop();
		verify(sqs, never()).deleteMessageAsync(eq(new DeleteMessageRequest("http://testQueue.amazonaws.com", "ReceiptHandle")));
	}

	@Test
	public void doStop_containerNotRunning_shouldNotThrowAnException() throws Exception {
		// Arrange
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setAmazonSqs(mock(AmazonSQSAsync.class));
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.setAutoStartup(false);
		container.afterPropertiesSet();

		// Act & Assert
		container.stop();
	}

	@Test
	public void receiveMessage_throwsAnException_operationShouldBeRetried() throws Exception {
		// Arrange
		AmazonSQSAsync amazonSqs = mock(AmazonSQSAsync.class);
		when(amazonSqs.receiveMessage(any(ReceiveMessageRequest.class))).thenThrow(new RuntimeException("Boom!"))
				.thenReturn(new ReceiveMessageResult()
						.withMessages(new Message().withBody("messageContent"),
								new Message().withBody("messageContent")));

		final CountDownLatch countDownLatch = new CountDownLatch(1);
		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				countDownLatch.countDown();
				assertEquals("messageContent", message.getPayload());
			}
		};

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		messageHandler.setApplicationContext(applicationContext);

		mockGetQueueUrl(amazonSqs, "testQueue", "http://testQueue.amazonaws.com");
		messageHandler.afterPropertiesSet();

		when(amazonSqs.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(new GetQueueAttributesResult());

		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		container.setBackOffTime(0);
		container.setAmazonSqs(amazonSqs);
		container.setMessageHandler(messageHandler);
		container.setAutoStartup(false);
		container.afterPropertiesSet();

		// Act
		container.start();

		// Assert
		assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));
	}

	@Test
	public void receiveMessage_withMessageListenerMethodAndNeverDeletionPolicy_waitsForAcknowledgmentBeforeDeletion() throws Exception {
		// Arrange
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				countDownLatch.countDown();
				super.executeMessage(stringMessage);
			}
		};

		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testListener", TestMessageListenerWithManualDeletionPolicy.class);

		mockGetQueueUrl(sqs, "testQueue", "http://testQueue.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://testQueue.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		mockReceiveMessage(sqs, "http://testQueue.amazonaws.com", "messageContent", "ReceiptHandle");

		// Act
		container.start();

		// Assert
		countDownLatch.await(1L, TimeUnit.SECONDS);
		verify(sqs, never()).deleteMessageAsync(eq(new DeleteMessageRequest("http://testQueue.amazonaws.com", "ReceiptHandle")));
		TestMessageListenerWithManualDeletionPolicy testMessageListenerWithManualDeletionPolicy = applicationContext.getBean(TestMessageListenerWithManualDeletionPolicy.class);
		testMessageListenerWithManualDeletionPolicy.getCountDownLatch().await(1L, TimeUnit.SECONDS);
		testMessageListenerWithManualDeletionPolicy.acknowledge();
		verify(sqs, times(1)).deleteMessageAsync(eq(new DeleteMessageRequest("http://testQueue.amazonaws.com", "ReceiptHandle")));
	}

	@Test
	public void executeMessage_withDifferentDeletionPolicies_shouldActAccordingly() throws Exception {
		// Arrange
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				super.executeMessage(stringMessage);
			}
		};

		AmazonSQSAsync sqs = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testListener", TestMessageListenerWithAllPossibleDeletionPolicies.class);

		mockGetQueueUrl(sqs, "alwaysSuccess", "http://alwaysSuccess.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://alwaysSuccess.amazonaws.com");
		mockGetQueueUrl(sqs, "alwaysError", "http://alwaysError.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://alwaysError.amazonaws.com");
		mockGetQueueUrl(sqs, "onSuccessSuccess", "http://onSuccessSuccess.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://onSuccessSuccess.amazonaws.com");
		mockGetQueueUrl(sqs, "onSuccessError", "http://onSuccessError.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://onSuccessError.amazonaws.com");
		mockGetQueueUrl(sqs, "noRedriveSuccess", "http://noRedriveSuccess.amazonaws.com");
		mockGetQueueAttributesWithRedrivePolicy(sqs, "http://noRedriveSuccess.amazonaws.com");
		mockGetQueueUrl(sqs, "noRedriveError", "http://noRedriveError.amazonaws.com");
		mockGetQueueAttributesWithRedrivePolicy(sqs, "http://noRedriveError.amazonaws.com");
		mockGetQueueUrl(sqs, "neverSuccess", "http://neverSuccess.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://neverSuccess.amazonaws.com");
		mockGetQueueUrl(sqs, "neverError", "http://neverError.amazonaws.com");
		mockGetQueueAttributesWithEmptyResult(sqs, "http://neverError.amazonaws.com");

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		mockReceiveMessage(sqs, "http://alwaysSuccess.amazonaws.com", "foo", "alwaysSuccess");
		mockReceiveMessage(sqs, "http://alwaysError.amazonaws.com", "foo", "alwaysError");
		mockReceiveMessage(sqs, "http://onSuccessSuccess.amazonaws.com", "foo", "onSuccessSuccess");
		mockReceiveMessage(sqs, "http://onSuccessError.amazonaws.com", "foo", "onSuccessError");
		mockReceiveMessage(sqs, "http://noRedriveSuccess.amazonaws.com", "foo", "noRedriveSuccess");
		mockReceiveMessage(sqs, "http://noRedriveError.amazonaws.com", "foo", "noRedriveError");
		mockReceiveMessage(sqs, "http://neverSuccess.amazonaws.com", "foo", "neverSuccess");
		mockReceiveMessage(sqs, "http://neverError.amazonaws.com", "foo", "neverError");

		// Act
		container.start();

		// Assert
		container.stop();
		TestMessageListenerWithAllPossibleDeletionPolicies bean = applicationContext.getBean(TestMessageListenerWithAllPossibleDeletionPolicies.class);
		assertTrue(bean.getCountdownLatch().await(1L, TimeUnit.SECONDS));
		verify(sqs, times(1)).deleteMessageAsync(eq(new DeleteMessageRequest("http://alwaysSuccess.amazonaws.com", "alwaysSuccess")));
		verify(sqs, times(1)).deleteMessageAsync(eq(new DeleteMessageRequest("http://alwaysError.amazonaws.com", "alwaysError")));
		verify(sqs, times(1)).deleteMessageAsync(eq(new DeleteMessageRequest("http://onSuccessSuccess.amazonaws.com", "onSuccessSuccess")));
		verify(sqs, never()).deleteMessageAsync(eq(new DeleteMessageRequest("http://onSuccessError.amazonaws.com", "onSuccessError")));
		verify(sqs, times(1)).deleteMessageAsync(eq(new DeleteMessageRequest("http://noRedriveSuccess.amazonaws.com", "noRedriveSuccess")));
		verify(sqs, never()).deleteMessageAsync(eq(new DeleteMessageRequest("http://noRedriveError.amazonaws.com", "noRedriveError")));
		verify(sqs, never()).deleteMessageAsync(eq(new DeleteMessageRequest("http://neverSuccess.amazonaws.com", "neverSuccess")));
		verify(sqs, never()).deleteMessageAsync(eq(new DeleteMessageRequest("http://neverError.amazonaws.com", "neverError")));
	}

	private void mockReceiveMessage(AmazonSQSAsync sqs, String queueUrl, String messageContent, String receiptHandle) {
		when(sqs.receiveMessage(new ReceiveMessageRequest(queueUrl).withAttributeNames("All")
				.withMessageAttributeNames("All")
				.withMaxNumberOfMessages(10)))
				.thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody(messageContent).withReceiptHandle(receiptHandle)),
						new ReceiveMessageResult());
	}

	private static void mockGetQueueAttributesWithRedrivePolicy(AmazonSQSAsync sqs, String queueUrl) {
		when(sqs.getQueueAttributes(new GetQueueAttributesRequest(queueUrl).withAttributeNames(QueueAttributeName.RedrivePolicy))).
				thenReturn(new GetQueueAttributesResult().addAttributesEntry(QueueAttributeName.RedrivePolicy.toString(), "{\"some\": \"JSON\"}"));
	}

	private static void mockGetQueueAttributesWithEmptyResult(AmazonSQSAsync sqs, String queueUrl) {
		when(sqs.getQueueAttributes(new GetQueueAttributesRequest(queueUrl).withAttributeNames(QueueAttributeName.RedrivePolicy))).
				thenReturn(new GetQueueAttributesResult());
	}

	private static void mockGetQueueUrl(AmazonSQSAsync sqs, String queueName, String queueUrl) {
		when(sqs.getQueueUrl(new GetQueueUrlRequest(queueName))).thenReturn(new GetQueueUrlResult().
				withQueueUrl(queueUrl));
	}

	private static class TestMessageListener {

		private String message;

		@SuppressWarnings("UnusedDeclaration")
		@SqsListener("testQueue")
		private void handleMessage(String message) {
			this.message = message;
		}

		public String getMessage() {
			return this.message;
		}
	}

	private static class AnotherTestMessageListener {

		private String message;

		@SuppressWarnings("UnusedDeclaration")
		@SqsListener("anotherTestQueue")
		private void handleMessage(String message) {
			this.message = message;
		}

		public String getMessage() {
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

		@MessageExceptionHandler(RuntimeException.class)
		public void handle() {
			// Empty body just to avoid unnecessary log output because no exception handler was found.
		}

	}

	@SuppressWarnings("NonExceptionNameEndsWithException")
	private static class TestMessageListenerThatThrowsAnExceptionWithAllExceptOnRedriveDeletionPolicy {

		@SuppressWarnings("UnusedDeclaration")
		@SqsListener(value = "testQueue", deletionPolicy = SqsMessageDeletionPolicy.NO_REDRIVE)
		private void handleMessage(String message) {
			throw new RuntimeException();
		}

		@MessageExceptionHandler(RuntimeException.class)
		public void handle() {
			// Empty body just to avoid unnecessary log output because no exception handler was found.
		}

	}

	@Configuration
	@EnableSqs
	protected static class SqsTestConfig {

		@Bean
		public AmazonSQSAsync amazonSQS() {
			AmazonSQSAsync mockAmazonSQS = mock(AmazonSQSAsync.class);
			mockGetQueueUrl(mockAmazonSQS, "testQueue", "http://testQueue.amazonaws.com");
			when(mockAmazonSQS.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(new ReceiveMessageResult());
			when(mockAmazonSQS.getQueueAttributes(any(GetQueueAttributesRequest.class))).thenReturn(new GetQueueAttributesResult());
			return mockAmazonSQS;
		}

	}

	private static class TestMessageListenerWithManualDeletionPolicy {

		private Acknowledgment acknowledgment;
		private final CountDownLatch countDownLatch = new CountDownLatch(1);

		@SqsListener(value = "testQueue", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
		private void manualSuccess(String message, Acknowledgment acknowledgment) {
			this.acknowledgment = acknowledgment;
			this.countDownLatch.countDown();
		}

		public void acknowledge() {
			this.acknowledgment.acknowledge();
		}

		public CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}
	}

	private static class TestMessageListenerWithAllPossibleDeletionPolicies {

		private final CountDownLatch countdownLatch = new CountDownLatch(8);

		@SqsListener(value = "alwaysSuccess", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
		private void alwaysSuccess(String message) {
			this.countdownLatch.countDown();
		}

		@SqsListener(value = "alwaysError", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
		private void alwaysError(String message) {
			this.countdownLatch.countDown();
			throw new RuntimeException("BOOM!");
		}

		@SqsListener(value = "onSuccessSuccess", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
		private void onSuccessSuccess(String message) {
			this.countdownLatch.countDown();
		}

		@SqsListener(value = "onSuccessError", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
		private void onSuccessError(String message) {
			this.countdownLatch.countDown();
			throw new RuntimeException("BOOM!");
		}

		@SqsListener(value = "noRedriveSuccess", deletionPolicy = SqsMessageDeletionPolicy.NO_REDRIVE)
		private void noRedriveSuccess(String message) {
			this.countdownLatch.countDown();
		}

		@SqsListener(value = "noRedriveError", deletionPolicy = SqsMessageDeletionPolicy.NO_REDRIVE)
		private void noRedriveError(String message) {
			this.countdownLatch.countDown();
			throw new RuntimeException("BOOM!");
		}

		@SqsListener(value = "neverSuccess", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
		private void neverSuccess(String message, Acknowledgment acknowledgment) {
			this.countdownLatch.countDown();
		}

		@SqsListener(value = "neverError", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
		private void neverError(String message, Acknowledgment acknowledgment) {
			this.countdownLatch.countDown();
			throw new RuntimeException("BOOM!");
		}

		public CountDownLatch getCountdownLatch() {
			return this.countdownLatch;
		}

		@MessageExceptionHandler(RuntimeException.class)
		private void swallowExceptions() {
		}
	}

}