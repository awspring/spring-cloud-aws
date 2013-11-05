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
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import org.elasticspring.messaging.support.destination.CachingDestinationResolver;
import org.elasticspring.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.ErrorHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class AbstractMessageListenerContainerTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();


	@Test
	public void testAfterPropertiesSetIsSettingActiveFlag() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");

		container.afterPropertiesSet();
		Assert.assertTrue(container.isActive());
	}

	@Test
	public void testAmazonSqsNullThrowsException() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("amazonSqs must not be null");

		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");

		container.afterPropertiesSet();
	}

	@Test
	public void testMessageListenerNullThrowsException() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("messageListener must not be null");

		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setDestinationName("testQueue");

		container.afterPropertiesSet();
	}

	@Test
	public void testDestinationNameNullThrowsException() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("destinationName must not be null");

		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageListener(Mockito.mock(MessageListener.class));

		container.afterPropertiesSet();
	}

	@Test
	public void testDestinationResolverIsCreatedIfNull() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");
		container.afterPropertiesSet();

		DestinationResolver destinationResolver = container.getDestinationResolver();
		Assert.assertNotNull(destinationResolver);
		Assert.assertTrue(CachingDestinationResolver.class.isInstance(destinationResolver));
	}

	@Test
	public void testDisposableBeanResetActiveFlag() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");

		container.afterPropertiesSet();
		container.destroy();

		Assert.assertFalse(container.isActive());
	}

	@Test
	public void testSetAndGetBeanName() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setBeanName("test");
		Assert.assertEquals("test", container.getBeanName());
	}

	@Test
	public void testCustomDestinationResolverSet() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(Mockito.mock(AmazonSQSAsync.class));
		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");

		DestinationResolver<String> destinationResolver = Mockito.mock(DynamicQueueUrlDestinationResolver.class);
		container.setDestinationResolver(destinationResolver);

		container.afterPropertiesSet();

		Assert.assertEquals(destinationResolver, container.getDestinationResolver());
	}

	@Test
	public void testMaxNumberOfMessages() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		Assert.assertNull(container.getMaxNumberOfMessages());
		container.setMaxNumberOfMessages(23);
		Assert.assertEquals(new Integer(23), container.getMaxNumberOfMessages());
	}

	@Test
	public void testVisibilityTimeout() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		Assert.assertNull(container.getVisibilityTimeout());
		container.setVisibilityTimeout(32);
		Assert.assertEquals(new Integer(32), container.getVisibilityTimeout());
	}

	@Test
	public void testWaitTimeout() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		Assert.assertNull(container.getWaitTimeOut());
		container.setWaitTimeOut(42);
		Assert.assertEquals(new Integer(42), container.getWaitTimeOut());
	}

	@Test
	public void testIsAutoStartup() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		Assert.assertTrue(container.isAutoStartup());
		container.setAutoStartup(false);
		Assert.assertFalse(container.isAutoStartup());
	}

	@Test
	public void testGetAndSetPhase() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		Assert.assertEquals(Integer.MAX_VALUE, container.getPhase());
		container.setPhase(23);
		Assert.assertEquals(23L, container.getPhase());
	}

	@Test
	public void testIsActive() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		AmazonSQSAsync mock = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");

		container.afterPropertiesSet();

		Mockito.when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();
		Assert.assertTrue(container.isRunning());

		container.stop();
		Assert.assertFalse(container.isRunning());

		//Container can still be active an restarted later (e.g. paused for a while)
		Assert.assertTrue(container.isActive());
	}

	@Test
	public void testReceiveMessageRequestCreated() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		AmazonSQSAsync mock = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");
		container.setMaxNumberOfMessages(11);
		container.setVisibilityTimeout(22);
		container.setWaitTimeOut(33);

		container.afterPropertiesSet();

		Mockito.when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		ReceiveMessageRequest receiveMessageRequest = container.getReceiveMessageRequest();
		Assert.assertEquals("http://testQueue.amazonaws.com", receiveMessageRequest.getQueueUrl());
		Assert.assertEquals(11L, receiveMessageRequest.getMaxNumberOfMessages().longValue());
		Assert.assertEquals(22L, receiveMessageRequest.getVisibilityTimeout().longValue());
		Assert.assertEquals(33L, receiveMessageRequest.getWaitTimeSeconds().longValue());
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


		AmazonSQSAsync mock = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");

		container.afterPropertiesSet();

		Mockito.when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		try {
			Assert.assertTrue(countDownLatch.await(10, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			Assert.fail("Expected doStart() method to be called");
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


		AmazonSQSAsync mock = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");

		container.afterPropertiesSet();

		Mockito.when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		container.stop();

		try {
			Assert.assertTrue(countDownLatch.await(10, TimeUnit.MILLISECONDS));
		} catch (InterruptedException e) {
			Assert.fail("Expected doStart() method to be called");
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


		AmazonSQSAsync mock = Mockito.mock(AmazonSQSAsync.class);
		container.setAmazonSqs(mock);
		container.setMessageListener(Mockito.mock(MessageListener.class));
		container.setDestinationName("testQueue");

		container.afterPropertiesSet();

		Mockito.when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue"))).
				thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		container.stop(new Runnable() {

			@Override
			public void run() {
				try {
					Assert.assertTrue(countDownLatch.await(10, TimeUnit.MILLISECONDS));
				} catch (InterruptedException e) {
					Assert.fail("Expected doStart() method to be called");
				}
			}
		});
	}

	@Test
	public void testWithDefaultErrorHandler() throws Exception {
		final Logger logger = Mockito.mock(Logger.class);

		AbstractMessageListenerContainer container = new AbstractMessageListenerContainer() {

			@Override
			protected void doStart() {
			}

			@Override
			protected void doStop() {
			}

			@Override
			protected Logger getLogger() {
				return logger;
			}
		};

		container.handleError(new Throwable("test"));
		Mockito.verify(logger, Mockito.times(1)).error(Mockito.isA(String.class), Mockito.isA(Throwable.class));
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
		ErrorHandler errorHandler = Mockito.mock(ErrorHandler.class);

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
		Mockito.verify(errorHandler, Mockito.times(1)).handleError(throwable);
	}

	private static class StubAbstractMessageListenerContainer extends AbstractMessageListenerContainer {

		@Override
		protected void doStart() {
		}

		@Override
		protected void doStop() {
		}
	}
}
