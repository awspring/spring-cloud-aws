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

package org.springframework.cloud.aws.messaging;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.support.documentation.RuntimeUse;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationMessage;
import org.springframework.cloud.aws.messaging.config.annotation.NotificationSubject;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alain Sahli
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class NotificationMessagingTemplateIntegrationTest
		extends AbstractContainerTest {

	@Autowired
	private NotificationMessagingTemplate notificationMessagingTemplate;

	@Autowired
	private NotificationReceiver notificationReceiver;

	@Before
	public void resetMocks() throws Exception {
		this.notificationReceiver.reset();
	}

	@Test
	public void send_validTextMessage_shouldBeDelivered() throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		this.notificationReceiver.setCountDownLatch(countDownLatch);
		String subject = "A subject";

		// Act
		this.notificationMessagingTemplate.sendNotification("SqsReceivingSnsTopic",
				new TestPerson("Agim", "Emruli"), subject);

		// Assert
		assertTrue(countDownLatch.await(10, TimeUnit.SECONDS));
		assertEquals("Agim", this.notificationReceiver.getMessage().getFirstName());
		assertEquals("Emruli", this.notificationReceiver.getMessage().getLastName());
		assertEquals(subject, this.notificationReceiver.getSubject());
	}

	@Test
	public void send_validTextMessageWithoutDestination_shouldBeDeliveredToDefaultDestination()
			throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		this.notificationReceiver.setCountDownLatch(countDownLatch);
		String subject = "Hello default destination";

		// Act
		this.notificationMessagingTemplate
				.sendNotification(new TestPerson("Agim", "Emruli"), subject);

		// Assert
		assertTrue(countDownLatch.await(10, TimeUnit.SECONDS));
		assertEquals("Agim", this.notificationReceiver.getMessage().getFirstName());
		assertEquals("Emruli", this.notificationReceiver.getMessage().getLastName());
		assertEquals(subject, this.notificationReceiver.getSubject());
	}

	@RuntimeUse
	protected static class NotificationReceiver {

		private CountDownLatch countDownLatch;

		private TestPerson message;

		private String subject;

		private void setCountDownLatch(CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
		}

		private TestPerson getMessage() {
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
		@SqsListener("NotificationQueue")
		private void messageListener(@NotificationSubject String subject,
				@NotificationMessage TestPerson message) {
			this.subject = subject;
			this.message = message;
			this.countDownLatch.countDown();
		}

	}

	public static class TestPerson {

		private String firstName;

		private String lastName;

		public TestPerson() {
		}

		public TestPerson(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return this.lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

	}

}
