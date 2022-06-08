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
package io.awspring.cloud.samples.ses;

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

/*
	We are using Localstack for SES. Since localstack does not support sending and email but rather uses MOTO which mocks sending an email.
	Only way to see that integration works is then to print logs of mail being sent.
	More about MOTO https://github.com/spulec/moto

	Other problem is that if we user real AWS SES service we would have to verify mail first, which would not be most convenient way to showcase how integration works.
	Users would have to manually add their emails to CDK and change the code.
 */
@SpringBootApplication
public class MailSendingApplication {

	private static final String SENDER = "something@foo.bar";
	private static final String RECIPIENT = "someMail@foo.bar";
	private static final Logger LOGGER = LoggerFactory.getLogger(MailSendingApplication.class);

	/*
	 * DEBUG must be set as env variable to get log for sending an email since Moto is only writing Mail has been sent
	 * logs in debug mode.
	 */
	private static final LocalStackContainer localStack = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:0.14.2")).withEnv(Collections.singletonMap("DEBUG", "1"))
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
			sendAnEmail(mailSender, sesClient);
			printEmailBeingSentMockFromLogs();

		};
	}

	public static void printEmailBeingSentMockFromLogs() throws InterruptedException {
		// sleep 2 seconds so localstack logs show up.
		Thread.sleep(2000);
		// last 6 lines will be log in SES Localstack will be about how mail is sent.
		List<String> logs = Arrays.asList(localStack.getLogs().split("\n"));
		LOGGER.info(String.join("\n", (logs.subList(Math.max(0, logs.size() - 6), logs.size()))));
	}

	public static void sendAnEmail(MailSender mailSender, SesClient sesClient) {
		// e-mail address has to verified before we email it. If it is not verified SES will return error.
		sesClient.verifyEmailAddress(VerifyEmailAddressRequest.builder().emailAddress(RECIPIENT).build());
		sesClient.verifyEmailAddress(VerifyEmailAddressRequest.builder().emailAddress(SENDER).build());

		// SimpleMailMessage is created, and we use MailSender bean which is autoconfigured to send an email through
		// SES.
		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom(SENDER);
		simpleMailMessage.setTo(RECIPIENT);
		simpleMailMessage.setSubject("test subject");
		simpleMailMessage.setText("test content");
		mailSender.send(simpleMailMessage);
	}

}
