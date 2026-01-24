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

import io.awspring.cloud.sqs.config.MessageConverterFactory;
import io.awspring.cloud.sqs.support.resolver.NotificationMessageArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.NotificationSubjectArgumentResolver;
import io.awspring.cloud.sqs.support.resolver.SnsNotificationArgumentResolver;
import java.util.List;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import tools.jackson.databind.json.JsonMapper;

/**
 * Internal, migration-only implementation for the Jackson 3 variant.
 * <p>
 * This type is transitional and will be removed after the Jackson 3 migration is complete. It is internal
 * wiring and not intended to be used by application code.
 *
 * @author Matej Nedic
 * @since 4.0.0
 */
@Deprecated(forRemoval = true)
public class JacksonJsonMessageConverterMigration implements JacksonMessageConverterMigration {
	private JsonMapper jsonMapper;

	public JacksonJsonMessageConverterMigration(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	public void setJsonMapper(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public MessageConverter createMigrationMessageConverter() {
		return MessageConverterFactory.createJacksonJsonMessageConverter(jsonMapper);
	}

	@Override
	public void addJacksonMigrationResolvers(List<HandlerMethodArgumentResolver> argumentResolvers,
			MessageConverter messageConverter) {
		argumentResolvers.add(new NotificationMessageArgumentResolver(messageConverter, jsonMapper));
		argumentResolvers.add(new NotificationSubjectArgumentResolver(jsonMapper));
		argumentResolvers.add(new SnsNotificationArgumentResolver(messageConverter, jsonMapper));
	}

	public static void enrichResolversDefault(List<HandlerMethodArgumentResolver> argumentResolvers,
			MessageConverter messageConverter) {
		argumentResolvers.add(new NotificationMessageArgumentResolver(messageConverter, new JsonMapper()));
		argumentResolvers.add(new NotificationSubjectArgumentResolver(new JsonMapper()));
		argumentResolvers.add(new SnsNotificationArgumentResolver(messageConverter, new JsonMapper()));
	}
}
