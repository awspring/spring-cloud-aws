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
package io.awspring.cloud.sqs.support.converter.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListenerAnnotationBeanPostProcessor;
import io.awspring.cloud.sqs.config.legacy.LegacyJacskon2MessageConverterFactory;
import io.awspring.cloud.sqs.support.converter.JacksonMessageConverterFactory;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.awspring.cloud.sqs.support.resolver.legacy.LegacyJackson2NotificationMessageArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.legacy.LegacyJackson2SnsNotificationArgumentResolver;
import java.util.List;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * Used to create {@link LegacyJackson2MessageConverterFactory} and provide ObjectMapper to
 * {@link SqsListenerAnnotationBeanPostProcessor}.
 * @author Matej Nedic
 * @since 4.0.0
 */
@Deprecated
public class LegacyJackson2MessageConverterFactory implements JacksonMessageConverterFactory {

	private ObjectMapper objectMapper;

	public LegacyJackson2MessageConverterFactory(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public MessageConverter create() {
		return LegacyJacskon2MessageConverterFactory.createLegacyJackson2MessageConverter(objectMapper);
	}

	@Override
	public void enrichResolvers(List<HandlerMethodArgumentResolver> argumentResolvers,
			MessageConverter messageConverter) {
		argumentResolvers.add(new LegacyJackson2NotificationMessageArgumentResolver(messageConverter, objectMapper));
		argumentResolvers.add(new LegacyJackson2NotificationSubjectArgumentResolver(objectMapper));
		argumentResolvers.add(new LegacyJackson2SnsNotificationArgumentResolver(messageConverter, objectMapper));
	}

	@Override
	public void enrichMessageConverter(MessagingMessageConverter messageConverter) {
		if (messageConverter instanceof LegacyJackson2SqsMessagingMessageConverter sqsConverter) {
			sqsConverter.setObjectMapper(this.objectMapper);
		}
	}
}
