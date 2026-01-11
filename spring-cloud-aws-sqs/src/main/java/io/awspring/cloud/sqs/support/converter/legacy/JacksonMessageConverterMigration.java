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

import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;

import java.util.List;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;

/**
 * Converter factory used to create MessageConverter either for Jackson 2 or Jackson 3. Used also to provide
 * Implementation for Jackson 3 {@link JacksonJsonMessageConverterMigration} Implementation for Jackson 2
 * {@link LegacyJackson2MessageConverterMigration}
 * Note that you can declare either of implementations as a Bean which will be picked by autoconfiguration.
 * NOTE this is transition only api.
 * @author Matej Nedic
 * @since 4.0.0
 */
@Deprecated(forRemoval = true)
public interface JacksonMessageConverterMigration {

	/**
	 * Creates migration MessageConverter, either Jackson 3 or Jackson 2 specific.
	 * @return MessageConverter used by SQS integration
	 */
	MessageConverter createMigrationMessageConverter();

	/**
	 * Used by {@link SqsListenerConfigurer} to add resolvers which will be used when resolving
	 * message.
	 * @param argumentResolvers List of argument resolvers
	 * @param messageConverter MessageConverters which will be used by resolvers.
	 */
	void addJacksonMigrationResolvers(List<HandlerMethodArgumentResolver> argumentResolvers, MessageConverter messageConverter);

	/**
	 * Used to enrich {@link MessagingMessageConverter} with Jackson implementation
	 * @param messageConverter Which will be enriched
	 */
	default void configureLegacyObjectMapper(MessagingMessageConverter messageConverter) {
	}

}
