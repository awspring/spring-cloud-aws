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
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class SimpleEmailServiceJavaMailSender extends SimpleEmailServiceMailSender implements JavaMailSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleEmailServiceMailSender.class);

	public SimpleEmailServiceJavaMailSender(String accessKey, String secretKey) {
		super(accessKey, secretKey);
	}

	public MimeMessage createMimeMessage() {
		return new MimeMessage(Session.getInstance(new Properties()));
	}

	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		try {
			return new MimeMessage(Session.getInstance(new Properties()), contentStream);
		} catch (MessagingException e) {
			throw new MailParseException("Could not parse raw MIME content", e);
		}
	}

	public void send(MimeMessage mimeMessage) throws MailException {
		this.send(new MimeMessage[]{mimeMessage});
	}


	public void send(MimeMessage[] mimeMessages) throws MailException {
		Map<Object, Exception> failedMessages = new HashMap<Object, Exception>();

		for (MimeMessage mimeMessage : mimeMessages) {
			RawMessage rm = createRawMessage(mimeMessage);
			try {
				SendRawEmailResult sendRawEmailResult = getEmailService().sendRawEmail(new SendRawEmailRequest(rm));
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Message with id: {0} successfully send", sendRawEmailResult.getMessageId());
				}
			} catch (AmazonClientException e) {
				//Ignore Exception because we are collecting and throwing all if any
				//noinspection ThrowableResultOfMethodCallIgnored
				failedMessages.put(mimeMessage, e);
			}
		}

		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		send(new MimeMessagePreparator[]{mimeMessagePreparator});
	}

	public void send(MimeMessagePreparator[] mimeMessagePreparators) throws MailException {
		MimeMessage mimeMessage = createMimeMessage();
		for (MimeMessagePreparator mimeMessagePreparator : mimeMessagePreparators) {
			try {
				mimeMessagePreparator.prepare(mimeMessage);
			} catch (Exception e) {
				throw new MailPreparationException(e);
			}
		}
		send(mimeMessage);
	}

	private RawMessage createRawMessage(MimeMessage mimeMessage) {
		ByteArrayOutputStream out;
		try {
			out = new ByteArrayOutputStream();
			mimeMessage.writeTo(out);
		} catch (IOException e) {
			throw new MailPreparationException(e);
		} catch (MessagingException e) {
			throw new MailParseException(e);
		}
		return new RawMessage(ByteBuffer.wrap(out.toByteArray()));
	}
}