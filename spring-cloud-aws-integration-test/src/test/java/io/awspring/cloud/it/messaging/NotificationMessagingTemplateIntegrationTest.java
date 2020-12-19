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

package io.awspring.cloud.it.messaging;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import io.awspring.cloud.core.support.documentation.RuntimeUse;
import io.awspring.cloud.messaging.config.annotation.NotificationMessage;
import io.awspring.cloud.messaging.config.annotation.NotificationSubject;
import io.awspring.cloud.messaging.core.NotificationMessagingTemplate;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Alain Sahli
 */
@ExtendWith(SpringExtension.class)
public abstract class NotificationMessagingTemplateIntegrationTest extends AbstractContainerTest {

	@Autowired
	private NotificationMessagingTemplate notificationMessagingTemplate;

	@Autowired
	private NotificationReceiver notificationReceiver;

	@BeforeEach
	void resetMocks() throws Exception {
		this.notificationReceiver.reset();
	}

	@Test
	void send_validTextMessage_shouldBeDelivered() throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		this.notificationReceiver.setCountDownLatch(countDownLatch);
		String subject = "A subject";

		// Act
		this.notificationMessagingTemplate.sendNotification("SqsReceivingSnsTopic", new TestPerson("Agim", "Emruli"),
				subject);

		// Assert
		await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
			assertThat(this.notificationReceiver.getMessage()).isNotNull();
			assertThat(this.notificationReceiver.getMessage().getFirstName()).isEqualTo("Agim");
			assertThat(this.notificationReceiver.getMessage().getLastName()).isEqualTo("Emruli");
			assertThat(this.notificationReceiver.getSubject()).isEqualTo(subject);
		});
	}

	@Test
	void send_validTextMessageWithoutDestination_shouldBeDeliveredToDefaultDestination() throws Exception {
		// Arrange
		CountDownLatch countDownLatch = new CountDownLatch(1);
		this.notificationReceiver.setCountDownLatch(countDownLatch);
		String subject = "Hello default destination";

		// Act
		this.notificationMessagingTemplate.sendNotification(new TestPerson("Agim", "Emruli"), subject);

		// Assert
		await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> {
			assertThat(this.notificationReceiver.getMessage()).isNotNull();
			assertThat(this.notificationReceiver.getMessage().getFirstName()).isEqualTo("Agim");
			assertThat(this.notificationReceiver.getMessage().getLastName()).isEqualTo("Emruli");
			assertThat(this.notificationReceiver.getSubject()).isEqualTo(subject);
		});
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
		private void messageListener(@NotificationSubject String subject, @NotificationMessage TestPerson message) {
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
