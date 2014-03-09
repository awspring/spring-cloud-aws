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
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
		container.setMessageHandler(Mockito.mock(MessageHandler.class));
		container.setApplicationContext(new StaticApplicationContext());

		container.afterPropertiesSet();

		SimpleAsyncTaskExecutor taskExecutor = (SimpleAsyncTaskExecutor) container.getTaskExecutor();
		Assert.assertNotNull(taskExecutor);
		Assert.assertEquals(SimpleMessageListenerContainer.class.getSimpleName() + "-", taskExecutor.getThreadNamePrefix());
	}

	@Test
	public void testWithDefaultTaskExecutorAndBeanName() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageHandler(Mockito.mock(MessageHandler.class));
		container.setApplicationContext(new StaticApplicationContext());
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
		container.setMessageHandler(Mockito.mock(MessageHandler.class));
		container.setApplicationContext(new StaticApplicationContext());
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
		MessageHandler messageHandler = new MessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				Assert.assertEquals("messageContent", message.getPayload());
				countDownLatch.countDown();
			}
		};
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		container.setApplicationContext(applicationContext);
		container.setBeanName("testContainerName");

		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("testQueue"))).thenReturn(new GetQueueUrlResult().
				withQueueUrl("http://testQueue.amazonaws.com"));

		container.afterPropertiesSet();

		Mockito.when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com"))).
				thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent"),
						new Message().withBody("messageContent"))).
				thenReturn(new ReceiveMessageResult());

		container.start();

		Assert.assertTrue(countDownLatch.await(1, TimeUnit.SECONDS));

		container.stop();
	}

	@Test
	public void testContainerDoesNotProcessMessageAfterBeingStopped() throws Exception {
		final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		MessageHandler messageHandler = new MessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				Assert.fail("Should not have been called");
			}
		};
		container.setMessageHandler(messageHandler);
		container.setApplicationContext(new StaticApplicationContext());
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
				Assert.assertTrue(IllegalArgumentException.class.isInstance(throwable));
				super.stop();
			}
		};
		SyncTaskExecutor taskExecutor = new SyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);

		MessageHandler messageHandler = new MessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				throw new IllegalArgumentException("expected exception");
			}
		};
		container.setMessageHandler(messageHandler);
		container.setApplicationContext(new StaticApplicationContext());
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
	public void listener_withMultipleMessageHandlers_shouldBeCalledCorrectly() throws Exception {
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

		MessageHandler messageHandler = Mockito.mock(MessageHandler.class);
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("testMessageListener", TestMessageListener.class);
		applicationContext.registerSingleton("anotherTestMessageListener", AnotherTestMessageListener.class);
		container.setApplicationContext(applicationContext);

		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("testQueue"))).thenReturn(new GetQueueUrlResult().
				withQueueUrl("http://testQueue.amazonaws.com"));
		Mockito.when(sqs.getQueueUrl(new GetQueueUrlRequest("anotherTestQueue"))).thenReturn(new GetQueueUrlResult().
				withQueueUrl("http://anotherTestQueue.amazonaws.com"));

		container.afterPropertiesSet();

		Mockito.when(sqs.receiveMessage(new ReceiveMessageRequest("http://testQueue.amazonaws.com"))).
				thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("messageContent"))).
				thenReturn(new ReceiveMessageResult());
		Mockito.when(sqs.receiveMessage(new ReceiveMessageRequest("http://anotherTestQueue.amazonaws.com"))).
				thenReturn(new ReceiveMessageResult().withMessages(new Message().withBody("anotherMessageContent"))).
				thenReturn(new ReceiveMessageResult());

		container.start();

		Assert.assertTrue(countDownLatch.await(2L, TimeUnit.SECONDS));
		Mockito.verify(messageHandler, Mockito.times(2)).handleMessage(this.stringMessageCaptor.capture());
		List<String> capturedPayloads = Arrays.asList(this.stringMessageCaptor.getAllValues().get(0).getPayload(), this.stringMessageCaptor.getAllValues().get(1).getPayload());
		Assert.assertTrue(capturedPayloads.contains("messageContent"));
		Assert.assertTrue(capturedPayloads.contains("anotherMessageContent"));
	}

	private static class TestMessageListener {

		@SuppressWarnings("UnusedDeclaration")
		@MessageMapping("testQueue")
		private void handleMessage(String ignore) {

		}

	}

	private static class AnotherTestMessageListener {

		@SuppressWarnings("UnusedDeclaration")
		@MessageMapping("anotherTestQueue")
		private void handleMessage(String ignore) {

		}

	}

}