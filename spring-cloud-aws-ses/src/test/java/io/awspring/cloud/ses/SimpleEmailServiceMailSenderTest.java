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
package io.awspring.cloud.ses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

/**
 * Tests for {@link SimpleEmailServiceMailSender}.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 */
class SimpleEmailServiceMailSenderTest {

	@Test
	void testSendSimpleMailWithMinimalProperties() {
		SesClient emailService = mock(SesClient.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService,
				"arn:aws:ses:us-east-1:00000000:identity/domain.com");

		SimpleMailMessage simpleMailMessage = createSimpleMailMessage();

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		assertThat(sendEmailRequest.source()).isEqualTo(simpleMailMessage.getFrom());
		assertThat(sendEmailRequest.destination().toAddresses().get(0))
				.isEqualTo(Objects.requireNonNull(simpleMailMessage.getTo())[0]);
		assertThat(sendEmailRequest.message().subject().data()).isEqualTo(simpleMailMessage.getSubject());
		assertThat(sendEmailRequest.message().body().text().data()).isEqualTo(simpleMailMessage.getText());
		assertThat(sendEmailRequest.destination().ccAddresses().size()).isEqualTo(0);
		assertThat(sendEmailRequest.destination().bccAddresses().size()).isEqualTo(0);
		assertThat(sendEmailRequest.sourceArn()).isEqualTo("arn:aws:ses:us-east-1:00000000:identity/domain.com");
	}

	@Test
	void testSendSimpleMailWithConfigurationSetNameSet() {
		SesClient emailService = mock(SesClient.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService, null,
				"Configuration Set");
		SimpleMailMessage simpleMailMessage = createSimpleMailMessage();
		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		assertThat(sendEmailRequest.configurationSetName()).isEqualTo("Configuration Set");
	}

	@Test
	void testSendSimpleMailWithCCandBCC() {
		SesClient emailService = mock(SesClient.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

		SimpleMailMessage simpleMailMessage = createSimpleMailMessage();
		simpleMailMessage.setBcc("bcc@domain.com");
		simpleMailMessage.setCc("cc@domain.com");

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		assertThat(sendEmailRequest.source()).isEqualTo(simpleMailMessage.getFrom());
		assertThat(sendEmailRequest.destination().toAddresses().get(0))
				.isEqualTo(Objects.requireNonNull(simpleMailMessage.getTo())[0]);
		assertThat(sendEmailRequest.message().subject().data()).isEqualTo(simpleMailMessage.getSubject());
		assertThat(sendEmailRequest.message().body().text().data()).isEqualTo(simpleMailMessage.getText());
		assertThat(sendEmailRequest.destination().ccAddresses().get(0))
				.isEqualTo(Objects.requireNonNull(simpleMailMessage.getCc())[0]);
		assertThat(sendEmailRequest.destination().bccAddresses().get(0))
				.isEqualTo(Objects.requireNonNull(simpleMailMessage.getBcc())[0]);
	}

	@Test
	void testSendSimpleMailWithNoTo() {
		SesClient emailService = mock(SesClient.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

		// Not using createSimpleMailMessage as we don't want the to address set.
		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom("sender@domain.com");
		simpleMailMessage.setSubject("message subject");
		simpleMailMessage.setText("message body");

		simpleMailMessage.setBcc("bcc@domain.com");

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		assertThat(sendEmailRequest.message().subject().data()).isEqualTo(simpleMailMessage.getSubject());
		assertThat(sendEmailRequest.message().body().text().data()).isEqualTo(simpleMailMessage.getText());
		assertThat(sendEmailRequest.destination().bccAddresses().get(0))
				.isEqualTo(Objects.requireNonNull(simpleMailMessage.getBcc())[0]);
	}

	@Test
	void testSendMultipleMails() {
		SesClient emailService = mock(SesClient.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());

		mailSender.send(createSimpleMailMessage(), createSimpleMailMessage());
		verify(emailService, times(2)).sendEmail(ArgumentMatchers.any(SendEmailRequest.class));
	}

	@Test
	void testSendMultipleMailsWithExceptionWhileSending() {
		SesClient emailService = mock(SesClient.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

		SimpleMailMessage firstMessage = createSimpleMailMessage();
		firstMessage.setBcc("bcc@domain.com");

		SimpleMailMessage failureMail = createSimpleMailMessage();
		when(emailService.sendEmail(ArgumentMatchers.isA(SendEmailRequest.class)))
				.thenReturn(SendEmailResponse.builder().build())
				.thenThrow(SesException.builder().message("error").build())
				.thenReturn(SendEmailResponse.builder().build());

		SimpleMailMessage thirdMessage = createSimpleMailMessage();

		try {
			mailSender.send(firstMessage, failureMail, thirdMessage);
			fail("Exception expected due to error while sending mail");
		}
		catch (MailSendException e) {
			assertThat(e.getFailedMessages().size()).isEqualTo(1);
			assertThat(e.getFailedMessages().containsKey(failureMail)).isTrue();
		}
	}

	@Test
	void testShutDownOfResources() {
		SesClient emailService = mock(SesClient.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

		mailSender.destroy();
		verify(emailService, times(1)).close();
	}

	private SimpleMailMessage createSimpleMailMessage() {
		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom("sender@domain.com");
		simpleMailMessage.setTo("receiver@domain.com");
		simpleMailMessage.setSubject("message subject");
		simpleMailMessage.setText("message body");
		return simpleMailMessage;
	}

}
