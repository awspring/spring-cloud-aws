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

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

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
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testWithDefaultTaskExecutorAndNoBeanName() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageHandler(Mockito.mock(QueueMessageHandler.class));

		container.afterPropertiesSet();

		SimpleAsyncTaskExecutor taskExecutor = (SimpleAsyncTaskExecutor) container.getTaskExecutor();
		Assert.assertNotNull(taskExecutor);
		Assert.assertEquals(SimpleMessageListenerContainer.class.getSimpleName() + "-", taskExecutor.getThreadNamePrefix());
	}

	@Test
	public void testWithDefaultTaskExecutorAndBeanName() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageHandler(Mockito.mock(QueueMessageHandler.class));
		container.setBeanName("testContainerName");
		container.afterPropertiesSet();

		SimpleAsyncTaskExecutor taskExecutor = (SimpleAsyncTaskExecutor) container.getTaskExecutor();
		Assert.assertNotNull(taskExecutor);
		Assert.assertEquals("testContainerName-", taskExecutor.getThreadNamePrefix());
	}

	@Test
	public void testCustomTaskExecutor() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageHandler(Mockito.mock(QueueMessageHandler.class));
		container.setBeanName("testContainerName");
		container.afterPropertiesSet();

		Assert.assertEquals(taskExecutor, container.getTaskExecutor());
	}

	@Test
	public void testSimpleReceiveMessage() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);


		final CountDownLatch countDownLatch = new CountDownLatch(1);
		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				Assert.assertEquals("messageContent", message.getPayload());
				countDownLatch.countDown();
			}
		};
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		messageHandler.setApplicationContext(applicationContext);
		container.setBeanName("testContainerName");
		messageHandler.afterPropertiesSet();

		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("testQueue"))).thenReturn(new GetQueueUrlResult().
				withQueueUrl("http://testQueue.amazonaws.com"));

		container.afterPropertiesSet();

		Mockito.when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com").withAttributeNames("All"))).
				thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent"),
						new Message().withBody("messageContent"))).
				thenReturn(new ReceiveMessageResult());

		container.start();

		assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));

		container.stop();
	}

	@Test
	public void testContainerDoesNotProcessMessageAfterBeingStopped() throws Exception {
		final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				Assert.fail("Should not have been called");
			}
		};
		container.setMessageHandler(messageHandler);
		container.setBeanName("testContainerName");

		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("testQueue"))).thenReturn(new GetQueueUrlResult().
				withQueueUrl("http://testQueue.amazonaws.com"));

		container.afterPropertiesSet();

		Mockito.when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com"))).
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
	public void testListenerMethodThrowsException() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void handleError(Throwable throwable) {
				Assert.assertNotNull(throwable);
				assertTrue(IllegalArgumentException.class.isInstance(throwable));
				super.stop();
			}
		};
		AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				throw new IllegalArgumentException("expected exception");
			}
		};
		container.setMessageHandler(messageHandler);
		container.setBeanName("testContainerName");

		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("testQueue"))).thenReturn(new GetQueueUrlResult().
				withQueueUrl("http://testQueue.amazonaws.com"));

		container.afterPropertiesSet();

		Mockito.when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com"))).
				thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent"),
						new Message().withBody("messageContent"))).
				thenReturn(new ReceiveMessageResult());

		container.start();
	}

	@Test
	public void listener_withMultipleMessageHandlers_shouldBeCalled() throws Exception {
		final CountDownLatch countDownLatch = new CountDownLatch(2);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				super.executeMessage(stringMessage);
				countDownLatch.countDown();
			}
		};
		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		applicationContext.registerSingleton("anotherTestMessageListener", AnotherTestMessageListener.class);

		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("testQueue"))).thenReturn(new GetQueueUrlResult().
				withQueueUrl("http://testQueue.amazonaws.com"));
		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("anotherTestQueue"))).thenReturn(new GetQueueUrlResult().
				withQueueUrl("http://anotherTestQueue.amazonaws.com"));

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		Mockito.when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com").withAttributeNames("All"))).
				thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent"))).
				thenReturn(new ReceiveMessageResult());
		Mockito.when(sqs.receiveMessage(new ReceiveMessageRequest("http://anotherTestQueue.amazonaws.com").withAttributeNames("All"))).
				thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("anotherMessageContent"))).
				thenReturn(new ReceiveMessageResult());

		container.start();

		assertTrue(countDownLatch.await(2L, TimeUnit.SECONDS));
		container.stop();
		assertEquals("messageContent", applicationContext.getBean(TestMessageListener.class).getMessage());
		assertEquals("anotherMessageContent", applicationContext.getBean(AnotherTestMessageListener.class).getMessage());
	}

	@Test
	public void messageExecutor_withMessageWitAttributes_shouldPassThemAsHeaders() throws Exception {
		// Arrange
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer() {

			@Override
			protected void executeMessage(org.springframework.messaging.Message<String> stringMessage) {
				super.executeMessage(stringMessage);
				countDownLatch.countDown();
			}
		};

		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		QueueMessageHandler messageHandler = spy(new QueueMessageHandler());
		container.setMessageHandler(messageHandler);

		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);

		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("testQueue"))).thenReturn(new GetQueueUrlResult().
				withQueueUrl("http://testQueue.amazonaws.com"));

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		Mockito.when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com").withAttributeNames("All"))).
				thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent").withAttributes(Collections.singletonMap("SenderId", "ID"))));

		// Act
		container.start();

		// Assert
		assertTrue(countDownLatch.await(2L, TimeUnit.SECONDS));
		container.stop();

		Mockito.verify(messageHandler).handleMessage(this.stringMessageCaptor.capture());
		assertEquals("ID", this.stringMessageCaptor.getValue().getHeaders().get("SenderId"));

	}

	private static class TestMessageListener {

		private String message;

		@SuppressWarnings("UnusedDeclaration")
		@MessageMapping("testQueue")
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
		@MessageMapping("anotherTestQueue")
		private void handleMessage(String message) {
			this.message = message;
		}

		public String getMessage() {
			return this.message;
		}
	}

}