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
package io.awspring.cloud.autoconfigure.config.appconfig;

import io.awspring.cloud.appconfig.AppConfigPropertySource;
import io.awspring.cloud.autoconfigure.config.reload.ConfigurationChangeDetector;
import io.awspring.cloud.autoconfigure.config.reload.ConfigurationUpdateStrategy;
import io.awspring.cloud.autoconfigure.config.reload.PollingAwsPropertySourceChangeDetector;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Optional;

/**
 * {@link EnableAutoConfiguration Auto-Configuration} for reloading properties from AppConfig.
 *
 * @author Matej Nedic
 * @since 4.1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AppConfigProperties.class)
@ConditionalOnClass({ EndpointAutoConfiguration.class, RestartEndpoint.class, ContextRefresher.class })
@AutoConfigureAfter({ InfoEndpointAutoConfiguration.class, RefreshEndpointAutoConfiguration.class,
		RefreshAutoConfiguration.class })
@ConditionalOnProperty(value = AppConfigProperties.PREFIX + ".reload.strategy")
@ConditionalOnBean(ContextRefresher.class)
public class AppConfigReloadAutoConfiguration {

	@Bean("appConfigTaskScheduler")
	@ConditionalOnMissingBean
	public TaskSchedulerWrapper<TaskScheduler> appConfigTaskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

		threadPoolTaskScheduler.setThreadNamePrefix("spring-cloud-aws-appconfig-ThreadPoolTaskScheduler-");
		threadPoolTaskScheduler.setDaemon(true);

		return new TaskSchedulerWrapper<>(threadPoolTaskScheduler);
	}

	@Bean("appConfigConfigurationUpdateStrategy")
	@ConditionalOnMissingBean(name = "appConfigConfigurationUpdateStrategy")
	public ConfigurationUpdateStrategy appConfigConfigurationUpdateStrategy(AppConfigProperties properties,
			Optional<RestartEndpoint> restarter, ContextRefresher refresher) {
		return ConfigurationUpdateStrategy.create(properties.getReload(), refresher, restarter);
	}

	@Bean
	@ConditionalOnBean(ConfigurationUpdateStrategy.class)
	public ConfigurationChangeDetector<AppConfigPropertySource> appConfigPollingAwsPropertySourceChangeDetector(
			AppConfigProperties properties,
			@Qualifier("appConfigConfigurationUpdateStrategy") ConfigurationUpdateStrategy strategy,
			@Qualifier("appConfigTaskScheduler") TaskSchedulerWrapper<TaskScheduler> taskScheduler,
			ConfigurableEnvironment environment) {

		return new PollingAwsPropertySourceChangeDetector<>(properties.getReload(), AppConfigPropertySource.class,
				strategy, taskScheduler.getTaskScheduler(), environment);
	}
}
