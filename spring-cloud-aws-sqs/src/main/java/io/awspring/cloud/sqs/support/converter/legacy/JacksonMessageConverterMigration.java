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
 * Internal, migration-only temporary contract used by the framework to bridge Jackson 2 and Jackson 3.
 * <p>
 * This type is transitional and will be removed after the Jackson 3 migration is complete. It is internal wiring and
 * implementations are not supported outside the migration.
 * <p>
 * To customize payload conversion, provide a {@link MessagingMessageConverter} (such as
 * {@link io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter}) bean. To customize listener method
 * argument resolvers, use {@link SqsListenerConfigurer}.
 *
 * @author Matej Nedic
 * @since 4.0.0
 */
@Deprecated(forRemoval = true)
public interface JacksonMessageConverterMigration {

	/**
	 * @return the migration {@link MessageConverter}
	 */
	MessageConverter createMigrationMessageConverter();

	/**
	 * @param argumentResolvers list to add migration resolvers to
	 * @param messageConverter migration {@link MessageConverter} to be used by those resolvers
	 */
	void addJacksonMigrationResolvers(List<HandlerMethodArgumentResolver> argumentResolvers,
			MessageConverter messageConverter);

	/**
	 * @param messageConverter converter to configure as part of the legacy migration path
	 */
	default void configureLegacyObjectMapper(MessagingMessageConverter messageConverter) {
	}

}
