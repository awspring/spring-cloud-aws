/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.support.converter;

import static io.awspring.cloud.sqs.config.MessageConverterFactory.createDefaultMappingJacksonMessageConverter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

public class SqsMessagingMessageConverter
		extends AbstractMessagingMessageConverter<software.amazon.awssdk.services.sqs.model.Message> {

	public SqsMessagingMessageConverter() {

		super(createDefaultCompositeMessageConverter());
	}

	private static CompositeMessageConverter createDefaultCompositeMessageConverter() {
		List<MessageConverter> messageConverters = new ArrayList<>();
		messageConverters.add(createClassMatchingMessageConverter());
		messageConverters.add(createStringMessageConverter());
		messageConverters.add(createDefaultMappingJacksonMessageConverter());
		return new CompositeMessageConverter(messageConverters);
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
