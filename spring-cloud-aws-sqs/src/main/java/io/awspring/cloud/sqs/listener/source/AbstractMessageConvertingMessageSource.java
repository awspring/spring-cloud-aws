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
package io.awspring.cloud.sqs.listener.source;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.listener.ContainerOptions;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.support.converter.AcknowledgementAwareMessageConversionContext;
import io.awspring.cloud.sqs.support.converter.ContextAwareMessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.MessageConversionContext;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * A {@link MessageSource} implementation capable of converting messages from a Source type to a Target type. Subclasses
 * can use the {@link #convertMessage} or {@link #convertMessages} methods to perform the conversion.
 * <p>
 * The {@link MessagingMessageConverter} can be retrieved from the {@link ContainerOptions} or from a subclass.
 * <p>
 * For converters that implement {@link ContextAwareMessagingMessageConverter}, a {@link MessageConversionContext} will
 * be created, which can contain more useful information for message conversion.
 * <p>
 * If such context implements the {@link AcknowledgementAwareMessageConversionContext}, an
 * {@link AcknowledgementCallback} can be added to the context by using the {@link #setupAcknowledgementForConversion}
 * method/.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMessageConvertingMessageSource<T, S> implements MessageSource<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMessageConvertingMessageSource.class);

	private MessagingMessageConverter<S> messagingMessageConverter;

	@Nullable
	private MessageConversionContext messageConversionContext;

	private AcknowledgementMode acknowledgementMode;

	@SuppressWarnings("unchecked")
	@Override
	public void configure(ContainerOptions<?, ?> containerOptions) {
		this.messagingMessageConverter = (MessagingMessageConverter<S>) containerOptions.getMessageConverter();
		this.messageConversionContext = maybeCreateConversionContext();
		this.acknowledgementMode = containerOptions.getAcknowledgementMode();
		configureMessageSource(containerOptions);
	}

	protected void configureMessageSource(ContainerOptions<?, ?> containerOptions) {
	}

	protected void setupAcknowledgementForConversion(AcknowledgementCallback<T> callback) {
		if (this.acknowledgementMode.equals(AcknowledgementMode.MANUAL)) {
			ConfigUtils.INSTANCE.acceptIfInstance(this.messageConversionContext,
					AcknowledgementAwareMessageConversionContext.class,
					aamcc -> aamcc.setAcknowledgementCallback(callback));
		}
	}

	/**
	 * Set the payload deserialization type. This will be used by the message converter to deserialize messages to the
	 * target type. Note that type mappers in MessagingMessageConverters take precedence over this type.
	 * @param payloadDeserializationType the target class
	 */
	public void setPayloadDeserializationType(@Nullable Class<?> payloadDeserializationType) {
		ConfigUtils.INSTANCE.acceptBothIfNoneNull(payloadDeserializationType, this.messageConversionContext,
				this::doConfigurePayloadTypeOnContext);
	}

	/**
	 * Hook method for subclasses to configure the payload type on their specific MessageConversionContext
	 * implementation.
	 * @param payloadType the payload type to configure
	 * @param context the message conversion context
	 */
	protected void doConfigurePayloadTypeOnContext(Class<?> payloadType, MessageConversionContext context) {
	}

	@Nullable
	private MessageConversionContext maybeCreateConversionContext() {
		return this.messagingMessageConverter instanceof ContextAwareMessagingMessageConverter
				? ((ContextAwareMessagingMessageConverter<?>) this.messagingMessageConverter)
						.createMessageConversionContext()
				: null;
	}

	protected Collection<Message<T>> convertMessages(Collection<S> messages) {
		return messages.stream().map(this::convertMessage).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Nullable
	@SuppressWarnings("unchecked")
	protected Message<T> convertMessage(S msg) {
		try {
			logger.trace("Converting message {}", msg);
			return this.messagingMessageConverter instanceof ContextAwareMessagingMessageConverter
					? (Message<T>) getContextAwareConverter().toMessagingMessage(msg, this.messageConversionContext)
					: (Message<T>) this.messagingMessageConverter.toMessagingMessage(msg);
		}
		catch (Exception e) {
			logger.error("Error converting message {}, ignoring.", msg, e);
			return null;
		}
	}

	private ContextAwareMessagingMessageConverter<S> getContextAwareConverter() {
		return (ContextAwareMessagingMessageConverter<S>) this.messagingMessageConverter;
	}

	@Nullable
	protected MessageConversionContext getMessageConversionContext() {
		return this.messageConversionContext;
	}

}
