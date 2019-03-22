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

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.StringUtils;

/**
 * Simple MailSender implementation to send E-Mails with the Amazon Simple Email Service.
 * This implementation has no dependencies to the Java Mail API. It can be used to send
 * simple mail messages that doesn't have any attachment and therefore only consist of a
 * text body.
 *
 * @author Agim Emruli
 */
public class SimpleEmailServiceMailSender implements MailSender, DisposableBean {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SimpleEmailServiceMailSender.class);

	private final AmazonSimpleEmailService emailService;

	public SimpleEmailServiceMailSender(
			AmazonSimpleEmailService amazonSimpleEmailService) {
		this.emailService = amazonSimpleEmailService;
	}

	@Override
	public void send(SimpleMailMessage simpleMailMessage) throws MailException {
		send(new SimpleMailMessage[] { simpleMailMessage });
	}

	@SuppressWarnings("OverloadedVarargsMethod")
	@Override
	public void send(SimpleMailMessage... simpleMailMessages) throws MailException {

		Map<Object, Exception> failedMessages = new HashMap<>();

		for (SimpleMailMessage simpleMessage : simpleMailMessages) {
			try {
				SendEmailResult sendEmailResult = getEmailService()
						.sendEmail(prepareMessage(simpleMessage));
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Message with id: {} successfully send",
							sendEmailResult.getMessageId());
				}
			}
			catch (AmazonClientException e) {
				// Ignore Exception because we are collecting and throwing all if any
				// noinspection ThrowableResultOfMethodCallIgnored
				failedMessages.put(simpleMessage, e);
			}
		}

		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	@Override
	public final void destroy() throws Exception {
		getEmailService().shutdown();
	}

	protected AmazonSimpleEmailService getEmailService() {
		return this.emailService;
	}

	private SendEmailRequest prepareMessage(SimpleMailMessage simpleMailMessage) {
		Destination destination = new Destination();
		destination.withToAddresses(simpleMailMessage.getTo());

		if (simpleMailMessage.getCc() != null) {
			destination.withCcAddresses(simpleMailMessage.getCc());
		}

		if (simpleMailMessage.getBcc() != null) {
			destination.withBccAddresses(simpleMailMessage.getBcc());
		}

		Content subject = new Content(simpleMailMessage.getSubject());
		Body body = new Body(new Content(simpleMailMessage.getText()));

		SendEmailRequest emailRequest = new SendEmailRequest(simpleMailMessage.getFrom(),
				destination, new Message(subject, body));

		if (StringUtils.hasText(simpleMailMessage.getReplyTo())) {
			emailRequest.withReplyToAddresses(simpleMailMessage.getReplyTo());
		}

		return emailRequest;
	}

}
