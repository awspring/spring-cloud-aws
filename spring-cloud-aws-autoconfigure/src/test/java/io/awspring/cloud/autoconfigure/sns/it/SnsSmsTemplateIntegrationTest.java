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
package io.awspring.cloud.autoconfigure.sns.it;

import static io.awspring.cloud.sns.sms.SnsSmsHeaders.DEFAULT_SENDER_ID;
import static io.awspring.cloud.sns.sms.SnsSmsHeaders.DEFAULT_SMS_TYPE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;

import io.awspring.cloud.sns.sms.SnsSmsTemplate;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for {@link SnsSmsTemplate}.
 *
 * @author Matej Nedic
 * @since 3.0
 */
@Testcontainers
@SpringBootTest()
class SnsSmsTemplateIntegrationTest {

	@Autowired
	private SnsSmsTemplate snsSmsTemplate;

	private static final String REGION = "eu-west-1";

	private static HashMap<String, Object> smsAttributeValues = new HashMap<>();

	{
		smsAttributeValues.put(DEFAULT_SMS_TYPE, "Transactional");
		smsAttributeValues.put(DEFAULT_SENDER_ID, "AWSPRING");
		smsAttributeValues.put("AWS.SNS.MOBILE.APNS.PUSH_TYPE", "background");
	}
	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withServices(SNS).withReuse(true);

	@DynamicPropertySource
	static void snsProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.aws.sns.region", REGION::toString);
		registry.add("spring.cloud.aws.sns.endpoint", localstack.getEndpointOverride(SNS)::toString);
		registry.add("spring.cloud.aws.credentials.access-key", "noop"::toString);
		registry.add("spring.cloud.aws.credentials.secret-key", "noop"::toString);
		registry.add("spring.cloud.aws.region.static", "eu-west-1"::toString);
	}

	@Test
	void send_validTextMessage_useSmsTopicMessageChannel_send_phoneNumber() {
		assertThatCode(() -> snsSmsTemplate.sendMessage("+000000000000", "message")).doesNotThrowAnyException();
	}

	@Test
	void send_validTextMessage_useSmsTopicMessageChannel_send_phoneNumber_with_Headers() {
		assertThatCode(() -> snsSmsTemplate.sendMessage("+000000000000", "Hello", smsAttributeValues))
				.doesNotThrowAnyException();
	}

	@Test
	void send_validTextMessage_useSmsNotification_send_phoneNumber_with_Headers() {
		assertThatCode(() -> snsSmsTemplate.sendMessage("+000000000000", "message", smsAttributeValues))
				.doesNotThrowAnyException();
	}

	@Test
	void send_validObject_useSmsNotification_send_phoneNumber_with_Headers() {
		assertThatCode(() -> snsSmsTemplate.sendMessage("+000000000000", new Person("foo"), smsAttributeValues))
				.doesNotThrowAnyException();
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
