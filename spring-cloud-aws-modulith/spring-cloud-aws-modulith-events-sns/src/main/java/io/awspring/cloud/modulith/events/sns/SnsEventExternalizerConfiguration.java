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
package io.awspring.cloud.modulith.events.sns;

import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsOperations;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.config.EventExternalizationAutoConfiguration;
import org.springframework.modulith.events.support.BrokerRouting;
import org.springframework.modulith.events.support.DelegatingEventExternalizer;

/**
 * Auto-configuration to set up a {@link DelegatingEventExternalizer} to externalize events to SNS.
 *
 * @author Maciej Walkowiak
 * @author Oliver Drotbohm
 * @since 1.1
 */
@AutoConfiguration
@AutoConfigureAfter(EventExternalizationAutoConfiguration.class)
@ConditionalOnClass(SnsTemplate.class)
@ConditionalOnProperty(name = "spring.modulith.events.externalization.enabled", havingValue = "true", matchIfMissing = true)
class SnsEventExternalizerConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(SnsEventExternalizerConfiguration.class);

	@Bean
	DelegatingEventExternalizer snsEventExternalizer(EventExternalizationConfiguration configuration,
			SnsOperations operations, BeanFactory factory) {

		logger.debug("Registering domain event externalization to SNS…");

		var context = new StandardEvaluationContext();
		context.setBeanResolver(new BeanFactoryResolver(factory));

		return new DelegatingEventExternalizer(configuration, (target, payload) -> {

			var routing = BrokerRouting.of(target, context);
			var builder = SnsNotification.builder(payload);
			var key = routing.getKey(payload);

			// when routing key is set, SNS topic must be a FIFO topic
			if (key != null) {
				builder.groupId(key);
			}

			operations.sendNotification(routing.getTarget(), builder.build());

			return CompletableFuture.completedFuture(null);
		});
	}
}
