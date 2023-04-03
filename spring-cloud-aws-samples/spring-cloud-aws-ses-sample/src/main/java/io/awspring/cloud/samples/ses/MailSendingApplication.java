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

import java.io.File;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.VerifyEmailAddressRequest;

/**
 * A sample application to demonstrate sending simple emails and emails with attachments.
 * <p>
 * To run this sample application, you need to do either of the following.
 * <ul>
 * <li>If you wish to send emails to a real email ID, you need to verify identities as mentioned in
 * <a href="https://docs.aws.amazon.com/ses/latest/dg/creating-identities.html">the Amazon Simple Email Service
 * docs.</a> After you do that, simply update this sample app with email IDs that you have verified with AWS.</li>
 * <li>If you wish to just test out email sending capability in a test environment, you can do so by running localstack.
 * Just issue the following command from the root of the `spring-cloud-aws-ses-sample`:
 *
 * <pre>
 * docker-compose -f docker-compose.yml up -d
 * </pre>
 *
 * See more information on localstack see <a href="https://docs.localstack.cloud/getting-started/">here</a> and
 * <a href="https://docs.localstack.cloud/user-guide/aws/ses/">here</a>.</li>
 * </ul>
 * </p>
 *
 */
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
			sendAnEmailWithAttachment(mailSender, sesClient);
			sendHtmlEmail(mailSender, sesClient);
			// check localstack logs for sent email, if you use localstack for running this sample
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

	/**
	 * To send emails with attachments, you must provide the Java Mail API and an implementation of the API in the
	 * classpath. See the dependencies provided in this sample app. If you don't provider an implementation of the Java
	 * Mail API, you would get the following exception at runtime.
	 * 
	 * <pre>
	 * java.lang.IllegalStateException: Not provider of jakarta.mail.util.StreamProvider was found
	 * </pre>
	 *
	 * @param mailSender A {@link JavaMailSender}.
	 * @param sesClient An {@link SesClient}.
	 */
	public static void sendAnEmailWithAttachment(MailSender mailSender, SesClient sesClient) {
		// e-mail address has to verified before we email it. If it is not verified SES will return error.
		sesClient.verifyEmailAddress(VerifyEmailAddressRequest.builder().emailAddress(RECIPIENT).build());
		sesClient.verifyEmailAddress(VerifyEmailAddressRequest.builder().emailAddress(SENDER).build());

		// A JavaMailSender is needed. Spring Cloud AWS SES automatically configures a JavaMailSender when it finds
		// the Java Mail API in the classpath. At runtime, an implementation of teh Java Mail API must also be
		// available.
		JavaMailSender javaMailSender = (JavaMailSender) mailSender;
		javaMailSender.send(mimeMessage -> {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			helper.addTo(RECIPIENT);
			helper.setFrom(SENDER);
			File resource = new ClassPathResource("answer.txt").getFile();
			helper.addAttachment("answer.txt", resource.getAbsoluteFile());
			helper.setSubject("What is the meaning of life, the universe, and everything?");
			helper.setText("Open the attached file for the answer you are seeking", false);
		});
	}

	/**
	 * To send HTML emails, you must provide the Java Mail API and an implementation of the API in the classpath. See
	 * the dependencies provided in this sample app. If you don't provider an implementation of the Java Mail API, you
	 * would get the following exception at runtime.
	 * 
	 * <pre>
	 * java.lang.IllegalStateException: Not provider of jakarta.mail.util.StreamProvider was found
	 * </pre>
	 *
	 * @param mailSender A {@link JavaMailSender}.
	 * @param sesClient An {@link SesClient}.
	 */
	public static void sendHtmlEmail(MailSender mailSender, SesClient sesClient) {
		// e-mail address has to verified before we email it. If it is not verified SES will return error.
		sesClient.verifyEmailAddress(VerifyEmailAddressRequest.builder().emailAddress(RECIPIENT).build());
		sesClient.verifyEmailAddress(VerifyEmailAddressRequest.builder().emailAddress(SENDER).build());

		// A JavaMailSender is needed. Spring Cloud AWS SES automatically configures a JavaMailSender when it finds
		// the Java Mail API in the classpath. At runtime, an implementation of teh Java Mail API must also be
		// available.
		JavaMailSender javaMailSender = (JavaMailSender) mailSender;
		javaMailSender.send(mimeMessage -> {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			helper.addTo(RECIPIENT);
			helper.setFrom(SENDER);
			helper.setSubject("What is the meaning of life, the universe, and everything?");
			String htmlMessage = """
					<h2>What is the meaning of life, the universe, and everything?</h2>
					<h3>42</h3>
					""";
			helper.setText(htmlMessage, true);
		});
	}
}
