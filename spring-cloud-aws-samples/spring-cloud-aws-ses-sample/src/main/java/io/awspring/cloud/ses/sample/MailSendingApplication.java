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
package io.awspring.cloud.ses.sample;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SES;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.VerifyEmailAddressRequest;

@SpringBootApplication
public class MailSendingApplication {

	Logger LOG = LoggerFactory.getLogger(MailSendingApplication.class);

	// DEBUG must be set as env variable to get log for sending an email since Localstack is mocking email server.
	private static final LocalStackContainer localStack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.0")).withEnv(Collections.singletonMap("DEBUG", "1"))
					.withServices(SES);

	public static void main(String[] args) {
		localStack.start();
		System.setProperty("spring.cloud.aws.ses.region", localStack.getRegion());
		System.setProperty("spring.cloud.aws.ses.endpoint", localStack.getEndpointOverride(SES).toString());
		System.setProperty("spring.cloud.aws.credentials.access-key", "test");
		System.setProperty("spring.cloud.aws.credentials.secret-key", "test");
		SpringApplication.run(MailSendingApplication.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(MailSender mailSender, SesClient sesClient) {
		return args -> {
			sesClient.verifyEmailAddress(
					VerifyEmailAddressRequest.builder().emailAddress("matejnedic1@gmail.com").build());
			SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
			simpleMailMessage.setFrom("matejnedic1@gmail.com");
			simpleMailMessage.setTo("matejnedic1@gmail.com");
			simpleMailMessage.setSubject("test subject");
			simpleMailMessage.setText("test content");
			mailSender.send(simpleMailMessage);

			// sleep 2 seconds so localstack logs show up. Awatility can be used instead to improve.
			Thread.sleep(2000);
			// last 6 lines will be log in SES localstack mocked mail server about sending email
			List<String> logs = Arrays.asList(localStack.getLogs().split("\n"));
			LOG.info(String.join("\n", (logs.subList(Math.max(0, logs.size() - 6), logs.size()))));
		};
	}

}
