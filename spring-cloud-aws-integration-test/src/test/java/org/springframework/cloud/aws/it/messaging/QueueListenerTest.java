/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.it.messaging;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.core.SqsMessageHeaders;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @since 1.0
 */
@SuppressWarnings({ "AbstractClassWithoutAbstractMethods",
		"SpringJavaAutowiringInspection" })
@ExtendWith(SpringExtension.class)
abstract class QueueListenerTest extends AbstractContainerTest {

	@Autowired
	private MessageListener messageListener;

	@Autowired
	private MessageListenerWithSendTo messageListenerWithSendTo;

	@Autowired
	private RedrivePolicyTestListener redrivePolicyTestListener;

	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;

	@Autowired
	private ManualDeletionPolicyTestListener manualDeletionPolicyTestListener;

	@Test
	void messageMapping_singleMessageOnQueue_messageReceived() throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();
		String payload = randomString();

		// Act
		this.queueMessagingTemplate.send("QueueListenerTest",
				MessageBuilder.withPayload(payload).build());

		// Assert
		await().atMost(Duration.ofSeconds(30)).until(
				() -> this.messageListener.getReceivedMessages().contains(payload));
	}

	@Test
	void send_simpleString_shouldBeReceivedWithoutDoubleQuotes() throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();
		String payload = randomString();

		// Act
		this.queueMessagingTemplate.convertAndSend("QueueListenerTest", payload);

		// Assert
		await().atMost(Duration.ofSeconds(15)).until(
				() -> this.messageListener.getReceivedMessages().contains(payload));
	}

	@Test
	void sendToAnnotation_WithAValidDestination_messageIsSent() throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();
		this.messageListenerWithSendTo.getReceivedMessages().clear();
		String payload = randomString();

		// Act
		this.queueMessagingTemplate.convertAndSend("SendToQueue", payload);

		// Assert
		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			assertThat(this.messageListenerWithSendTo.getReceivedMessages())
					.contains(payload);
			assertThat(this.messageListener.getReceivedMessages())
					.contains(payload.toUpperCase()); // messageListenerWithSendTo
														// converts to upper case
		});
	}

	@Test
	void receiveMessage_withArgumentAnnotatedWithHeaderOrHeaders_shouldReceiveHeaderValues()
			throws Exception {
		// Arrange
		this.messageListener.setCountDownLatch(new CountDownLatch(1));
		this.messageListener.getReceivedMessages().clear();

		// Act
		ByteBuffer binaryValue = ByteBuffer.wrap("Binary value".getBytes());
		int numberValue = 123456;
		String stringValue = "String value";
		this.queueMessagingTemplate.send("QueueListenerTest",
				MessageBuilder.withPayload("Is the header received?")
						.setHeader("stringHeader", stringValue)
						.setHeader("numberHeader", numberValue)
						.setHeader("binaryHeader", binaryValue).build());

		// Assert
		await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
			assertThat(this.messageListener.getSenderId()).isNotNull();
			assertThat(this.messageListener.getAllHeaders()).isNotNull();
			assertThat(this.messageListener.getAllHeaders().get("stringHeader"))
					.isEqualTo(stringValue);
			assertThat(this.messageListener.getAllHeaders().get("numberHeader"))
					.isEqualTo(numberValue);
			assertThat(this.messageListener.getAllHeaders().get("binaryHeader"))
					.isEqualTo(binaryValue);
		});
	}

	@Test
	void redrivePolicy_withMessageMappingThrowingAnException_messageShouldAppearInDeadLetterQueue()
			throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		this.redrivePolicyTestListener.setCountDownLatch(countDownLatch);

		// Act
		this.queueMessagingTemplate.convertAndSend("QueueWithRedrivePolicy", "Hello");

		// Assert
		await().atMost(Duration.ofSeconds(30))
				.until(() -> countDownLatch.getCount() == 0);
	}

	@RepeatedTest(10) // just to make sure that it does not fail
	void manualDeletion_withAcknowledgmentCalled_shouldSucceedAndDeleteMessage()
			throws Exception {
		// Act
		this.queueMessagingTemplate.convertAndSend("ManualDeletionQueue", "Message");

		// Assert
		await().atMost(Duration.ofSeconds(15))
				.until(() -> this.manualDeletionPolicyTestListener.getCountDownLatch()
						.getCount() == 0);
	}

	private static String randomString() {
		return UUID.randomUUID().toString();
	}

	public static class MessageListener {

		private static final Logger LOGGER = LoggerFactory
				.getLogger(MessageListener.class);

		private final List<String> receivedMessages = new ArrayList<>();

		private CountDownLatch countDownLatch = new CountDownLatch(1);

		private String senderId;

		private Long timestamp;

		private Long sentTimestamp;

		private Long approximateFirstReceiveTimestamp;

		private Long approximateReceiveCount;

		private Map<String, Object> allHeaders;

		@RuntimeUse
		@SqsListener("QueueListenerTest")
		public void receiveMessage(String message,
				@Header(value = "SenderId", required = false) String senderId,
				@Headers Map<String, Object> allHeaders, SqsMessageHeaders asSqsHeaders) {
			LOGGER.debug("Received message with content {}", message);
			this.receivedMessages.add(message);
			this.senderId = senderId;
			this.allHeaders = allHeaders;
			this.approximateReceiveCount = asSqsHeaders.getApproximateReceiveCount();
			this.approximateFirstReceiveTimestamp = asSqsHeaders
					.getApproximateFirstReceiveTimestamp();
			this.timestamp = asSqsHeaders.getTimestamp();
			this.sentTimestamp = asSqsHeaders.getSentTimestamp();
			this.getCountDownLatch().countDown();
		}

		CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}

		public void setCountDownLatch(CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
		}

		public List<String> getReceivedMessages() {
			return new ArrayList<>(this.receivedMessages);
		}

		public String getSenderId() {
			return this.senderId;
		}

		public Long getTimestamp() {
			return timestamp;
		}

		public Long getSentTimestamp() {
			return sentTimestamp;
		}

		public Long getApproximateFirstReceiveTimestamp() {
			return approximateFirstReceiveTimestamp;
		}

		public Long getApproximateReceiveCount() {
			return approximateReceiveCount;
		}

		public Map<String, Object> getAllHeaders() {
			return Collections.unmodifiableMap(this.allHeaders);
		}

	}

	public static class MessageListenerWithSendTo {

		private static final Logger LOGGER = LoggerFactory
				.getLogger(MessageListener.class);

		private final List<String> receivedMessages = new ArrayList<>();

		@RuntimeUse
		@SqsListener("SendToQueue")
		@SendTo("QueueListenerTest")
		public String receiveMessage(String message) {
			LOGGER.debug("Received message with content {}", message);
			this.receivedMessages.add(message);
			return message.toUpperCase();
		}

		public List<String> getReceivedMessages() {
			return new ArrayList<>(this.receivedMessages);
		}

	}

	public static class RedrivePolicyTestListener {

		private CountDownLatch countDownLatch = new CountDownLatch(1);

		public void setCountDownLatch(CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
		}

		@RuntimeUse
		@SqsListener(value = "QueueWithRedrivePolicy",
				deletionPolicy = SqsMessageDeletionPolicy.NO_REDRIVE)
		public void receiveThrowingException(String message) {
			throw new RuntimeException();
		}

		@RuntimeUse
		@SqsListener("DeadLetterQueue")
		public void receiveDeadLetters(String message) {
			this.countDownLatch.countDown();
		}

		@MessageExceptionHandler(RuntimeException.class)
		public void handle() {
			// Empty body just to avoid unnecessary log output because no exception
			// handler was found.
		}

	}

	public static class ManualDeletionPolicyTestListener {

		private final CountDownLatch countDownLatch = new CountDownLatch(1);

		@SqsListener(value = "ManualDeletionQueue",
				deletionPolicy = SqsMessageDeletionPolicy.NEVER)
		public void receive(String message, Acknowledgment acknowledgment)
				throws ExecutionException, InterruptedException {
			acknowledgment.acknowledge().get();
			this.countDownLatch.countDown();
		}

		public CountDownLatch getCountDownLatch() {
			return this.countDownLatch;
		}

	}

}
