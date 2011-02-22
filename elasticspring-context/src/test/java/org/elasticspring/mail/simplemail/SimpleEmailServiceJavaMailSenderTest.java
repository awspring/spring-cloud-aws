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
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for class SimpleEmailServiceJavaMailSender
 */
public class SimpleEmailServiceJavaMailSenderTest {

	@Test
	public void testCreateMimeMessage() throws Exception {
		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender("access", "secret");
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		assertNotNull(mimeMessage);
	}

	@Test
	public void testCreateMimeMessageFromPreDefinedMessage() throws Exception {
		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender("access", "secret");

		MimeMessage original = createMimeMessage();

		MimeMessage mimeMessage = mailSender.createMimeMessage(new ByteArrayInputStream(getMimeMessageAsByteArray(original)));
		assertNotNull(mimeMessage);
		assertEquals(original.getSubject(), mimeMessage.getSubject());
		assertEquals(original.getContent(), mimeMessage.getContent());
		assertEquals(original.getRecipients(Message.RecipientType.TO)[0], mimeMessage.getRecipients(Message.RecipientType.TO)[0]);
	}


	@Test
	public void testSendMimeMessage() throws MessagingException, IOException {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");
		ArgumentCaptor<SendRawEmailRequest> request = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		when(emailService.sendRawEmail(request.capture())).thenReturn(new SendRawEmailResult().withMessageId("123"));
		MimeMessage mimeMessage = createMimeMessage();
		mailSender.send(mimeMessage);
		SendRawEmailRequest rawEmailRequest = request.getValue();
		assertTrue(Arrays.equals(getMimeMessageAsByteArray(mimeMessage), rawEmailRequest.getRawMessage().getData().array()));
	}

	@Test
	public void testSendMultipleMimeMessages() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");


		when(emailService.sendRawEmail(isA(SendRawEmailRequest.class))).thenReturn(new SendRawEmailResult().withMessageId("123"));
		mailSender.send(new MimeMessage[]{createMimeMessage(), createMimeMessage()});
		verify(emailService, times(2)).sendRawEmail(isA(SendRawEmailRequest.class));
	}

	@Test
	public void testSendMailWithMimeMessagePreparator() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		MimeMessagePreparator preparator = new MimeMessagePreparator() {

			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
				mimeMessageHelper.setTo("to@domain.com");
				mimeMessageHelper.setSubject("subject");
				mimeMessageHelper.setText("body");
			}
		};

		ArgumentCaptor<SendRawEmailRequest> request = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		when(emailService.sendRawEmail(request.capture())).thenReturn(new SendRawEmailResult().withMessageId("123"));

		mailSender.send(preparator);

		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(request.getValue().getRawMessage().getData().array()));
		assertEquals("to@domain.com", mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
		assertEquals("subject", mimeMessage.getSubject());
		assertEquals("body", mimeMessage.getContent());
	}

	@Test
	public void testSendMailWithMultipleMimeMessagePreparators() throws Exception {

		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		MimeMessagePreparator[] preparators = new MimeMessagePreparator[3];
		preparators[0] = new MimeMessagePreparator() {

			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
				mimeMessageHelper.setTo("to@domain.com");
			}
		};

		preparators[1] = new MimeMessagePreparator() {

			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
				mimeMessageHelper.setSubject("subject");
			}
		};

		preparators[2] = new MimeMessagePreparator() {

			public void prepare(MimeMessage mimeMessage) throws Exception {
				MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
				mimeMessageHelper.setText("body");
			}
		};

		ArgumentCaptor<SendRawEmailRequest> request = ArgumentCaptor.forClass(SendRawEmailRequest.class);
		when(emailService.sendRawEmail(request.capture())).thenReturn(new SendRawEmailResult().withMessageId("123"));

		mailSender.send(preparators);

		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(request.getValue().getRawMessage().getData().array()));
		assertEquals("to@domain.com", mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
		assertEquals("subject", mimeMessage.getSubject());
		assertEquals("body", mimeMessage.getContent());

	}

	@Test
	public void testCreateMimeMessageWithExceptionInInputStream() throws Exception {
		InputStream inputStream = mock(InputStream.class);

		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		IOException ioException = new IOException("error");
		when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(ioException);

		try {
			mailSender.createMimeMessage(inputStream);
			fail("MailPreparationException expected due to error while creating mail");
		} catch (MailParseException e) {
			assertTrue(e.getMessage().startsWith("Could not parse raw MIME content"));
			assertSame(ioException, e.getCause().getCause());
		}
	}

	@Test
	public void testSendMultipleMailsWithException() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		MimeMessage failureMail = createMimeMessage();
		when(emailService.sendRawEmail(isA(SendRawEmailRequest.class))).
				thenReturn(new SendRawEmailResult()).
				thenThrow(new AmazonClientException("error")).
				thenReturn(new SendRawEmailResult());

		try {
			mailSender.send(new MimeMessage[]{createMimeMessage(), failureMail, createMimeMessage()});
			fail("Exception expected due to error while sending mail");
		} catch (MailSendException e) {
			assertEquals(1, e.getFailedMessages().size());
			assertTrue(e.getFailedMessages().containsKey(failureMail));
		}
	}

	@Test
	public void testSendMultipleMailsWithExceptionWhilePreparing() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = createJavaMailSender(emailService, "accessKey", "secretKey");

		try {
			mailSender.send(new MimeMessage[]{new MimeMessage(Session.getInstance(new Properties()))});
			fail("Exception expected due to error while sending mail");
		} catch (MailPreparationException ignore) {
			//expected due to empty mail message
		}

		try {
			MimeMessage failureMessage = new MimeMessage(Session.getInstance(new Properties())){

				@Override
				public void writeTo(OutputStream os) throws IOException, MessagingException {
					throw new MessagingException("exception");
				}
			};
			mailSender.send(new MimeMessage[]{failureMessage});
			fail("Exception expected due to error while sending mail");
		} catch (MailParseException ignore) {
			//expected due to empty mail message
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
