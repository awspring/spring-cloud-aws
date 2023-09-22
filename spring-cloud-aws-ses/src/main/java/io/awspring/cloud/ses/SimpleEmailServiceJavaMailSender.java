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

import jakarta.activation.FileTypeMap;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.ConfigurableMimeFileTypeMap;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse;

/**
 * {@link JavaMailSender} implementation that allows to send {@link MimeMessage} using the Simple E-Mail Service. In
 * contrast to {@link SimpleEmailServiceMailSender} this class also allows the use of attachment and other mime parts
 * inside mail messages.
 *
 * @author Agim Emruli
 * @author Eddú Meléndez
 * @author Arun Patra
 * @since 1.0
 */
public class SimpleEmailServiceJavaMailSender extends SimpleEmailServiceMailSender implements JavaMailSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleEmailServiceJavaMailSender.class);

	private static final String SMART_MIME_MESSAGE_CLASS_NAME = "org.springframework.mail.javamail.SmartMimeMessage";

	private Properties javaMailProperties = new Properties();

	@Nullable
	private volatile Session session;

	@Nullable
	private String defaultEncoding;

	@Nullable
	private FileTypeMap defaultFileTypeMap;

	public SimpleEmailServiceJavaMailSender(SesClient sesClient) {
		this(sesClient, null);
	}

	public SimpleEmailServiceJavaMailSender(SesClient sesClient, @Nullable String sourceArn) {
		super(sesClient, sourceArn);
	}

	/**
	 * Allow Map access to the JavaMail properties of this sender, with the option to add or override specific entries.
	 * <p>
	 * Useful for specifying entries directly, for example via "javaMailProperties[mail.from]".
	 * @return java mail properties
	 */
	protected Properties getJavaMailProperties() {
		return this.javaMailProperties;
	}

	/**
	 * Set JavaMail properties for the {@code Session}.
	 * <p>
	 * A new {@code Session} will be created with those properties.
	 * <p>
	 * Non-default properties in this instance will override given JavaMail properties.
	 * @param javaMailProperties java mail props
	 */
	public void setJavaMailProperties(Properties javaMailProperties) {
		Assert.notNull(javaMailProperties, "javaMailProperties are required");
		this.javaMailProperties = javaMailProperties;
		this.session = null;
	}

	/**
	 * Return the JavaMail {@code Session}, lazily initializing it if hasn't been specified explicitly.
	 * @return cached session or a new one from java mail properties
	 */
	@Nullable
	protected Session getSession() {
		if (this.session == null) {
			this.session = Session.getInstance(getJavaMailProperties());
		}
		return this.session;
	}

	/**
	 * Set the JavaMail {@code Session}, possibly pulled from JNDI.
	 * <p>
	 * Default is a new {@code Session} without defaults, that is completely configured via this instance's properties.
	 * <p>
	 * If using a pre-configured {@code Session}, non-default properties in this instance will override the settings in
	 * the {@code Session}.
	 * @param session JavaMail session
	 * @see #setJavaMailProperties
	 */
	public void setSession(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}

	/**
	 * Set the default encoding to use for {@link MimeMessage MimeMessages} created by this instance.
	 * <p>
	 * Such an encoding will be auto-detected by {@link MimeMessageHelper}.
	 * @param defaultEncoding default encoding for mime messages
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Set the default Java Activation {@link FileTypeMap} to use for {@link MimeMessage MimeMessages} created by this
	 * instance.
	 * <p>
	 * A {@code FileTypeMap} specified here will be autodetected by {@link MimeMessageHelper}, avoiding the need to
	 * specify the {@code FileTypeMap} for each {@code MimeMessageHelper} instance.
	 * <p>
	 * For example, you can specify a custom instance of Spring's {@link ConfigurableMimeFileTypeMap} here. If not
	 * explicitly specified, a default {@code ConfigurableMimeFileTypeMap} will be used, containing an extended set of
	 * MIME type mappings (as defined by the {@code mime.types} file contained in the Spring jar).
	 * @param defaultFileTypeMap Java Activation file type map
	 * @see MimeMessageHelper#setFileTypeMap
	 */
	public void setDefaultFileTypeMap(FileTypeMap defaultFileTypeMap) {
		this.defaultFileTypeMap = defaultFileTypeMap;
	}

	@Override
	public MimeMessage createMimeMessage() {

		// We have to use reflection as SmartMimeMessage is not package-private
		if (ClassUtils.isPresent(SMART_MIME_MESSAGE_CLASS_NAME, ClassUtils.getDefaultClassLoader())) {
			Class<?> smartMimeMessage = ClassUtils.resolveClassName(SMART_MIME_MESSAGE_CLASS_NAME,
					ClassUtils.getDefaultClassLoader());
			Constructor<?> constructor = ClassUtils.getConstructorIfAvailable(smartMimeMessage, Session.class,
					String.class, FileTypeMap.class);
			if (constructor != null) {
				Object mimeMessage = BeanUtils.instantiateClass(constructor, getSession(), this.defaultEncoding,
						this.defaultFileTypeMap);
				return (MimeMessage) mimeMessage;
			}
		}

		return new MimeMessage(getSession());
	}

	@Override
	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		Assert.notNull(contentStream, "contentStream are required");
		try {
			return new MimeMessage(getSession(), contentStream);
		}
		catch (MessagingException e) {
			throw new MailParseException("Could not parse raw MIME content", e);
		}
	}

	@Override
	public void send(MimeMessage mimeMessage) throws MailException {
		Assert.notNull(mimeMessage, "mimeMessage are required");
		this.send(new MimeMessage[] { mimeMessage });
	}

	@SuppressWarnings("OverloadedVarargsMethod")
	@Override
	public void send(MimeMessage... mimeMessages) throws MailException {
		Assert.notNull(mimeMessages, "mimeMessages are required");
		Map<Object, Exception> failedMessages = new HashMap<>();

		for (MimeMessage mimeMessage : mimeMessages) {
			try {
				RawMessage rawMessage = createRawMessage(mimeMessage);

				SendRawEmailResponse sendRawEmailResponse = getEmailService().sendRawEmail(
						SendRawEmailRequest.builder().sourceArn(getSourceArn()).rawMessage(rawMessage).build());

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Message with id: {} successfully sent", sendRawEmailResponse.messageId());
				}
				mimeMessage.setHeader("Message-ID", sendRawEmailResponse.messageId());
			}
			catch (Exception e) {
				// Ignore Exception because we are collecting and throwing all if any
				// noinspection ThrowableResultOfMethodCallIgnored
				failedMessages.put(mimeMessage, e);
			}
		}

		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	@Override
	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		Assert.notNull(mimeMessagePreparator, "mimeMessagePreparator are required");
		send(new MimeMessagePreparator[] { mimeMessagePreparator });
	}

	@SuppressWarnings("OverloadedVarargsMethod")
	@Override
	public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
		Assert.notNull(mimeMessagePreparators, "mimeMessagePreparator are required");
		MimeMessage mimeMessage = createMimeMessage();
		for (MimeMessagePreparator mimeMessagePreparator : mimeMessagePreparators) {
			try {
				mimeMessagePreparator.prepare(mimeMessage);
			}
			catch (Exception e) {
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
		}
		catch (IOException e) {
			throw new MailPreparationException(e);
		}
		catch (MessagingException e) {
			throw new MailParseException(e);
		}
		return RawMessage.builder().data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(out.toByteArray()))).build();
	}

}
