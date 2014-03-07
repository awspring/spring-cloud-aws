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
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class SimpleMessageListenerContainerTest {

	@Test
	public void testWithDefaultTaskExecutorAndNoBeanName() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageHandler(Mockito.mock(MessageHandler.class));
		container.setDestinationName("testQueue");

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
		container.setDestinationName("testQueue");
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
		container.setDestinationName("testQueue");
		container.setBeanName("testContainerName");
		container.afterPropertiesSet();

		Assert.assertEquals(taskExecutor, container.getTaskExecutor());
	}

	@Test
	public void testSimpleReceiveMessage() throws Exception {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		container.setTaskExecutor(taskExecutor);

		AmazonSQSAsync sqs = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(sqs);


		final CountDownLatch countDownLatch = new CountDownLatch(1);
		container.setMessageHandler(new MessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				Assert.assertEquals("messageContent", message.getPayload());
				countDownLatch.countDown();
			}
		});

		container.setDestinationName("testQueue");
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

		container.setMessageHandler(new MessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				Assert.fail("Should not have been called");
			}
		});

		container.setDestinationName("testQueue");
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

		container.setMessageHandler(new MessageHandler() {

			@Override
			public void handleMessage(org.springframework.messaging.Message<?> message) throws MessagingException {
				throw new IllegalArgumentException("expected exception");
			}
		});

		container.setDestinationName("testQueue");
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
}