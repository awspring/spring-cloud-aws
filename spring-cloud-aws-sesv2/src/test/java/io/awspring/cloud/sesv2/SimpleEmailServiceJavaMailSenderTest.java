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
package io.awspring.cloud.sesv2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

import jakarta.activation.FileTypeMap;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.util.Objects;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/**
 * Tests for class {@link SimpleEmailServiceJavaMailSender}.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Arun Patra
 */
class SimpleEmailServiceJavaMailSenderTest {

	@Test
	void createMimeMessage_withDefaultPropertiesAndNoEncodingAndFileTypeMap_returnsSessionWithEmptyProperties() {
		// Arrange
		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(null);

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();

		// Assert
		assertThat(mimeMessage).isNotNull();
		assertThat(mimeMessage.getSession().getProperties()).isEmpty();
	}

	@Test
	void createMimeMessage_withCustomProperties_sessionMaintainsCustomProperties() {
		// Arrange
		Properties mailProperties = new Properties();
		mailProperties.setProperty("mail.from", "agim.emruli@maildomain.com");

		SimpleEmailServiceJavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(null);
		mailSender.setJavaMailProperties(mailProperties);

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();

		// Assert
		assertThat(mimeMessage).isNotNull();
		assertThat(mimeMessage.getSession().getProperty("mail.from")).isEqualTo("agim.emruli@maildomain.com");
	}

	@Test
	void createMimeMessage_withCustomSession_sessionUsedInMailIsCustomSession() {
		// Arrange
		Session customSession = Session.getInstance(new Properties());

		SimpleEmailServiceJavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(null);
		mailSender.setSession(customSession);

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();

		// Assert
		assertThat(mimeMessage.getSession()).isSameAs(customSession);
	}

	@Test
	void createMimeMessage_withCustomEncoding_encodingIsDetectedInMimeMessageHelper() {
		// Arrange
		SimpleEmailServiceJavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(null);
		mailSender.setDefaultEncoding("ISO-8859-1");

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

		// Assert
		assertThat(mimeMessageHelper.getEncoding()).isEqualTo("ISO-8859-1");
	}

	@Test
	void createMimeMessage_withCustomFileTypeMap_fileTypeMapIsAvailableInMailSender() {
		// Arrange
		SimpleEmailServiceJavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(null);
		mailSender.setDefaultFileTypeMap(FileTypeMap.getDefaultFileTypeMap());

		// Act
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

		// Assert
		assertThat(mimeMessageHelper.getFileTypeMap()).as("ISO-8859-1").isNotNull();
	}

	@Test
	void testCreateMimeMessageFromPreDefinedMessage() throws Exception {
		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(null);

		MimeMessage original = createMimeMessage();

		MimeMessage mimeMessage = mailSender
				.createMimeMessage(new ByteArrayInputStream(getMimeMessageAsByteArray(original)));
		assertThat(mimeMessage).isNotNull();
		assertThat(mimeMessage.getSubject()).isEqualTo(original.getSubject());
		assertThat(mimeMessage.getContent()).isEqualTo(original.getContent());
		assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0])
				.isEqualTo(original.getRecipients(Message.RecipientType.TO)[0]);
	}

	@Test
	void testSendMimeMessage() throws MessagingException {
		SesV2Client emailService = mock(SesV2Client.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService,
				"arn:aws:ses:us-east-1:00000001:identity/domain.com");

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());

		MimeMessage mimeMessage = createMimeMessage();
		mailSender.send(mimeMessage);
		assertThat(mimeMessage.getMessageID()).isEqualTo("123");
		assertThat(request.getValue().fromEmailAddressIdentityArn())
				.isEqualTo("arn:aws:ses:us-east-1:00000001:identity/domain.com");
	}

	@Test
	void testSendMimeMessageWithConfigurationSetNameSet() throws MessagingException {
		SesV2Client emailService = mock(SesV2Client.class);
		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService, null, "Configuration Set");
		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());
		MimeMessage mimeMessage = createMimeMessage();

		mailSender.send(mimeMessage);

		assertThat(request.getValue().configurationSetName()).isEqualTo("Configuration Set");
	}

	@Test
	void testSendMultipleMimeMessages() throws Exception {
		SesV2Client emailService = mock(SesV2Client.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		when(emailService.sendEmail(ArgumentMatchers.isA(SendEmailRequest.class)))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());

		mailSender.send(createMimeMessage(), createMimeMessage());
		verify(emailService, times(2)).sendEmail(ArgumentMatchers.isA(SendEmailRequest.class));
	}

	@Test
	void testSendMailWithMimeMessagePreparator() throws Exception {
		SesV2Client emailService = mock(SesV2Client.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		MimeMessagePreparator preparator = mimeMessage -> {
			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
			mimeMessageHelper.setTo("to@domain.com");
			mimeMessageHelper.setSubject("subject");
			mimeMessageHelper.setText("body");
		};

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());

		mailSender.send(preparator);

		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()),
				new ByteArrayInputStream(request.getValue().content().raw().data().asByteArray()));
		assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0]).hasToString("to@domain.com");
		assertThat(mimeMessage.getSubject()).isEqualTo("subject");
		assertThat(mimeMessage.getContent()).isEqualTo("body");
	}

	@Test
	void testSendMailWithMultipleMimeMessagePreparators() throws Exception {

		SesV2Client emailService = mock(SesV2Client.class);

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

		ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
		when(emailService.sendEmail(request.capture()))
				.thenReturn(SendEmailResponse.builder().messageId("123").build());

		mailSender.send(preparators);

		MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()),
				new ByteArrayInputStream(request.getValue().content().raw().data().asByteArray()));
		assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0]).hasToString("to@domain.com");
		assertThat(mimeMessage.getSubject()).isEqualTo("subject");
		assertThat(mimeMessage.getContent()).isEqualTo("body");

	}

	@Test
	void testCreateMimeMessageWithExceptionInInputStream() throws Exception {
		InputStream inputStream = mock(InputStream.class);

		SesV2Client emailService = mock(SesV2Client.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		IOException ioException = new IOException("error");
		when(inputStream.read(ArgumentMatchers.any(byte[].class), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
				.thenThrow(ioException);

		try {
			mailSender.createMimeMessage(inputStream);
			fail("MailPreparationException expected due to error while creating mail");
		}
		catch (MailParseException e) {
			assertThat(Objects.requireNonNull(e.getMessage()).startsWith("Could not parse raw MIME content")).isTrue();
			assertThat(e.getCause().getCause()).isSameAs(ioException);
		}
	}

	@Test
	void testSendMultipleMailsWithException() throws Exception {
		SesV2Client emailService = mock(SesV2Client.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		MimeMessage failureMail = createMimeMessage();

		when(emailService.sendEmail(ArgumentMatchers.isA(SendEmailRequest.class)))
				.thenReturn(SendEmailResponse.builder().build())
				.thenThrow(SesV2Exception.builder().message("error").build())
				.thenReturn(SendEmailResponse.builder().build());

		try {
			mailSender.send(createMimeMessage(), failureMail, createMimeMessage());
			fail("Exception expected due to error while sending mail");
		}
		catch (MailSendException e) {
			assertThat(e.getFailedMessages()).hasSize(1);
			assertThat(e.getFailedMessages()).containsKey(failureMail);
		}
	}

	@Test
	void testSendMailsWithExceptionWhilePreparing() {
		SesV2Client emailService = mock(SesV2Client.class);

		JavaMailSender mailSender = new SimpleEmailServiceJavaMailSender(emailService);

		MimeMessage mimeMessage = null;
		try {
			mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
			mailSender.send(new MimeMessage[] { mimeMessage });
			fail("Exception expected due to error while sending mail");
		}
		catch (MailSendException e) {
			// expected due to empty mail message
			assertThat(e.getFailedMessages()).hasSize(1);
			// noinspection ThrowableResultOfMethodCallIgnored
			assertThat(e.getFailedMessages().get(mimeMessage)).isInstanceOf(MailPreparationException.class);
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
			assertThat(e.getFailedMessages()).hasSize(1);
			// noinspection ThrowableResultOfMethodCallIgnored
			assertThat(e.getFailedMessages().get(failureMessage)).isInstanceOf(MailParseException.class);
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

	private byte[] getMimeMessageAsByteArray(MimeMessage mimeMessage) throws IOException, MessagingException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		mimeMessage.writeTo(os);
		return os.toByteArray();
	}

}
