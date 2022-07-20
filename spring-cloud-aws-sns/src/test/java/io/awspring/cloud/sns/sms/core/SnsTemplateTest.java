/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sns.sms.core;

import static io.awspring.cloud.sns.Matchers.requestMatches;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.awspring.cloud.sns.sms.attributes.AttributeCodes;
import io.awspring.cloud.sns.sms.attributes.SmsMessageAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
public class SnsTemplateTest {

	private final SnsClient snsClient = mock(SnsClient.class);

	private SnsSmsTemplate snsTemplate;

	@BeforeEach
	void init() {
		snsTemplate = new SnsSmsTemplate(snsClient);
	}

	@Test
	void sendsTextMessage() {
		snsTemplate.send("000 000 000", "this is message");

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.phoneNumber()).isEqualTo("000 000 000");
			assertThat(r.message()).isEqualTo("this is message");
		}));
	}

	@Test
	void sendsTextMessageWithAttributes() {
		snsTemplate.send("000 000 000", "this is message", SmsMessageAttributes.builder().messageGroupId("tst")
				.deduplicationId("3t").messageStructure("JSON").senderID("agent007").originationNumber("202").build());

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.phoneNumber()).isEqualTo("000 000 000");
			assertThat(r.message()).isEqualTo("this is message");
			assertThat(r.messageAttributes().get(AttributeCodes.SENDER_ID).stringValue()).isEqualTo("agent007");
			assertThat(r.messageAttributes().get(AttributeCodes.ORIGINATION_NUMBER).stringValue()).isEqualTo("202");
			assertThat(r.messageGroupId()).isEqualTo("tst");
			assertThat(r.messageDeduplicationId()).isEqualTo("3t");
			assertThat(r.messageStructure()).isEqualTo("JSON");
		}));
	}

	@Test
	void sendsTextMessageWithAttributes_targetArn() {
		snsTemplate.sendToTargetArn("arn:something:something", "this is message",
				SmsMessageAttributes.builder().messageGroupId("tst").deduplicationId("3t").messageStructure("JSON")
						.senderID("agent007").originationNumber("202").build());

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.targetArn()).isEqualTo("arn:something:something");
			assertThat(r.message()).isEqualTo("this is message");
			assertThat(r.messageAttributes().get(AttributeCodes.SENDER_ID).stringValue()).isEqualTo("agent007");
			assertThat(r.messageAttributes().get(AttributeCodes.ORIGINATION_NUMBER).stringValue()).isEqualTo("202");
			assertThat(r.messageGroupId()).isEqualTo("tst");
			assertThat(r.messageDeduplicationId()).isEqualTo("3t");
			assertThat(r.messageStructure()).isEqualTo("JSON");
		}));
	}

	@Test
	void sendsTextMessageWithAttributes_topicArn() {
		snsTemplate.sendToTopicArn("arn:something:something", "this is message",
				SmsMessageAttributes.builder().messageGroupId("tst").deduplicationId("3t").messageStructure("JSON")
						.senderID("agent007").originationNumber("202").build());

		verify(snsClient).publish(requestMatches(r -> {
			assertThat(r.topicArn()).isEqualTo("arn:something:something");
			assertThat(r.message()).isEqualTo("this is message");
			assertThat(r.messageAttributes().get(AttributeCodes.SENDER_ID).stringValue()).isEqualTo("agent007");
			assertThat(r.messageAttributes().get(AttributeCodes.ORIGINATION_NUMBER).stringValue()).isEqualTo("202");
			assertThat(r.messageGroupId()).isEqualTo("tst");
			assertThat(r.messageDeduplicationId()).isEqualTo("3t");
			assertThat(r.messageStructure()).isEqualTo("JSON");
		}));
	}

}
