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
package io.awspring.cloud.sqs.support.converter;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.HeaderMapper;

/**
 * {@link MessagingMessageConverter} implementation for converting SQS
 * {@link software.amazon.awssdk.services.sqs.model.Message} instances to Spring Messaging {@link Message} instances.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see SqsHeaderMapper
 * @see SqsMessageConversionContext
 */
public class SqsMessagingMessageConverter
		extends AbstractMessagingMessageConverter<software.amazon.awssdk.services.sqs.model.Message> {

	@Override
	protected HeaderMapper<software.amazon.awssdk.services.sqs.model.Message> getDefaultHeaderMapper() {
		return new SqsHeaderMapper();
	}

	@Override
	protected Object getPayloadToConvert(software.amazon.awssdk.services.sqs.model.Message message) {
		return message.body();
	}

	@Override
	public MessageConversionContext createMessageConversionContext() {
		return new SqsMessageConversionContext();
	}

}
