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

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import org.springframework.cloud.aws.messaging.listener.AbstractMessageListenerContainer.QueueAttributes;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.cloud.aws.messaging.support.destination.DynamicQueueUrlDestinationResolver;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.core.CachingDestinationResolverProxy;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
public class MessageListenerContainerTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testAfterPropertiesSetIsSettingActiveFlag() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class, withSettings().stubOnly()));
		container.setMessageHandler(mock(QueueMessageHandler.class));

		container.afterPropertiesSet();
		assertThat(container.isActive()).isTrue();
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

		container.setAmazonSqs(mock(AmazonSQSAsync.class, withSettings().stubOnly()));

		container.afterPropertiesSet();
	}

	@Test
	public void testDestinationResolverIsCreatedIfNull() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class, withSettings().stubOnly()));
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.afterPropertiesSet();

		DestinationResolver<String> destinationResolver = container
				.getDestinationResolver();
		assertThat(destinationResolver).isNotNull();
		assertThat(CachingDestinationResolverProxy.class.isInstance(destinationResolver))
				.isTrue();
	}

	@Test
	public void testDisposableBeanResetActiveFlag() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class, withSettings().stubOnly()));
		container.setMessageHandler(mock(QueueMessageHandler.class));

		container.afterPropertiesSet();
		container.destroy();

		assertThat(container.isActive()).isFalse();
	}

	@Test
	public void testSetAndGetBeanName() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setBeanName("test");
		assertThat(container.getBeanName()).isEqualTo("test");
	}

	@Test
	public void testCustomDestinationResolverSet() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		container.setAmazonSqs(mock(AmazonSQSAsync.class, withSettings().stubOnly()));
		container.setMessageHandler(mock(QueueMessageHandler.class));

		DestinationResolver<String> destinationResolver = mock(
				DynamicQueueUrlDestinationResolver.class);
		container.setDestinationResolver(destinationResolver);

		container.afterPropertiesSet();

		assertThat(container.getDestinationResolver()).isEqualTo(destinationResolver);
	}

	@Test
	public void testMaxNumberOfMessages() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertThat(container.getMaxNumberOfMessages()).isNull();
		container.setMaxNumberOfMessages(23);
		assertThat(container.getMaxNumberOfMessages()).isEqualTo(new Integer(23));
	}

	@Test
	public void testVisibilityTimeout() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertThat(container.getVisibilityTimeout()).isNull();
		container.setVisibilityTimeout(32);
		assertThat(container.getVisibilityTimeout()).isEqualTo(new Integer(32));
	}

	@Test
	public void testWaitTimeout() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertThat(container.getWaitTimeOut()).isEqualTo(new Integer(20));
		container.setWaitTimeOut(42);
		assertThat(container.getWaitTimeOut()).isEqualTo(new Integer(42));
	}

	@Test
	public void testIsAutoStartup() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertThat(container.isAutoStartup()).isTrue();
		container.setAutoStartup(false);
		assertThat(container.isAutoStartup()).isFalse();
	}

	@Test
	public void testGetAndSetPhase() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		assertThat(container.getPhase()).isEqualTo(Integer.MAX_VALUE);
		container.setPhase(23);
		assertThat(container.getPhase()).isEqualTo(23L);
	}

	@Test
	public void testIsActive() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class, withSettings().stubOnly());
		container.setAmazonSqs(mock);
		container.setMessageHandler(mock(QueueMessageHandler.class));

		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue")))
				.thenReturn(new GetQueueUrlResult()
						.withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();
		assertThat(container.isRunning()).isTrue();

		container.stop();
		assertThat(container.isRunning()).isFalse();

		// Container can still be active an restarted later (e.g. paused for a while)
		assertThat(container.isActive()).isTrue();
	}

	@Test
	public void receiveMessageRequests_withOneElement_created() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class, withSettings().stubOnly());
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

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue")))
				.thenReturn(new GetQueueUrlResult()
						.withQueueUrl("http://testQueue.amazonaws.com"));
		when(mock.getQueueAttributes(any(GetQueueAttributesRequest.class)))
				.thenReturn(new GetQueueAttributesResult());

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();
		container.start();

		Map<String, QueueAttributes> registeredQueues = container.getRegisteredQueues();
		assertThat(registeredQueues.get("testQueue").getReceiveMessageRequest()
				.getQueueUrl()).isEqualTo("http://testQueue.amazonaws.com");
		assertThat(registeredQueues.get("testQueue").getReceiveMessageRequest()
				.getMaxNumberOfMessages().longValue()).isEqualTo(11L);
		assertThat(registeredQueues.get("testQueue").getReceiveMessageRequest()
				.getVisibilityTimeout().longValue()).isEqualTo(22L);
		assertThat(registeredQueues.get("testQueue").getReceiveMessageRequest()
				.getWaitTimeSeconds().longValue()).isEqualTo(33L);
	}

	@Test
	public void receiveMessageRequests_withMultipleElements_created() throws Exception {
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class, withSettings().stubOnly());
		container.setAmazonSqs(mock);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		QueueMessageHandler messageHandler = new QueueMessageHandler();
		messageHandler.setApplicationContext(applicationContext);
		container.setMessageHandler(messageHandler);
		applicationContext.registerSingleton("messageListener", MessageListener.class);
		applicationContext.registerSingleton("anotherMessageListener",
				AnotherMessageListener.class);

		container.setMaxNumberOfMessages(11);
		container.setVisibilityTimeout(22);
		container.setWaitTimeOut(33);

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue")))
				.thenReturn(new GetQueueUrlResult()
						.withQueueUrl("http://testQueue.amazonaws.com"));
		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("anotherTestQueue")))
				.thenReturn(new GetQueueUrlResult()
						.withQueueUrl("https://anotherTestQueue.amazonaws.com"));
		when(mock.getQueueAttributes(any(GetQueueAttributesRequest.class)))
				.thenReturn(new GetQueueAttributesResult());

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();
		container.start();

		Map<String, QueueAttributes> registeredQueues = container.getRegisteredQueues();
		assertThat(registeredQueues.get("testQueue").getReceiveMessageRequest()
				.getQueueUrl()).isEqualTo("http://testQueue.amazonaws.com");
		assertThat(registeredQueues.get("testQueue").getReceiveMessageRequest()
				.getMaxNumberOfMessages().longValue()).isEqualTo(11L);
		assertThat(registeredQueues.get("testQueue").getReceiveMessageRequest()
				.getVisibilityTimeout().longValue()).isEqualTo(22L);
		assertThat(registeredQueues.get("testQueue").getReceiveMessageRequest()
				.getWaitTimeSeconds().longValue()).isEqualTo(33L);
		assertThat(registeredQueues.get("anotherTestQueue").getReceiveMessageRequest()
				.getQueueUrl()).isEqualTo("https://anotherTestQueue.amazonaws.com");
		assertThat(registeredQueues.get("anotherTestQueue").getReceiveMessageRequest()
				.getMaxNumberOfMessages().longValue()).isEqualTo(11L);
		assertThat(registeredQueues.get("anotherTestQueue").getReceiveMessageRequest()
				.getVisibilityTimeout().longValue()).isEqualTo(22L);
		assertThat(registeredQueues.get("anotherTestQueue").getReceiveMessageRequest()
				.getWaitTimeSeconds().longValue()).isEqualTo(33L);
	}

	@Test
	public void testStartCallsDoStartMethod() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
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

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class, withSettings().stubOnly());
		container.setAmazonSqs(mock);
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue")))
				.thenReturn(new GetQueueUrlResult()
						.withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		try {
			assertThat(countDownLatch.await(10, TimeUnit.MILLISECONDS)).isTrue();
		}
		catch (InterruptedException e) {
			fail("Expected doStart() method to be called");
		}

	}

	@Test
	public void testStopCallsDoStopMethod() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
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

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class, withSettings().stubOnly());
		container.setAmazonSqs(mock);
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue")))
				.thenReturn(new GetQueueUrlResult()
						.withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		container.stop();

		try {
			assertThat(countDownLatch.await(10, TimeUnit.MILLISECONDS)).isTrue();
		}
		catch (InterruptedException e) {
			fail("Expected doStart() method to be called");
		}
	}

	@Test
	public void testStopCallsDoStopMethodWithRunnable() throws Exception {
		CountDownLatch countDownLatch = new CountDownLatch(1);
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

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class, withSettings().stubOnly());
		container.setAmazonSqs(mock);
		container.setMessageHandler(mock(QueueMessageHandler.class));
		container.afterPropertiesSet();

		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue")))
				.thenReturn(new GetQueueUrlResult()
						.withQueueUrl("http://testQueue.amazonaws.com"));

		container.start();

		container.stop(() -> {
			try {
				assertThat(countDownLatch.await(10, TimeUnit.MILLISECONDS)).isTrue();
			}
			catch (InterruptedException e) {
				fail("Expected doStart() method to be called");
			}
		});
	}

	@Test
	public void doDestroy_WhenContainerIsDestroyed_shouldBeCalled() throws Exception {
		// Arrange
		DestroyAwareAbstractMessageListenerContainer abstractMessageListenerContainer;
		abstractMessageListenerContainer = new DestroyAwareAbstractMessageListenerContainer();

		// Act
		abstractMessageListenerContainer.destroy();

		// Assert
		assertThat(abstractMessageListenerContainer.isDestroyCalled()).isTrue();
	}

	@Test
	public void receiveMessageRequests_withDestinationResolverThrowingException_shouldLogWarningAndNotCreateRequest()
			throws Exception {
		// Arrange
		AbstractMessageListenerContainer container = new StubAbstractMessageListenerContainer();
		Logger loggerMock = container.getLogger();

		AmazonSQSAsync mock = mock(AmazonSQSAsync.class, withSettings().stubOnly());
		container.setAmazonSqs(mock);
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		QueueMessageHandler messageHandler = new QueueMessageHandler();
		messageHandler.setApplicationContext(applicationContext);
		container.setMessageHandler(messageHandler);
		applicationContext.registerSingleton("messageListener", MessageListener.class);
		applicationContext.registerSingleton("anotherMessageListener",
				AnotherMessageListener.class);

		String destinationResolutionExceptionMessage = "Queue not found";
		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("testQueue")))
				.thenThrow(new DestinationResolutionException(
						destinationResolutionExceptionMessage));
		when(mock.getQueueUrl(new GetQueueUrlRequest().withQueueName("anotherTestQueue")))
				.thenReturn(new GetQueueUrlResult()
						.withQueueUrl("https://anotherTestQueue.amazonaws.com"));
		when(mock.getQueueAttributes(any(GetQueueAttributesRequest.class)))
				.thenReturn(new GetQueueAttributesResult());

		messageHandler.afterPropertiesSet();
		container.afterPropertiesSet();

		// Act
		container.start();

		// Assert
		ArgumentCaptor<String> logMsgArgCaptor = ArgumentCaptor.forClass(String.class);
		verify(loggerMock).warn(logMsgArgCaptor.capture());
		Map<String, QueueAttributes> registeredQueues = container.getRegisteredQueues();
		assertThat(registeredQueues.containsKey("testQueue")).isFalse();
		assertThat(logMsgArgCaptor.getValue())
				.isEqualTo("Ignoring queue with name 'testQueue': "
						+ destinationResolutionExceptionMessage);
		assertThat(registeredQueues.get("anotherTestQueue").getReceiveMessageRequest()
				.getQueueUrl()).isEqualTo("https://anotherTestQueue.amazonaws.com");
	}

	private static class StubAbstractMessageListenerContainer
			extends AbstractMessageListenerContainer {

		private final Logger mock = mock(Logger.class);

		@Override
		protected void doStart() {
		}

		@Override
		protected void doStop() {
		}

		@Override
		protected Logger getLogger() {
			return this.mock;
		}

	}

	private static class MessageListener {

		@SuppressWarnings({ "UnusedDeclaration", "EmptyMethod" })
		@SqsListener("testQueue")
		public void listenerMethod(String ignore) {

		}

	}

	private static class AnotherMessageListener {

		@SuppressWarnings({ "UnusedDeclaration", "EmptyMethod" })
		@SqsListener("anotherTestQueue")
		public void listenerMethod(String ignore) {

		}

	}

	private static class DestroyAwareAbstractMessageListenerContainer
			extends AbstractMessageListenerContainer {

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
