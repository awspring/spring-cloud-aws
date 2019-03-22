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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.activation.FileTypeMap;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for class SimpleEmailServiceJavaMailSender.
 */
public class SimpleEmailServiceJavaMailSenderTest {

	@Test
	public void createMimeMessage_withDefaultPropertiesAndNoEncodingAndFileTypeMap_returnsSessionWithEmptyProperties() {
		// Arrange
		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(null);

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();

		// Assert
		assertThat(mimeMessage).isNotNull();
		assertThat(mimeMessage.getSession().getProperties().size()).isEqualTo(0);
	}

	@Test
	public void createMimeMessage_withCustomProperties_sessionMaintainsCustomProperties() {
		// Arrange
		Properties mailProperties = new Properties();
		mailProperties.setProperty("mail.from", "agim.emruli@maildomain.com");

		SimpleEmailServiceJavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(
				null);
		mailSender.setJavaMailProperties(mailProperties);

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();

		// Assert
		assertThat(mimeMessage).isNotNull();
		assertThat(mimeMessage.getSession().getProperty("mail.from"))
				.isEqualTo("agim.emruli@maildomain.com");
	}

	@Test
	public void createMimeMessage_withCustomSession_sessionUsedInMailIsCustomSession() {
		// Arrange
		Session customSession = Session.getInstance(new Properties());

		SimpleEmailServiceJavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(
				null);
		mailSender.setSession(customSession);

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();

		// Assert
		assertThat(mimeMessage.getSession()).isSameAs(customSession);
	}

	@Test
	public void createMimeMessage_withCustomEncoding_encodingIsDetectedInMimeMessageHelper() {
		// Arrange
		SimpleEmailServiceJavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(
				null);
		mailSender.setDefaultEncoding("ISO-8859-1");

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

		// Assert
		assertThat(mimeMessageHelper.getEncoding()).isEqualTo("ISO-8859-1");
	}

	@Test
	public void createMimeMessage_withCustomFileTypeMap_fileTypeMapIsAvailableInMailSender() {
		// Arrange
		SimpleEmailServiceJavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(
				null);
		mailSender.setDefaultFileTypeMap(FileTypeMap.getDefaultFileTypeMap());

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

		// Assert
		assertThat(mimeMessageHelper.getFileTypeMap()).as("ISO-8859-1").isNotNull();
	}

	@Test
	public void testCreateMimeMessageFromPreDefinedMessage() throws Exception {
		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(null);

		MimeMessage original = createMimeMessage();

		MimeMessage mimeMessage = mailSender.createMimeMessage(
				new ByteArrayInputStream(getMimeMessageAsByteArray(original)));
		assertThat(mimeMessage).isNotNull();
		assertThat(mimeMessage.getSubject()).isEqualTo(original.getSubject());
		assertThat(mimeMessage.getContent()).isEqualTo(original.getContent());
		assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0])
				.isEqualTo(original.getRecipients(Message.RecipientType.TO)[0]);
	}

	@Test
	public void testSendMimeMessage() throws MessagingException {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);
		ArgumentCaptor<SendRawEmailRequest> request = ArgumentCaptor
				.forClass(SendRawEmailRequest.class);
		when(emailService.sendRawEmail(request.capture()))
				.thenReturn(new SendRawEmailResult().withMessageId("123"));
		MimeMessage mimeMessage = createMimeMessage();
		mailSender.send(mimeMessage);
		assertThat(mimeMessage.getMessageID()).isEqualTo("123");
	}

	@Test
	public void testSendMultipleMimeMessages() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		when(emailService.sendRawEmail(ArgumentMatchers.isA(SendRawEmailRequest.class)))
				.thenReturn(new SendRawEmailResult().withMessageId("123"));
		mailSender.send(createMimeMessage(), createMimeMessage());
		verify(emailService, times(2))
				.sendRawEmail(ArgumentMatchers.isA(SendRawEmailRequest.class));
	}

	@Test
	public void testSendMailWithMimeMessagePreparator() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		MimeMessagePreparator preparator = mimeMessage -> {
			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
			mimeMessageHelper.setTo("to@domain.com");
			mimeMessageHelper.setSubject("subject");
			mimeMessageHelper.setText("body");
		};

		ArgumentCaptor<SendRawEmailRequest> request = ArgumentCaptor
				.forClass(SendRawEmailRequest.class);
		when(emailService.sendRawEmail(request.capture()))
				.thenReturn(new SendRawEmailResult().withMessageId("123"));

		mailSender.send(preparator);

		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()),
				new ByteArrayInputStream(
						request.getValue().getRawMessage().getData().array()));
		assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString())
				.isEqualTo("to@domain.com");
		assertThat(mimeMessage.getSubject()).isEqualTo("subject");
		assertThat(mimeMessage.getContent()).isEqualTo("body");
	}

	@Test
	public void testSendMailWithMultipleMimeMessagePreparators() throws Exception {

		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		MimeMessagePreparator[] preparators = new MimeMessagePreparator[3];
		preparators[0] = mimeMessage -> {
			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
			mimeMessageHelper.setTo("to@domain.com");
		};

		preparators[1] = mimeMessage -> {
			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
			mimeMessageHelper.setSubject("subject");
		};

		preparators[2] = mimeMessage -> {
			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
			mimeMessageHelper.setText("body");
		};

		ArgumentCaptor<SendRawEmailRequest> request = ArgumentCaptor
				.forClass(SendRawEmailRequest.class);
		when(emailService.sendRawEmail(request.capture()))
				.thenReturn(new SendRawEmailResult().withMessageId("123"));

		mailSender.send(preparators);

		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()),
				new ByteArrayInputStream(
						request.getValue().getRawMessage().getData().array()));
		assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString())
				.isEqualTo("to@domain.com");
		assertThat(mimeMessage.getSubject()).isEqualTo("subject");
		assertThat(mimeMessage.getContent()).isEqualTo("body");

	}

	@Test
	public void testCreateMimeMessageWithExceptionInInputStream() throws Exception {
		InputStream inputStream = mock(InputStream.class);

		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		IOException ioException = new IOException("error");
		when(inputStream.read(ArgumentMatchers.any(byte[].class),
				ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
						.thenThrow(ioException);

		try {
			mailSender.createMimeMessage(inputStream);
			fail("MailPreparationException expected due to error while creating mail");
		}
		catch (MailParseException e) {
			assertThat(e.getMessage().startsWith("Could not parse raw MIME content"))
					.isTrue();
			assertThat(e.getCause().getCause()).isSameAs(ioException);
		}
	}

	@Test
	public void testSendMultipleMailsWithException() throws Exception {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		MimeMessage failureMail = createMimeMessage();
		when(emailService.sendRawEmail(ArgumentMatchers.isA(SendRawEmailRequest.class)))
				.thenReturn(new SendRawEmailResult())
				.thenThrow(new AmazonClientException("error"))
				.thenReturn(new SendRawEmailResult());

		try {
			mailSender.send(createMimeMessage(), failureMail, createMimeMessage());
			fail("Exception expected due to error while sending mail");
		}
		catch (MailSendException e) {
			assertThat(e.getFailedMessages().size()).isEqualTo(1);
			assertThat(e.getFailedMessages().containsKey(failureMail)).isTrue();
		}
	}

	@Test
	public void testSendMailsWithExceptionWhilePreparing() {
		AmazonSimpleEmailService emailService = mock(AmazonSimpleEmailService.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		MimeMessage mimeMessage = null;
		try {
			mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
			mailSender.send(new MimeMessage[] { mimeMessage });
			fail("Exception expected due to error while sending mail");
		}
		catch (MailSendException e) {
			// expected due to empty mail message
			assertThat(e.getFailedMessages().size()).isEqualTo(1);
			// noinspection ThrowableResultOfMethodCallIgnored
			assertThat(e.getFailedMessages()
					.get(mimeMessage) instanceof MailPreparationException).isTrue();
		}

		MimeMessage failureMessage = null;
		try {
			failureMessage = new MimeMessage(Session.getInstance(new Properties())) {

				@Override
				public void writeTo(OutputStream os) throws MessagingException {
					throw new MessagingException("exception");
				}
			};
			mailSender.send(new MimeMessage[] { failureMessage });
			fail("Exception expected due to error while sending mail");
		}
		catch (MailSendException e) {
			// expected due to exception writing message
			assertThat(e.getFailedMessages().size()).isEqualTo(1);
			// noinspection ThrowableResultOfMethodCallIgnored
			assertThat(e.getFailedMessages()
					.get(failureMessage) instanceof MailParseException).isTrue();
		}
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

	private byte[] getMimeMessageAsByteArray(MimeMessage mimeMessage)
			throws IOException, MessagingException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		mimeMessage.writeTo(os);
		return os.toByteArray();
	}

}
