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

import com.amazonaws.services.sqs.AmazonSQS;
import org.elasticspring.core.support.documentation.RuntimeUse;
import org.elasticspring.messaging.core.QueueMessagingTemplate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
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
@ContextConfiguration("QueueListenerTest-context.xml")
public class QueueListenerTest {

	@Autowired
	private MessageListener messageListener;

	@Autowired
	private MessageListenerWithSendTo messageListenerWithSendTo;

	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;

	@Autowired
	private AmazonSQS amazonSqs;

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
		assertEquals(8, this.messageListener.getAllHeaders().size());
	}

	public static class MessageListener {

		private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);
		private CountDownLatch countDownLatch = new CountDownLatch(1);
		private final List<String> receivedMessages = new ArrayList<String>();
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

		public void setCountDownLatch(CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
		}

		CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
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
