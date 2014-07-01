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

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.util.ErrorHandler;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class AbstractMessageListenerContainerTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();


	@Test
	public void testAfterPropertiesSetIsSettingActiveFlag() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class));
		container.setMessageHandler(mock(QueueMessageHandler.class));

		container.afterPropertiesSet();
		assertTrue(container.isActive());
	}

	@Test
	public void testAmazonSqsNullThrowsException() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("amazonSqs must not be null");

		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		container.setMessageHandler(mock(QueueMessageHandler.class));

		container.afterPropertiesSet();
	}

	@Test
	public void testMessageHandlerNullThrowsException() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("messageHandler must not be null");

		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class));

		container.afterPropertiesSet();
	}

	@Test
	public void testDestinationResolverIsCreatedIfNull() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class));
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.afterPropertiesSet();

		DestinationResolver<String> destinationResolver = container.getDestinationResolver();
		assertNotNull(destinationResolver);
		assertTrue(CachingDestinationResolver.class.isInstance(destinationResolver));
	}

	@Test
	public void testDisposableBeanResetActiveFlag() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class));
		container.setMessageHandler(mock(QueueMessageHandler.class));

		container.afterPropertiesSet();
		container.destroy();

		assertFalse(container.isActive());
	}

	@Test
	public void testSetAndGetBeanName() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setBeanName("test");
		assertEquals("test", container.getBeanName());
	}

	@Test
	public void testCustomDestinationResolverSet() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class));
		container.setMessageHandler(mock(QueueMessageHandler.class));

		DestinationResolver<String> destinationResolver = mock(DynamicQueueUrlDestinationResolver.class);
		container.setDestinationResolver(destinationResolver);

		container.afterPropertiesSet();

		assertEquals(destinationResolver, container.getDestinationResolver());
	}

	@Test
	public void testMaxNumberOfMessages() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertNull(container.getMaxNumberOfMessages());
		container.setMaxNumberOfMessages(23);
		assertEquals(new Integer(23), container.getMaxNumberOfMessages());
	}

	@Test
	public void testVisibilityTimeout() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertNull(container.getVisibilityTimeout());
		container.setVisibilityTimeout(32);
		assertEquals(new Integer(32), container.getVisibilityTimeout());
	}

	@Test
	public void testWaitTimeout() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertNull(container.getWaitTimeOut());
		container.setWaitTimeOut(42);
		assertEquals(new Integer(42), container.getWaitTimeOut());
	}

	@Test
	public void testIsAutoStartup() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertTrue(container.isAutoStartup());
		container.setAutoStartup(false);
		assertFalse(container.isAutoStartup());
	}

	@Test
	public void testGetAndSetPhase() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertEquals(Integer.MAX_VALUE, container.getPhase());
		container.setPhase(23);
		assertEquals(23L, container.getPhase());
	}

	@Test
	public void testIsActive() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		container.setMessageHandler(mock(QueueMessageHandler.class));

		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();
		assertTrue(container.isRunning());

		container.stop();
		assertFalse(container.isRunning());

		//Container can still be active an restarted later (e.g. paused for a while)
		assertTrue(container.isActive());
	}

	@Test
	public void receiveMessageRequests_withOneElement_created() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class);
		QueueMessageHandler messageHandler = new QueueMessageHandler();
		container.setAmazonSqs(mock);
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.setMessageHandler(messageHandler);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("messageListener", MessageListener.class);
		container.setMaxNumberOfMessages(11);
		container.setVisibilityTimeout(22);
		container.setWaitTimeOut(33);

		messageHandler.setApplicationContext(applicationContext);
		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		Map<String, ReceiveMessageRequest> messageRequests = container.getMessageRequests();
		assertEquals("http://testQueue.amazonaws.com", messageRequests.get("testQueue").getQueueUrl());
		assertEquals(11L, messageRequests.get("testQueue").getMaxNumberOfMessages().longValue());
		assertEquals(22L, messageRequests.get("testQueue").getVisibilityTimeout().longValue());
		assertEquals(33L, messageRequests.get("testQueue").getWaitTimeSeconds().longValue());
	}

	@Test
	public void receiveMessageRequests_withMultipleElements_created() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		QueueMessageHandler messageHandler = new QueueMessageHandler();
		messageHandler.setApplicationContext(applicationContext);
		container.setMessageHandler(messageHandler);
		applicationContext.registerSingleton("messageListener", MessageListener.class);
		applicationContext.registerSingleton("anotherMessageListener", AnotherMessageListener.class);

		container.setMaxNumberOfMessages(11);
		container.setVisibilityTimeout(22);
		container.setWaitTimeOut(33);

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));
		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("anotherTestQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://anotherTestQueue.amazonaws.com"));

		container.start();

		Map<String, ReceiveMessageRequest> messageRequests = container.getMessageRequests();
		assertEquals("http://testQueue.amazonaws.com", messageRequests.get("testQueue").getQueueUrl());
		assertEquals(11L, messageRequests.get("testQueue").getMaxNumberOfMessages().longValue());
		assertEquals(22L, messageRequests.get("testQueue").getVisibilityTimeout().longValue());
		assertEquals(33L, messageRequests.get("testQueue").getWaitTimeSeconds().longValue());
		assertEquals("http://anotherTestQueue.amazonaws.com", messageRequests.get("anotherTestQueue").getQueueUrl());
		assertEquals(11L, messageRequests.get("anotherTestQueue").getMaxNumberOfMessages().longValue());
		assertEquals(22L, messageRequests.get("anotherTestQueue").getVisibilityTimeout().longValue());
		assertEquals(33L, messageRequests.get("anotherTestQueue").getWaitTimeSeconds().longValue());
	}

	@Test
	public void testStartCallsDoStartMethod() throws Exception {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		AbstractMessageListenerContainer container = new AbstractMessageListenerContainer() {

			@Override
			protected void doStart() {
				countDownLatch.countDown();
			}

			@Override
			protected void doStop() {
				throw new UnsupportedOperationException("not supported yet");
			}
		};


		AmazonSQSAsync mock = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		try {
			assertTrue(countDownLatch.await(10, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail("Expected doStart() method to be called");
		}

	}

	@Test
	public void testStopCallsDoStopMethod() throws Exception {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		AbstractMessageListenerContainer container = new AbstractMessageListenerContainer() {

			@Override
			protected void doStart() {
				// do nothing in this case
			}

			@Override
			protected void doStop() {
				countDownLatch.countDown();
			}
		};


		AmazonSQSAsync mock = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		container.stop();

		try {
			assertTrue(countDownLatch.await(10, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			fail("Expected doStart() method to be called");
		}
	}

	@Test
	public void testStopCallsDoStopMethodWithRunnable() throws Exception {
		final CountDownLatch countDownLatch = new CountDownLatch(1);
		AbstractMessageListenerContainer container = new AbstractMessageListenerContainer() {

			@Override
			protected void doStart() {
				// do nothing in this case
			}

			@Override
			protected void doStop() {
				countDownLatch.countDown();
			}
		};


		AmazonSQSAsync mock = mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		container.stop(new Runnable() {

			@Override
			public void run() {
				try {
					assertTrue(countDownLatch.await(10, TimeUnit.MILLISECONDS));
				} catch (InterruptedException e) {
					fail("Expected doStart() method to be called");
				}
			}
		});
	}

	@Test
	public void testWithDefaultErrorHandler() throws Exception {
		final Logger logger = mock(Logger.class);

		AbstractMessageListenerContainer container = new AbstractMessageListenerContainer() {

			@Override
			protected Logger getLogger() {
				return logger;
			}

			@Override
			protected void doStart() {
			}

			@Override
			protected void doStop() {
			}


		};

		container.handleError(new Throwable("test"));
		verify(logger, times(1)).error(isA(String.class), isA(Throwable.class));
	}

	@Test
	public void testWithNullConfiguredErrorHandler() throws Exception {
		AbstractMessageListenerContainer container = new AbstractMessageListenerContainer() {

			@Override
			protected void doStart() {
			}

			@Override
			protected void doStop() {
			}
		};

		container.setErrorHandler(null);
		container.handleError(new Throwable("test"));
	}

	@Test
	public void testWithCustomErrorHandler() throws Exception {
		ErrorHandler errorHandler = mock(ErrorHandler.class);

		AbstractMessageListenerContainer container = new AbstractMessageListenerContainer() {

			@Override
			protected void doStart() {
			}

			@Override
			protected void doStop() {
			}
		};

		container.setErrorHandler(errorHandler);
		Throwable throwable = new Throwable("test");
		container.handleError(throwable);
		verify(errorHandler, times(1)).handleError(throwable);
	}

	@Test
	public void doDestroy_WhenContainerIsDestroyed_shouldBeCalled() throws Exception {
		// Arrange
		DestroyAwareAbstractMessageListenerContainer abstractMessageListenerContainer = new DestroyAwareAbstractMessageListenerContainer();

		// Act
		abstractMessageListenerContainer.destroy();

		// Assert
		assertTrue(abstractMessageListenerContainer.isDestroyCalled());
	}

	private static class StubAbstractMessageListenerContainer extends AbstractMessageListenerContainer {

		@Override
		protected void doStart() {
		}

		@Override
		protected void doStop() {
		}
	}

	private static class MessageListener {

		@SuppressWarnings({"UnusedDeclaration", "EmptyMethod"})
		@MessageMapping("testQueue")
		public void listenerMethod(String ignore) {

		}
	}

	private static class AnotherMessageListener {

		@SuppressWarnings({"UnusedDeclaration", "EmptyMethod"})
		@MessageMapping("anotherTestQueue")
		public void listenerMethod(String ignore) {

		}
	}

	private static class DestroyAwareAbstractMessageListenerContainer extends AbstractMessageListenerContainer {

		private boolean destroyCalled;

		private boolean isDestroyCalled() {
			return this.destroyCalled;
		}

		@Override
		protected void doStart() {

		}

		@Override
		protected void doStop() {

		}

		@Override
		protected void doDestroy() {
			this.destroyCalled = true;
		}


	}
}
