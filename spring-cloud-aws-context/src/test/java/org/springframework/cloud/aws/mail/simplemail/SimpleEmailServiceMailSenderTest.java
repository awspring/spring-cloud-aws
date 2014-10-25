/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.mockito.Matchers;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests for class SimpleEmailServiceMailSender
 */
public class SimpleEmailServiceMailSenderTest {


	@Test
	public void testSendSimpleMailWithMinimalProperties() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

		SimpleMailMessage simpleMailMessage = createSimpleMailMessage();

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture())).thenReturn(new SendEmailResult().withMessageId("123"));

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		assertEquals(simpleMailMessage.getFrom(), sendEmailRequest.getSource());
		assertEquals(simpleMailMessage.getTo()[0], sendEmailRequest.getDestination().getToAddresses().get(0));
		assertEquals(simpleMailMessage.getSubject(), sendEmailRequest.getMessage().getSubject().getData());
		assertEquals(simpleMailMessage.getText(), sendEmailRequest.getMessage().getBody().getText().getData());
		assertEquals(0, sendEmailRequest.getDestination().getCcAddresses().size());
		assertEquals(0, sendEmailRequest.getDestination().getBccAddresses().size());
	}

	@Test
	public void testSendSimpleMailWithCCandBCC() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

		SimpleMailMessage simpleMailMessage = createSimpleMailMessage();
		simpleMailMessage.setBcc("bcc@domain.com");
		simpleMailMessage.setCc("cc@domain.com");

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture())).thenReturn(new SendEmailResult().withMessageId("123"));

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		assertEquals(simpleMailMessage.getFrom(), sendEmailRequest.getSource());
		assertEquals(simpleMailMessage.getTo()[0], sendEmailRequest.getDestination().getToAddresses().get(0));
		assertEquals(simpleMailMessage.getSubject(), sendEmailRequest.getMessage().getSubject().getData());
		assertEquals(simpleMailMessage.getText(), sendEmailRequest.getMessage().getBody().getText().getData());
		assertEquals(simpleMailMessage.getBcc()[0], sendEmailRequest.getDestination().getBccAddresses().get(0));
		assertEquals(simpleMailMessage.getCc()[0], sendEmailRequest.getDestination().getCcAddresses().get(0));
	}

	@Test
	public void testSendMultipleMails() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture())).thenReturn(new SendEmailResult().withMessageId("123"));

		mailSender.send(new SimpleMailMessage[]{createSimpleMailMessage(), createSimpleMailMessage()});
		verify(emailService, times(2)).sendEmail(Matchers.any(SendEmailRequest.class));
	}

	@Test
	public void testSendMultipleMailsWithExceptionWhileSending() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

		SimpleMailMessage firstMessage = createSimpleMailMessage();
		firstMessage.setBcc("bcc@domain.com");

		SimpleMailMessage failureMail = createSimpleMailMessage();
		when(emailService.sendEmail(Matchers.isA(SendEmailRequest.class))).
				thenReturn(new SendEmailResult()).
				thenThrow(new AmazonClientException("error")).
				thenReturn(new SendEmailResult());

		SimpleMailMessage thirdMessage = createSimpleMailMessage();

		try {
			mailSender.send(new SimpleMailMessage[]{firstMessage, failureMail, thirdMessage});
			fail("Exception expected due to error while sending mail");
		} catch (MailSendException e) {
			assertEquals(1, e.getFailedMessages().size());
			assertTrue(e.getFailedMessages().containsKey(failureMail));
		}
	}

	@Test
	public void testShutDownOfResources() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = new SimpleEmailServiceMailSender(emailService);

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
