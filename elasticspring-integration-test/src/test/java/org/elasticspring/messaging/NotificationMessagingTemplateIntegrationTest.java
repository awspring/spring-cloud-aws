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

package org.elasticspring.messaging;

import org.elasticspring.core.support.documentation.RuntimeUse;
import org.elasticspring.messaging.config.annotation.NotificationMessage;
import org.elasticspring.messaging.config.annotation.NotificationSubject;
import org.elasticspring.messaging.core.NotificationMessagingTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alain Sahli
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class NotificationMessagingTemplateIntegrationTest {

	@Autowired
	private NotificationMessagingTemplate notificationMessagingTemplate;

	@Autowired
	private NotificationReceiver notificationReceiver;

	@Before
	public void setUp() throws Exception {
		this.notificationReceiver.reset();
	}

	@Test
	public void send_validTextMessage_shouldBeDelivered() throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		this.notificationReceiver.setCountDownLatch(countDownLatch);
		String message = "Message content for SQS";

		// Act
		this.notificationMessagingTemplate.sendNotification("SqsReceivingSnsTopic", message, "A subject");

		// Assert
		assertTrue(countDownLatch.await(10, TimeUnit.SECONDS));
		assertEquals(message, this.notificationReceiver.getMessage());
		assertEquals("A subject", this.notificationReceiver.getSubject());
	}

	@RuntimeUse
	private static class NotificationReceiver {

		private CountDownLatch countDownLatch;
		private String message;
		private String subject;

		private void setCountDownLatch(CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
		}

		private String getMessage() {
			return this.message;
		}

		private String getSubject() {
			return this.subject;
		}

		private void reset() {
			this.message = null;
			this.subject = null;
		}

		@RuntimeUse
		@MessageMapping("NotificationQueue")
		private void messageListener(@NotificationSubject String subject, @NotificationMessage String message) {
			this.subject = subject;
			this.message = message;
			this.countDownLatch.countDown();
		}

	}

}
