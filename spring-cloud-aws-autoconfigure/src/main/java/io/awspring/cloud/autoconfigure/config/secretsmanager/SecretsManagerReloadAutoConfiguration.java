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
package io.awspring.cloud.autoconfigure.config.secretsmanager;

import io.awspring.cloud.autoconfigure.config.reload.ConfigurationChangeDetector;
import io.awspring.cloud.autoconfigure.config.reload.ConfigurationUpdateStrategy;
import io.awspring.cloud.autoconfigure.config.reload.PollingAwsPropertySourceChangeDetector;
import io.awspring.cloud.secretsmanager.SecretsManagerPropertySource;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshEndpointAutoConfiguration;
import org.springframework.cloud.commons.util.TaskSchedulerWrapper;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecretsManagerProperties.class)
@ConditionalOnClass({ EndpointAutoConfiguration.class, RestartEndpoint.class, ContextRefresher.class })
@AutoConfigureAfter({ InfoEndpointAutoConfiguration.class, RefreshEndpointAutoConfiguration.class,
		RefreshAutoConfiguration.class })
@ConditionalOnProperty(value = SecretsManagerProperties.CONFIG_PREFIX + ".reload.strategy")
@ConditionalOnBean(ContextRefresher.class)
public class SecretsManagerReloadAutoConfiguration {

	@Bean("secretsManagerTaskScheduler")
	@ConditionalOnMissingBean(name = "secretsManagerTaskScheduler")
	public TaskSchedulerWrapper<TaskScheduler> secretsManagerTaskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setThreadNamePrefix("spring-cloud-aws-secretsmanager-ThreadPoolTaskScheduler-");
		threadPoolTaskScheduler.setDaemon(true);
		return new TaskSchedulerWrapper<>(threadPoolTaskScheduler);
	}

	@Bean
	@ConditionalOnMissingBean(name = "secretsManagerConfigurationUpdateStrategy")
	public ConfigurationUpdateStrategy secretsManagerConfigurationUpdateStrategy(SecretsManagerProperties properties,
			Optional<RestartEndpoint> restarter, ContextRefresher refresher) {
		return ConfigurationUpdateStrategy.create(properties.getReload(), refresher, restarter);
	}

	@Bean
	@ConditionalOnBean(ConfigurationUpdateStrategy.class)
	public ConfigurationChangeDetector<SecretsManagerPropertySource> secretsManagerPollingAwsPropertySourceChangeDetector(
			SecretsManagerProperties properties,
			@Qualifier("secretsManagerConfigurationUpdateStrategy") ConfigurationUpdateStrategy strategy,
			@Qualifier("secretsManagerTaskScheduler") TaskSchedulerWrapper<TaskScheduler> taskScheduler,
			ConfigurableEnvironment environment) {

		return new PollingAwsPropertySourceChangeDetector<>(properties.getReload(), SecretsManagerPropertySource.class,
				strategy, taskScheduler.getTaskScheduler(), environment);
	}
}
