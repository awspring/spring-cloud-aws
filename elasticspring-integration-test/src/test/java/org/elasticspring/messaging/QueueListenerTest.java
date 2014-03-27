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

package org.elasticspring.messaging;

import org.elasticspring.core.support.documentation.RuntimeUse;
import org.elasticspring.messaging.core.QueueMessagingTemplate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("QueueListenerTest-context.xml")
public class QueueListenerTest {

	@Autowired
	private MessageListener messageListener;

	@Autowired
	private MessageListenerWithSendTo messageListenerWithSendTo;

	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;

	@Test
	public void testSendAndReceive() throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();

		// Act
		this.queueMessagingTemplate.send("QueueListenerTest", MessageBuilder.withPayload("Hello world!").build());

		// Assert
		Assert.assertTrue(this.messageListener.getCountDownLatch().await(15, TimeUnit.SECONDS));
		Assert.assertEquals("Hello world!", this.messageListener.getReceivedMessages().get(0));
	}

	@Test
	public void sendToAnnotation_WithAValidDestination_messageIsSent() throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();
		this.messageListenerWithSendTo.getReceivedMessages().clear();

		// Act
		this.queueMessagingTemplate.send("SendToQueue", MessageBuilder.withPayload("Please answer!").build());

		// Assert
		Assert.assertTrue(this.messageListener.getCountDownLatch().await(15, TimeUnit.SECONDS));
		Assert.assertEquals("Please answer!", this.messageListenerWithSendTo.getReceivedMessages().get(0));
		Assert.assertEquals("PLEASE ANSWER!", this.messageListener.getReceivedMessages().get(0));
	}

	public static class MessageListener {

		private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);
		private CountDownLatch countDownLatch = new CountDownLatch(1);
		private final List<String> receivedMessages = new ArrayList<String>();

		@RuntimeUse
		@MessageMapping("QueueListenerTest")
		public void receiveMessage(String message) {
			LOGGER.debug("Received message with content {}", message);
			this.receivedMessages.add(message);
			this.getCountDownLatch().countDown();
		}

		public void setCountDownLatch(CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
		}

		CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}

		public List<String> getReceivedMessages() {
			return this.receivedMessages;
		}
	}

	public static class MessageListenerWithSendTo {

		private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);
		private final List<String> receivedMessages = new ArrayList<String>();

		@RuntimeUse
		@MessageMapping("SendToQueue")
		@SendTo("QueueListenerTest")
		public String receiveMessage(String message) {
			LOGGER.debug("Received message with content {}", message);
			this.receivedMessages.add(message);
			return message.toUpperCase();
		}

		public List<String> getReceivedMessages() {
			return this.receivedMessages;
		}

	}
}
