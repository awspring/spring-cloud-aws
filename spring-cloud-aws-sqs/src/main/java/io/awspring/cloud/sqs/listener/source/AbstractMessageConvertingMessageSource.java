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
import java.util.stream.Collectors;
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
 * {@link AcknowledgementCallback} can be added to the context by using the {@link #setupAcknowledgementConversion}
 * method/.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public abstract class AbstractMessageConvertingMessageSource<T, S> implements MessageSource<T> {

	private MessagingMessageConverter<S> messagingMessageConverter;

	private MessageConversionContext messageConversionContext;

	private AcknowledgementMode acknowledgementMode;

	@Override
	public void configure(ContainerOptions containerOptions) {
		this.messagingMessageConverter = getOrCreateMessageConverter(containerOptions);
		this.messageConversionContext = maybeCreateConversionContext();
		this.acknowledgementMode = containerOptions.getAcknowledgementMode();
		doConfigureAfterConversion(containerOptions);
	}

	protected abstract void doConfigureAfterConversion(ContainerOptions containerOptions);

	protected void setupAcknowledgementConversion(AcknowledgementCallback<T> callback) {
		if (this.acknowledgementMode.equals(AcknowledgementMode.MANUAL)) {
			ConfigUtils.INSTANCE.acceptIfInstance(this.messageConversionContext,
					AcknowledgementAwareMessageConversionContext.class,
					aamcc -> aamcc.setAcknowledgementCallback(callback));
		}
	}

	@Nullable
	private MessageConversionContext maybeCreateConversionContext() {
		return this.messagingMessageConverter instanceof ContextAwareMessagingMessageConverter
				? ((ContextAwareMessagingMessageConverter<?>) this.messagingMessageConverter)
						.createMessageConversionContext()
				: null;
	}

	protected Collection<Message<T>> convertMessages(Collection<S> messages) {
		return messages.stream().map(this::convertMessage).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	protected Message<T> convertMessage(S msg) {
		return this.messagingMessageConverter instanceof ContextAwareMessagingMessageConverter
				? (Message<T>) getContextAwareConverter().toMessagingMessage(msg, this.messageConversionContext)
				: (Message<T>) this.messagingMessageConverter.toMessagingMessage(msg);
	}

	private ContextAwareMessagingMessageConverter<S> getContextAwareConverter() {
		return (ContextAwareMessagingMessageConverter<S>) this.messagingMessageConverter;
	}

	@SuppressWarnings("unchecked")
	private MessagingMessageConverter<S> getOrCreateMessageConverter(ContainerOptions containerOptions) {
		return containerOptions.getMessageConverter() != null
				? (MessagingMessageConverter<S>) containerOptions.getMessageConverter()
				: createMessageConverter();
	}

	protected abstract MessagingMessageConverter<S> createMessageConverter();

	protected MessageConversionContext getMessageConversionContext() {
		return this.messageConversionContext;
	}

}
