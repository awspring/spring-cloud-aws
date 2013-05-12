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
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.elasticspring.messaging.Message;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class ActorBasedMessageListenerContainerTest {

	@Test
	public void testGenericLifecycle() throws Exception {

		AmazonSQSAsync amazonSqsMock = Mockito.mock(AmazonSQSBufferedAsyncClient.class);


		final CountDownLatch countDownLatch = new CountDownLatch(10);

		final ReceiveMessageResult messageResult = new ReceiveMessageResult().withMessages(new com.amazonaws.services.sqs.model.Message().withBody("content").withReceiptHandle("123"));
		final String queueUrl = "http://test.aws.amazon.com";
		Mockito.when(amazonSqsMock.receiveMessage(new ReceiveMessageRequest(queueUrl))).thenAnswer(new Answer<ReceiveMessageResult>() {

			@Override
			public ReceiveMessageResult answer(InvocationOnMock invocation) throws Throwable {
				countDownLatch.countDown();
				return messageResult;
			}
		});

		ActorBasedMessageListenerContainer actorBasedMessageListenerContainer = new ActorBasedMessageListenerContainer();
		actorBasedMessageListenerContainer.setDestinationName("test");
		actorBasedMessageListenerContainer.setDestinationResolver(new DestinationResolver() {

			@Override
			public String resolveDestinationName(String destination) {
				return queueUrl;
			}
		});
		actorBasedMessageListenerContainer.setAmazonSqs(amazonSqsMock);

		final AtomicInteger messageCount = new AtomicInteger();

		actorBasedMessageListenerContainer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(Message<String> message) {
				messageCount.incrementAndGet();
			}
		});

		actorBasedMessageListenerContainer.setBeanName("test");
		actorBasedMessageListenerContainer.afterPropertiesSet();

		actorBasedMessageListenerContainer.start();
		countDownLatch.await();
		actorBasedMessageListenerContainer.stop();

		Mockito.verify(amazonSqsMock, Mockito.times(messageCount.intValue())).deleteMessage(
				new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle("123"));
	}

	@Test
	public void testListenerThrowsException() throws Exception {
		AmazonSQSAsync amazonSqsMock = Mockito.mock(AmazonSQSBufferedAsyncClient.class);


		final CountDownLatch countDownLatch = new CountDownLatch(1);

		final ReceiveMessageResult messageResult = new ReceiveMessageResult().withMessages(
				new com.amazonaws.services.sqs.model.Message().withBody("content").withReceiptHandle("123").withMessageId("456"));
		final String queueUrl = "http://test.aws.amazon.com";
		Mockito.when(amazonSqsMock.receiveMessage(new ReceiveMessageRequest(queueUrl))).thenAnswer(new Answer<ReceiveMessageResult>() {

			@Override
			public ReceiveMessageResult answer(InvocationOnMock invocation) throws Throwable {
				countDownLatch.countDown();
				return messageResult;
			}
		});

		ActorBasedMessageListenerContainer actorBasedMessageListenerContainer = new ActorBasedMessageListenerContainer();
		actorBasedMessageListenerContainer.setDestinationName("test");
		actorBasedMessageListenerContainer.setDestinationResolver(new DestinationResolver() {

			@Override
			public String resolveDestinationName(String destination) {
				return queueUrl;
			}
		});
		actorBasedMessageListenerContainer.setAmazonSqs(amazonSqsMock);

		actorBasedMessageListenerContainer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(Message<String> message) {
				throw new IllegalArgumentException("myError");
			}
		});

		actorBasedMessageListenerContainer.setBeanName("test");
		actorBasedMessageListenerContainer.afterPropertiesSet();

		actorBasedMessageListenerContainer.start();
		countDownLatch.await();
		actorBasedMessageListenerContainer.stop();

		Mockito.verify(amazonSqsMock, Mockito.times(0)).deleteMessageAsync(
				new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle("123"));

	}
}