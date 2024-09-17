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
package io.awspring.cloud.sqs.support.converter;

import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * {@link MessagingMessageConverter} implementation for converting SQS
 * {@link software.amazon.awssdk.services.sqs.model.Message} instances to Spring Messaging {@link Message} instances.
 *
 * @author Tomaz Fernandes
 * @author Dongha kim
 * @since 3.0
 * @see SqsHeaderMapper
 * @see SqsMessageConversionContext
 */
public class SqsMessagingMessageConverter
		extends AbstractMessagingMessageConverter<software.amazon.awssdk.services.sqs.model.Message> {

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
