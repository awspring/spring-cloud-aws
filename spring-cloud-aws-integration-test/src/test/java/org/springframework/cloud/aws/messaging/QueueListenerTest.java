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

package org.springframework.cloud.aws.messaging;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class QueueListenerTest extends AbstractContainerTest {

	@Autowired
	private MessageListener messageListener;

	@Autowired
	private MessageListenerWithSendTo messageListenerWithSendTo;

	@Autowired
	private RedrivePolicyTestListener redrivePolicyTestListener;

	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;

	@Test
	public void messageMapping_singleMessageOnQueue_messageReceived() throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();

		// Act
		this.queueMessagingTemplate.send("QueueListenerTest", MessageBuilder.withPayload("Hello world!").build());

		// Assert
		assertTrue(this.messageListener.getCountDownLatch().await(15, TimeUnit.SECONDS));
		assertEquals("Hello world!", this.messageListener.getReceivedMessages().get(0));
	}

	@Test
	public void send_simpleString_shouldBeReceivedWithoutDoubleQuotes() throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();

		// Act
		this.queueMessagingTemplate.convertAndSend("QueueListenerTest", "Hello world!");

		// Assert
		assertTrue(this.messageListener.getCountDownLatch().await(15, TimeUnit.SECONDS));
		assertEquals("Hello world!", this.messageListener.getReceivedMessages().get(0));
	}

	@Test
	public void sendToAnnotation_WithAValidDestination_messageIsSent() throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();
		this.messageListenerWithSendTo.getReceivedMessages().clear();

		// Act
		this.queueMessagingTemplate.convertAndSend("SendToQueue", "Please answer!");

		// Assert
		assertTrue(this.messageListener.getCountDownLatch().await(15, TimeUnit.SECONDS));
		assertEquals("Please answer!", this.messageListenerWithSendTo.getReceivedMessages().get(0));
		assertEquals("PLEASE ANSWER!", this.messageListener.getReceivedMessages().get(0));
	}

	@Test
	public void receiveMessage_withArgumentAnnotatedWithHeaderOrHeaders_shouldReceiveHeaderValues() throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();

		// Act
		this.queueMessagingTemplate.send("QueueListenerTest", MessageBuilder.withPayload("Is the header received?").build());

		// Assert
		assertTrue(this.messageListener.getCountDownLatch().await(15, TimeUnit.SECONDS));
		assertNotNull(this.messageListener.getSenderId());
		assertNotNull(this.messageListener.getAllHeaders());
	}

	@Test
	public void redrivePolicy_withMessageMappingThrowingAnException_messageShouldAppearInDeadLetterQueue() throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		this.redrivePolicyTestListener.setCountDownLatch(countDownLatch);

		// Act
		this.queueMessagingTemplate.convertAndSend("QueueWithRedrivePolicy", "Hello");

		// Assert
		assertTrue(countDownLatch.await(15, TimeUnit.SECONDS));
	}

	public static class MessageListener {

		private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);
		private final List<String> receivedMessages = new ArrayList<>();
		private CountDownLatch countDownLatch = new CountDownLatch(1);
		private String senderId;
		private Map<String, Object> allHeaders;

		@RuntimeUse
		@MessageMapping("QueueListenerTest")
		public void receiveMessage(String message, @Header(value = "SenderId", required = false) String senderId, @Headers Map<String, Object> allHeaders) {
			LOGGER.debug("Received message with content {}", message);
			this.receivedMessages.add(message);
			this.senderId = senderId;
			this.allHeaders = allHeaders;
			this.getCountDownLatch().countDown();
		}

		CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}

		public void setCountDownLatch(CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
		}

		public List<String> getReceivedMessages() {
			return this.receivedMessages;
		}

		public String getSenderId() {
			return this.senderId;
		}

		public Map<String, Object> getAllHeaders() {
			return Collections.unmodifiableMap(this.allHeaders);
		}
	}

	public static class MessageListenerWithSendTo {

		private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);
		private final List<String> receivedMessages = new ArrayList<>();

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

	public static class RedrivePolicyTestListener {

		private CountDownLatch countDownLatch = new CountDownLatch(1);

		public void setCountDownLatch(CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
		}

		@RuntimeUse
		@MessageMapping("QueueWithRedrivePolicy")
		public void receiveThrowingException(String message) {
			throw new RuntimeException();
		}

		@RuntimeUse
		@MessageMapping("DeadLetterQueue")
		public void receiveDeadLetters(String message) {
			this.countDownLatch.countDown();
		}

		@MessageExceptionHandler(RuntimeException.class)
		public void handle() {
			// Empty body just to avoid unnecessary log output because no exception handler was found.
		}

	}

}
