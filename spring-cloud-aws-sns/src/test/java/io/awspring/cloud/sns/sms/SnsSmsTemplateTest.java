/*
 * Copyright 2013-2021 the original author or authors.
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
package io.awspring.cloud.sns.sms;

import static io.awspring.cloud.sns.Matchers.requestMatches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * @author Matej Nedic
 * @since 3.0
 */
class SnsSmsTemplateTest {

	private static final String TARGET_ARN = "arn:aws:sns:eu-west:123456789012:test";

	private final SnsClient snsClient = mock(SnsClient.class);

	private SnsSmsTemplate snsSmsTemplate;

	@BeforeEach
	void init() {
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setSerializedPayloadClass(String.class);
		snsSmsTemplate = new SnsSmsTemplate(snsClient, converter);
	}

	@Test
	void sendSmsNotificationToPhoneNumber_withHeaders() {
		SnsSmsNotification<String> snsSmsNotification = SnsSmsNotification.builder("message").smsType("Transactional")
				.senderId("AWSPRING").header("AWS.SNS.MOBILE.APNS.PUSH_TYPE", "background").build();
		snsSmsTemplate.sendNotification("+000000000000", snsSmsNotification);

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.phoneNumber()).isEqualTo("+000000000000");
			assertHeadersAndMessage(r, "message");
		}));
	}

	@Test
	void sendSmsNotificationToPublisherApplication_withHeaders() {
		SnsSmsNotification<String> snsSmsNotification = SnsSmsNotification.builder("message").smsType("Transactional")
				.senderId("AWSPRING").header("AWS.SNS.MOBILE.APNS.PUSH_TYPE", "background").build();
		snsSmsTemplate.sendNotification(TARGET_ARN, snsSmsNotification);

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.targetArn()).isEqualTo(TARGET_ARN);
			assertHeadersAndMessage(r, "message");
		}));
	}

	@Test
	void sendsComplexSnsNotification_PlatformApplication() {
		SnsSmsNotification<Person> snsSmsNotification = SnsSmsNotification.builder(new Person("foo"))
				.smsType("Transactional").senderId("AWSPRING").header("AWS.SNS.MOBILE.APNS.PUSH_TYPE", "background")
				.build();
		snsSmsTemplate.sendNotification(TARGET_ARN, snsSmsNotification);

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.targetArn()).isEqualTo(TARGET_ARN);
			assertHeadersAndMessage(r, "{\"name\":\"foo\"}");
		}));
	}

	@Test
	void sendsComplexSnsNotification_PhoneNumber() {
		SnsSmsNotification<Person> snsSmsNotification = SnsSmsNotification.builder(new Person("foo"))
				.smsType("Transactional").senderId("AWSPRING").header("AWS.SNS.MOBILE.APNS.PUSH_TYPE", "background")
				.build();
		snsSmsTemplate.sendNotification("+000000000000", snsSmsNotification);

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.phoneNumber()).isEqualTo("+000000000000");
			assertHeadersAndMessage(r, "{\"name\":\"foo\"}");
		}));
	}

	private void assertHeadersAndMessage(PublishRequest r, String message) {
		assertThat(r.message()).isEqualTo(message);
		assertThat(r.messageAttributes()).containsEntry(SnsSmsHeaders.DEFAULT_SMS_TYPE,
				MessageAttributeValue.builder().stringValue("Transactional").dataType("String").build());
		assertThat(r.messageAttributes()).containsEntry(SnsSmsHeaders.DEFAULT_SENDER_ID,
				MessageAttributeValue.builder().stringValue("AWSPRING").dataType("String").build());
		assertThat(r.messageAttributes()).containsEntry("AWS.SNS.MOBILE.APNS.PUSH_TYPE",
				MessageAttributeValue.builder().stringValue("background").dataType("String").build());
		assertThat(r.messageAttributes().size()).isEqualTo(6);
	}

	static class Person {
		private final String name;

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

}
