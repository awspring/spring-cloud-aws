/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.mail.simplemail;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;


/**
 * Tests for class SimpleEmailServiceMailSender
 */
public class SimpleEmailServiceMailSenderTest {


	@Test
	public void testSendSimpleMailWithMinimalProperties() throws Exception {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = createMailSender("access","secret",emailService);

		SimpleMailMessage simpleMailMessage = createSimpleMailMessage();

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		Mockito.when(emailService.sendEmail(request.capture())).thenReturn(new SendEmailResult().withMessageId("123"));

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		Assert.assertEquals(simpleMailMessage.getFrom(), sendEmailRequest.getSource());
		Assert.assertEquals(simpleMailMessage.getTo()[0], sendEmailRequest.getDestination().getToAddresses().get(0));
		Assert.assertEquals(simpleMailMessage.getSubject(), sendEmailRequest.getMessage().getSubject().getData());
		Assert.assertEquals(simpleMailMessage.getText(), sendEmailRequest.getMessage().getBody().getText().getData());
		Assert.assertEquals(0, sendEmailRequest.getDestination().getCcAddresses().size());
		Assert.assertEquals(0, sendEmailRequest.getDestination().getBccAddresses().size());
	}

	@Test
	public void testSendSimpleMailWithCCandBCC() throws Exception {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = createMailSender("access","secret",emailService);

		SimpleMailMessage simpleMailMessage = createSimpleMailMessage();
		simpleMailMessage.setBcc("bcc@domain.com");
		simpleMailMessage.setCc("cc@domain.com");

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		Mockito.when(emailService.sendEmail(request.capture())).thenReturn(new SendEmailResult().withMessageId("123"));

		mailSender.send(simpleMailMessage);

		SendEmailRequest sendEmailRequest = request.getValue();
		Assert.assertEquals(simpleMailMessage.getFrom(), sendEmailRequest.getSource());
		Assert.assertEquals(simpleMailMessage.getTo()[0], sendEmailRequest.getDestination().getToAddresses().get(0));
		Assert.assertEquals(simpleMailMessage.getSubject(), sendEmailRequest.getMessage().getSubject().getData());
		Assert.assertEquals(simpleMailMessage.getText(), sendEmailRequest.getMessage().getBody().getText().getData());
		Assert.assertEquals(simpleMailMessage.getBcc()[0], sendEmailRequest.getDestination().getBccAddresses().get(0));
		Assert.assertEquals(simpleMailMessage.getCc()[0], sendEmailRequest.getDestination().getCcAddresses().get(0));
	}

	@Test
	public void testSendMultipleMails() throws Exception {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = createMailSender("access","secret",emailService);

		mailSender.send(new SimpleMailMessage[]{createSimpleMailMessage(), createSimpleMailMessage()});
		Mockito.verify(emailService, Mockito.times(2)).sendEmail(Matchers.any(SendEmailRequest.class));
	}

	@Test
	public void testSendMultipleMailsWithExceptionWhileSending() throws Exception {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = createMailSender("access","secret",emailService);

		SimpleMailMessage firstMessage = createSimpleMailMessage();
		firstMessage.setBcc("bcc@domain.com");

		SimpleMailMessage failureMail = createSimpleMailMessage();
		Mockito.when(emailService.sendEmail(Matchers.isA(SendEmailRequest.class))).
		thenReturn(new SendEmailResult()).
		thenThrow(new AmazonClientException("error")).
		thenReturn(new SendEmailResult());

		SimpleMailMessage thirdMessage = createSimpleMailMessage();

		try {
			mailSender.send(new SimpleMailMessage[]{firstMessage, failureMail, thirdMessage});
			Assert.fail("Exception expected due to error while sending mail");
		} catch (MailSendException e) {
			Assert.assertEquals(1, e.getFailedMessages().size());
			Assert.assertTrue(e.getFailedMessages().containsKey(failureMail));
		}
	}

	@Test
	public void testShutDownOfResources() throws Exception {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);
		SimpleEmailServiceMailSender mailSender = createMailSender("access","secret",emailService);

		mailSender.destroy();
		Mockito.verify(emailService, Mockito.times(1)).shutdown();
	}

	private SimpleMailMessage createSimpleMailMessage() {
		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom("sender@domain.com");
		simpleMailMessage.setTo("receiver@domain.com");
		simpleMailMessage.setSubject("message subject");
		simpleMailMessage.setText("message body");
		return simpleMailMessage;
	}

	private SimpleEmailServiceMailSender createMailSender(String accessKey, String secretKey, final AmazonSimpleEmailService emailService) {
		return new SimpleEmailServiceMailSender(accessKey, secretKey) {

			@Override
			protected AmazonSimpleEmailService getEmailService() {
				return emailService;
			}
		};
	}
}
