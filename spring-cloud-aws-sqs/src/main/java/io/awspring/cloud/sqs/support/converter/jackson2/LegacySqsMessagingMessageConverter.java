/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.sqs.support.converter.jackson2;

import static io.awspring.cloud.sqs.config.JacksonAbstractMessageConverterFactory.createDefaultMappingLegacyJackson2MessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.support.converter.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * {@link MessagingMessageConverter} implementation for converting SQS
 * {@link software.amazon.awssdk.services.sqs.model.Message} instances to Spring Messaging {@link Message} instances.
 *
 * @author Tomaz Fernandes
 * @author Dongha kim
 * @see SqsHeaderMapper
 * @see SqsMessageConversionContext
 * @since 3.0
 */
@Deprecated
public class LegacySqsMessagingMessageConverter
		extends AbstractMessagingMessageConverter<software.amazon.awssdk.services.sqs.model.Message> {

	public LegacySqsMessagingMessageConverter() {
		this.payloadMessageConverter = createDefaultCompositeMessageConverter();
	}

	private CompositeMessageConverter createDefaultCompositeMessageConverter() {
		List<MessageConverter> messageConverters = new ArrayList<>();
		messageConverters.add(createClassMatchingMessageConverter());
		messageConverters.add(createStringMessageConverter());
		messageConverters.add(createDefaultMappingLegacyJackson2MessageConverter());
		return new CompositeMessageConverter(messageConverters);
	}

	/**
	 * Set the {@link ObjectMapper} instance to be used for converting the {@link Message} instances payloads.
	 *
	 * @param objectMapper the object mapper instance.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "messageConverter cannot be null");
		MappingJackson2MessageConverter converter = getMappingJackson2MessageConverter().orElseThrow(
				() -> new IllegalStateException("%s can only be set in %s instances, or %s containing one.".formatted(
						ObjectMapper.class.getSimpleName(), MappingJackson2MessageConverter.class.getSimpleName(),
						CompositeMessageConverter.class.getSimpleName())));
		converter.setObjectMapper(objectMapper);
	}

	private Optional<MappingJackson2MessageConverter> getMappingJackson2MessageConverter() {
		return this.getPayloadMessageConverter() instanceof CompositeMessageConverter compositeConverter
				? compositeConverter.getConverters().stream()
						.filter(converter -> converter instanceof MappingJackson2MessageConverter)
						.map(MappingJackson2MessageConverter.class::cast).findFirst()
				: this.getPayloadMessageConverter() instanceof MappingJackson2MessageConverter converter
						? Optional.of(converter)
						: Optional.empty();
	}

	@Override
	protected HeaderMapper<software.amazon.awssdk.services.sqs.model.Message> createDefaultHeaderMapper() {
		return new SqsHeaderMapper();
	}

	@Override
	protected Object getPayloadToDeserialize(software.amazon.awssdk.services.sqs.model.Message message) {
		return message.body();
	}

	@Override
	public MessageConversionContext createMessageConversionContext() {
		return new SqsMessageConversionContext();
	}

	@Override
	protected software.amazon.awssdk.services.sqs.model.Message doConvertMessage(
			software.amazon.awssdk.services.sqs.model.Message messageWithHeaders, Object payload) {
		Assert.isInstanceOf(String.class, payload, "payload must be instance of String");
		return messageWithHeaders.toBuilder().body((String) payload).build();
	}

}
