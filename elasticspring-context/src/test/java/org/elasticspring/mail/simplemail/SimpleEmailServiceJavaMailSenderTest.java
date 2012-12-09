/*
 *
 *  * Copyright 2010-2012 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.elasticspring.mail.simplemail;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;

/**
 * Tests for class SimpleEmailServiceJavaMailSender
 */
public class SimpleEmailServiceJavaMailSenderTest {

	@Test
	public void testCreateMimeMessage() throws Exception {
		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender("access", "secret");
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		Assert.assertNotNull(mimeMessage);
	}

	@Test
	public void testCreateMimeMessageFromPreDefinedMessage() throws Exception {
		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender("access", "secret");

		MimeMessage original = createMimeMessage();

		MimeMessage mimeMessage = mailSender.createMimeMessage(new ByteArrayInputStream(getMimeMessageAsByteArray(original)));
		Assert.assertNotNull(mimeMessage);
		Assert.assertEquals(original.getSubject(), mimeMessage.getSubject());
		Assert.assertEquals(original.getContent(), mimeMessage.getContent());
		Assert.assertEquals(original.getRecipients(Message.RecipientType.TO)[0], mimeMessage.getRecipients(Message.RecipientType.TO)[0]);
	}


	@Test
	public void testSendMimeMessage() throws MessagingException, IOException {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");
		ArgumentCaptor<SendRawEmailRequest> request = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		Mockito.when(emailService.sendRawEmail(request.capture())).thenReturn(new SendRawEmailResult().withMessageId("123"));
		MimeMessage mimeMessage = createMimeMessage();
		mailSender.send(mimeMessage);
		SendRawEmailRequest rawEmailRequest = request.getValue();
		Assert.assertTrue(Arrays.equals(getMimeMessageAsByteArray(mimeMessage), rawEmailRequest.getRawMessage().getData().array()));
	}

	@Test
	public void testSendMultipleMimeMessages() throws Exception {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");


		Mockito.when(emailService.sendRawEmail(Matchers.isA(SendRawEmailRequest.class))).thenReturn(new SendRawEmailResult().withMessageId("123"));
		mailSender.send(new MimeMessage[]{createMimeMessage(), createMimeMessage()});
		Mockito.verify(emailService, Mockito.times(2)).sendRawEmail(Matchers.isA(SendRawEmailRequest.class));
	}

	@Test
	public void testSendMailWithMimeMessagePreparator() throws Exception {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		MimeMessagePreparator preparator = new MimeMessagePreparator() {

			@Override
			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
				mimeMessageHelper.setTo("to@domain.com");
				mimeMessageHelper.setSubject("subject");
				mimeMessageHelper.setText("body");
			}
		};

		ArgumentCaptor<SendRawEmailRequest> request = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		Mockito.when(emailService.sendRawEmail(request.capture())).thenReturn(new SendRawEmailResult().withMessageId("123"));

		mailSender.send(preparator);

		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(request.getValue().getRawMessage().getData().array()));
		Assert.assertEquals("to@domain.com", mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
		Assert.assertEquals("subject", mimeMessage.getSubject());
		Assert.assertEquals("body", mimeMessage.getContent());
	}

	@Test
	public void testSendMailWithMultipleMimeMessagePreparators() throws Exception {

		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		MimeMessagePreparator[] preparators = new MimeMessagePreparator[3];
		preparators[0] = new MimeMessagePreparator() {

			@Override
			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
				mimeMessageHelper.setTo("to@domain.com");
			}
		};

		preparators[1] = new MimeMessagePreparator() {

			@Override
			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
				mimeMessageHelper.setSubject("subject");
			}
		};

		preparators[2] = new MimeMessagePreparator() {

			@Override
			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
				mimeMessageHelper.setText("body");
			}
		};

		ArgumentCaptor<SendRawEmailRequest> request = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		Mockito.when(emailService.sendRawEmail(request.capture())).thenReturn(new SendRawEmailResult().withMessageId("123"));

		mailSender.send(preparators);

		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(request.getValue().getRawMessage().getData().array()));
		Assert.assertEquals("to@domain.com", mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
		Assert.assertEquals("subject", mimeMessage.getSubject());
		Assert.assertEquals("body", mimeMessage.getContent());

	}

	@Test
	public void testCreateMimeMessageWithExceptionInInputStream() throws Exception {
		InputStream inputStream = Mockito.mock(InputStream.class);

		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		IOException ioException = new IOException("error");
		Mockito.when(inputStream.read(Matchers.any(byte[].class), Matchers.anyInt(), Matchers.anyInt())).thenThrow(ioException);

		try {
			mailSender.createMimeMessage(inputStream);
			Assert.fail("MailPreparationException expected due to error while creating mail");
		} catch (MailParseException e) {
			Assert.assertTrue(e.getMessage().startsWith("Could not parse raw MIME content"));
			Assert.assertSame(ioException, e.getCause().getCause());
		}
	}

	@Test
	public void testSendMultipleMailsWithException() throws Exception {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		MimeMessage failureMail = createMimeMessage();
		Mockito.when(emailService.sendRawEmail(Matchers.isA(SendRawEmailRequest.class))).
				thenReturn(new SendRawEmailResult()).
				thenThrow(new AmazonClientException("error")).
				thenReturn(new SendRawEmailResult());

		try {
			mailSender.send(new MimeMessage[]{createMimeMessage(), failureMail, createMimeMessage()});
			Assert.fail("Exception expected due to error while sending mail");
		} catch (MailSendException e) {
			Assert.assertEquals(1, e.getFailedMessages().size());
			Assert.assertTrue(e.getFailedMessages().containsKey(failureMail));
		}
	}

	@Test
	public void testSendMailsWithExceptionWhilePreparing() throws Exception {
		AmazonSimpleEmailService emailService = Mockito.mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		MimeMessage mimeMessage = null;
		try {
			mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
			mailSender.send(new MimeMessage[]{mimeMessage});
			Assert.fail("Exception expected due to error while sending mail");
		} catch (MailSendException e) {
			//expected due to empty mail message
			Assert.assertEquals(1, e.getFailedMessages().size());
			//noinspection ThrowableResultOfMethodCallIgnored
			Assert.assertTrue(e.getFailedMessages().get(mimeMessage) instanceof MailPreparationException);
		}

		MimeMessage failureMessage = null;
		try {
			failureMessage = new MimeMessage(Session.getInstance(new Properties())) {

				@Override
				public void writeTo(OutputStream os) throws IOException, MessagingException {
					throw new MessagingException("exception");
				}
			};
			mailSender.send(new MimeMessage[]{failureMessage});
			Assert.fail("Exception expected due to error while sending mail");
		} catch (MailSendException e) {
			//expected due to exception writing message
			Assert.assertEquals(1, e.getFailedMessages().size());
			//noinspection ThrowableResultOfMethodCallIgnored
			Assert.assertTrue(e.getFailedMessages().get(failureMessage) instanceof MailParseException);
		}
	}

	private JavaMailSender createJavaMailSender(final AmazonSimpleEmailService emailService, String accessKey, String secretKey) {
		return new SimpleEmailServiceJavaMailSender(accessKey, secretKey) {

			@Override
			protected AmazonSimpleEmailService getEmailService() {
				return emailService;
			}
		};
	}

	private MimeMessage createMimeMessage() throws MessagingException {
		MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(message);
		mimeMessageHelper.addTo("to@domain.com");
		mimeMessageHelper.setText("body text");
		mimeMessageHelper.setSubject("subject");
		mimeMessageHelper.getMimeMessage().saveChanges();
		return message;
	}

	private byte[] getMimeMessageAsByteArray(MimeMessage mimeMessage) throws IOException, MessagingException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		mimeMessage.writeTo(os);
		return os.toByteArray();
	}

}
