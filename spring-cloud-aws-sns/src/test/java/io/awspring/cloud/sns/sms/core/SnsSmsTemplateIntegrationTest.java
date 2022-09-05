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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;

import io.awspring.cloud.sns.sms.attributes.SmsMessageAttributes;
import io.awspring.cloud.sns.sms.attributes.SmsType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * @author Matej Nedic
 * @since 3.0.0
 */
@Testcontainers
class SnsSmsTemplateIntegrationTest {
	private static SnsSmsTemplate snsSmsTemplate;
	private static SnsClient snsClient;

	@Container
	static LocalStackContainer localstack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.3")).withServices(SNS).withEnv("DEBUG", "1");

	@BeforeAll
	public static void createSnsTemplate() {
		snsClient = SnsClient.builder().endpointOverride(localstack.getEndpointOverride(SNS))
				.region(Region.of(localstack.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("noop", "noop")))
				.build();
		snsSmsTemplate = new SnsSmsTemplate(snsClient);
	}

	@Test
	void sendValidMessage_ToPhoneNumber() {
		Assertions
				.assertDoesNotThrow(() -> snsSmsTemplate.send("+385 00 000 0000", "Spring Cloud AWS got you covered!"));

		await().untilAsserted(() -> {
			String logs = localstack.getLogs(OutputFrame.OutputType.STDOUT, OutputFrame.OutputType.STDERR);
			assertThat(logs)
					.contains("Publishing message to TopicArn: None | Message: Spring Cloud AWS got you covered!");
		});
	}

	@Test
	void sendValidMessage_ToPhoneNumber_WithAttributes() {
		Assertions.assertDoesNotThrow(
				() -> snsSmsTemplate.send("+385 00 000 0000", "Spring Cloud AWS got you covered!", SmsMessageAttributes
						.builder().smsType(SmsType.PROMOTIONAL).senderID("AWSPRING").maxPrice("1.00").build()));

		await().untilAsserted(() -> {
			String logs = localstack.getLogs(OutputFrame.OutputType.STDOUT, OutputFrame.OutputType.STDERR);
			assertThat(logs)
					.contains("Publishing message to TopicArn: None | Message: Spring Cloud AWS got you covered!");
		});
	}

}
