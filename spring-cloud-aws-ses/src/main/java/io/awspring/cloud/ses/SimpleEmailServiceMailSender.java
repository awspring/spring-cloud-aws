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

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

/**
 * Simple MailSender implementation to send E-Mails with the Amazon Simple Email Service. This implementation has no
 * dependencies to the Java Mail API. It can be used to send simple mail messages that doesn't have any attachment and
 * therefore only consist of a text body and a subject line.
 *
 * @author Agim Emruli
 * @author Eddú Meléndez
 * @author Arun Patra
 * @author Dominik Kovács
 */
public class SimpleEmailServiceMailSender implements MailSender, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleEmailServiceMailSender.class);

	private final SesV2Client sesClient;

	@Nullable
	private final String identityArn;

	@Nullable
	private final String configurationSetName;

	public SimpleEmailServiceMailSender(SesV2Client sesClient) {
		this(sesClient, null);
	}

	public SimpleEmailServiceMailSender(SesV2Client sesClient, @Nullable String identityArn) {
		this(sesClient, identityArn, null);
	}

	public SimpleEmailServiceMailSender(SesV2Client sesClient, @Nullable String identityArn,
			@Nullable String configurationSetName) {
		this.sesClient = sesClient;
		this.identityArn = identityArn;
		this.configurationSetName = configurationSetName;
	}

	@Override
	public void destroy() {
		sesClient.close();
	}

	@Override
	public void send(SimpleMailMessage simpleMessage) throws MailException {
		Assert.notNull(simpleMessage, "simpleMessage are required");
		send(new SimpleMailMessage[] { simpleMessage });
	}

	@Override
	public void send(SimpleMailMessage... simpleMessages) throws MailException {
		Assert.notNull(simpleMessages, "simpleMessages are required");
		Map<Object, Exception> failedMessages = new HashMap<>();

		for (SimpleMailMessage simpleMessage : simpleMessages) {
			try {
				SendEmailResponse sendEmailResult = getEmailService().sendEmail(prepareMessage(simpleMessage));
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Message with id: {} successfully send", sendEmailResult.messageId());
				}
			}
			catch (SesV2Exception e) {
				// Ignore Exception because we are collecting and throwing all if any
				// noinspection ThrowableResultOfMethodCallIgnored
				failedMessages.put(simpleMessage, e);
			}
		}

		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}

	}

	protected SesV2Client getEmailService() {
		return this.sesClient;
	}

	@Nullable
	protected String getIdentityArn() {
		return identityArn;
	}

	@Nullable
	protected String getConfigurationSetName() {
		return configurationSetName;
	}

	private SendEmailRequest prepareMessage(SimpleMailMessage simpleMailMessage) {
		Assert.notNull(simpleMailMessage, "simpleMailMessage are required");

		SendEmailRequest.Builder emailRequestBuilder = SendEmailRequest.builder().destination(destination -> {
			if (simpleMailMessage.getTo() != null) {
				destination.toAddresses(simpleMailMessage.getTo());
			}
			if (simpleMailMessage.getCc() != null) {
				destination.ccAddresses(simpleMailMessage.getCc());
			}
			if (simpleMailMessage.getBcc() != null) {
				destination.bccAddresses(simpleMailMessage.getBcc());
			}
		}).fromEmailAddress(simpleMailMessage.getFrom()).configurationSetName(getConfigurationSetName())
				.fromEmailAddressIdentityArn(getIdentityArn())
				.content(content -> content
						.simple(message -> message.subject(subject -> subject.data(simpleMailMessage.getSubject()))
								.body(body -> body.text(text -> text.data(simpleMailMessage.getText())))));

		if (StringUtils.hasText(simpleMailMessage.getReplyTo())) {
			emailRequestBuilder.replyToAddresses(simpleMailMessage.getReplyTo());
		}

		return emailRequestBuilder.build();
	}

}
