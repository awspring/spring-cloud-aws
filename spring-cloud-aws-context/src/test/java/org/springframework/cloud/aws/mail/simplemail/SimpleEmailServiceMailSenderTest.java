/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.mail.simplemail;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for class SimpleEmailServiceMailSender.
 */
public class SimpleEmailServiceMailSenderTest {

	@Test
	public void testSendSimpleMailWithMinimalProperties() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(
				emailService);

		SimpleMailMessage simpleMailMessage = createSimpleMailMessage();

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor
				.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(new SendEmailResult().withMessageId("123"));

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		assertThat(sendEmailRequest.getSource()).isEqualTo(simpleMailMessage.getFrom());
		assertThat(sendEmailRequest.getDestination().getToAddresses().get(0))
				.isEqualTo(simpleMailMessage.getTo()[0]);
		assertThat(sendEmailRequest.getMessage().getSubject().getData())
				.isEqualTo(simpleMailMessage.getSubject());
		assertThat(sendEmailRequest.getMessage().getBody().getText().getData())
				.isEqualTo(simpleMailMessage.getText());
		assertThat(sendEmailRequest.getDestination().getCcAddresses().size())
				.isEqualTo(0);
		assertThat(sendEmailRequest.getDestination().getBccAddresses().size())
				.isEqualTo(0);
	}

	@Test
	public void testSendSimpleMailWithCCandBCC() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(
				emailService);

		SimpleMailMessage simpleMailMessage = createSimpleMailMessage();
		simpleMailMessage.setBcc("bcc@domain.com");
		simpleMailMessage.setCc("cc@domain.com");

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor
				.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(new SendEmailResult().withMessageId("123"));

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		assertThat(sendEmailRequest.getSource()).isEqualTo(simpleMailMessage.getFrom());
		assertThat(sendEmailRequest.getDestination().getToAddresses().get(0))
				.isEqualTo(simpleMailMessage.getTo()[0]);
		assertThat(sendEmailRequest.getMessage().getSubject().getData())
				.isEqualTo(simpleMailMessage.getSubject());
		assertThat(sendEmailRequest.getMessage().getBody().getText().getData())
				.isEqualTo(simpleMailMessage.getText());
		assertThat(sendEmailRequest.getDestination().getBccAddresses().get(0))
				.isEqualTo(simpleMailMessage.getBcc()[0]);
		assertThat(sendEmailRequest.getDestination().getCcAddresses().get(0))
				.isEqualTo(simpleMailMessage.getCc()[0]);
	}

	@Test
	public void testSendMultipleMails() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(
				emailService);

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor
				.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(new SendEmailResult().withMessageId("123"));

		mailSender.send(createSimpleMailMessage(), createSimpleMailMessage());
		verify(emailService, times(2))
				.sendEmail(ArgumentMatchers.any(SendEmailRequest.class));
	}

	@Test
	public void testSendMultipleMailsWithExceptionWhileSending() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(
				emailService);

		SimpleMailMessage firstMessage = createSimpleMailMessage();
		firstMessage.setBcc("bcc@domain.com");

		SimpleMailMessage failureMail = createSimpleMailMessage();
		when(emailService.sendEmail(ArgumentMatchers.isA(SendEmailRequest.class)))
				.thenReturn(new SendEmailResult())
				.thenThrow(new AmazonClientException("error"))
				.thenReturn(new SendEmailResult());

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
	public void testShutDownOfResources() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(
				emailService);

		mailSender.destroy();
		verify(emailService, times(1)).shutdown();
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
