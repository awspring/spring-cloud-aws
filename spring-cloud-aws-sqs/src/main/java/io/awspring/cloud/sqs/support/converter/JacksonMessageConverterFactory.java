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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListenerAnnotationBeanPostProcessor;
import io.awspring.cloud.sqs.support.converter.legacy.LegacyJackson2MessageConverterFactory;
import java.util.List;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converter factory used to create MessageConverter either for Jackson 2 or Jackson 3. Used also to provide
 * Implementation for Jackson 3 {@link JacksonJsonMessageConverterFactory} Implementation for Jackson 2
 * {@link LegacyJackson2MessageConverterFactory} Which provide either: {@link ObjectMapper} or {@link JsonMapper} for
 * SQS integration. Note that you can declare either of implementations as a Bean which will be picked by
 * autoconfiguration.
 * To be removed with Jackson 2 support
 * @author Matej Nedic
 * @since 4.0.0
 */
public interface JacksonMessageConverterFactory {

	MessageConverter create();

	/**
	 * Used by {@link SqsListenerAnnotationBeanPostProcessor} to add resolvers which will be used when resolving
	 * message.
	 * @param argumentResolvers List of argument resolvers
	 * @param messageConverter MessageConverters which will be used by resolvers.
	 */
	void enrichResolvers(List<HandlerMethodArgumentResolver> argumentResolvers, MessageConverter messageConverter);

	/**
	 * Used to enrich {@link MessagingMessageConverter} with Jackson implementation
	 * @param messageConverter Which will be enriched
	 */
	default void enrichMessageConverter(MessagingMessageConverter messageConverter){}

}
