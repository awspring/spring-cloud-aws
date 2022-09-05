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

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.VerifyEmailAddressRequest;

@SpringBootApplication
public class MailSendingApplication {
	private static final String SENDER = "something@foo.bar";
	private static final String RECIPIENT = "someMail@foo.bar";

	public static void main(String[] args) {
		SpringApplication.run(MailSendingApplication.class, args);
	}

	@Bean
	ApplicationRunner applicationRunner(MailSender mailSender, SesClient sesClient) {
		return args -> {
			sendAnEmail(mailSender, sesClient);
			// check localstack logs for sent email
		};
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
