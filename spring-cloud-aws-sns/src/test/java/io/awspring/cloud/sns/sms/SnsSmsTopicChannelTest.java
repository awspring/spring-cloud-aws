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
import static io.awspring.cloud.sns.sms.SnsSmsHeaders.DEFAULT_SENDER_ID;
import static io.awspring.cloud.sns.sms.SnsSmsHeaders.DEFAULT_SMS_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

class SnsSmsTopicChannelTest {

	private static final String TARGET_ARN = "arn:aws:sns:eu-west:123456789012:test";
	private final SnsClient snsClient = mock(SnsClient.class);
	private final MessageChannel messageChannel = new SnsSmsTopicChannel(snsClient, TARGET_ARN, null);

	@Test
	void sendSMS_withHeaders() {
		// Arrange
		MessageAttributeValue.Builder builder = MessageAttributeValue.builder().dataType("String");
		String appleNotificationType = "AWS.SNS.MOBILE.APNS.PUSH_TYPE";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(DEFAULT_SENDER_ID, "AWSPRING")
				.setHeader(DEFAULT_SMS_TYPE, "Transactional").setHeader(appleNotificationType, "background").build();

		// Act
		boolean sent = messageChannel.send(message);

		// Assert
		assertThat(sent).isTrue();
		verify(snsClient).publish(requestMatches(it -> {
			assertThat(it.message()).isEqualTo("Hello");
			assertThat(it.targetArn()).isEqualTo(TARGET_ARN);
			assertThat(it.messageAttributes()).containsEntry(DEFAULT_SENDER_ID,
					builder.stringValue("AWSPRING").build());
			assertThat(it.messageAttributes()).containsEntry(DEFAULT_SMS_TYPE,
					builder.stringValue("Transactional").build());
			assertThat(it.messageAttributes()).containsEntry(appleNotificationType,
					builder.stringValue("background").build());
		}));
	}

	@Test
	void sendSMS_withHeaders_andPhoneNumber() {
		// Arrange
		MessageAttributeValue.Builder builder = MessageAttributeValue.builder().dataType("String");
		String appleNotificationType = "AWS.SNS.MOBILE.APNS.PUSH_TYPE";
		Message<String> message = MessageBuilder.withPayload("Hello").setHeader(DEFAULT_SENDER_ID, "AWSPRING")
				.setHeader(DEFAULT_SMS_TYPE, "Transactional").setHeader(appleNotificationType, "background").build();

		// Act
		boolean sent = new SnsSmsTopicChannel(snsClient, null, "+000000000000").send(message);

		// Assert
		assertThat(sent).isTrue();
		verify(snsClient).publish(requestMatches(it -> {
			assertThat(it.message()).isEqualTo("Hello");
			assertThat(it.phoneNumber()).isEqualTo("+000000000000");
			assertThat(it.messageAttributes()).containsEntry(DEFAULT_SENDER_ID,
					builder.stringValue("AWSPRING").build());
			assertThat(it.messageAttributes()).containsEntry(DEFAULT_SMS_TYPE,
					builder.stringValue("Transactional").build());
			assertThat(it.messageAttributes()).containsEntry(appleNotificationType,
					builder.stringValue("background").build());
		}));
	}

}
