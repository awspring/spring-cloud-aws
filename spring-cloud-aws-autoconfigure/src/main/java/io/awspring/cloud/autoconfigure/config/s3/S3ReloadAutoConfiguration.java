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
package io.awspring.cloud.autoconfigure.config.s3;

import io.awspring.cloud.autoconfigure.config.reload.ConfigurationChangeDetector;
import io.awspring.cloud.autoconfigure.config.reload.ConfigurationUpdateStrategy;
import io.awspring.cloud.autoconfigure.config.reload.PollingAwsPropertySourceChangeDetector;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import io.awspring.cloud.s3.config.S3PropertySource;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * {@link AutoConfiguration Auto-Configuration} for reloading properties from S3.
 *
 * @author Matej Nedic
 * @since 3.3.0
 */
@AutoConfiguration
@EnableConfigurationProperties(S3Properties.class)
@ConditionalOnClass({ EndpointAutoConfiguration.class, RestartEndpoint.class, ContextRefresher.class })
@AutoConfigureAfter({ InfoEndpointAutoConfiguration.class, RefreshEndpointAutoConfiguration.class,
		RefreshAutoConfiguration.class })
@ConditionalOnProperty(value = S3Properties.PREFIX + ".config.reload.strategy")
@ConditionalOnBean(ContextRefresher.class)
public class S3ReloadAutoConfiguration {

	@Bean("s3TaskScheduler")
	@ConditionalOnMissingBean
	public TaskSchedulerWrapper<TaskScheduler> taskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

		threadPoolTaskScheduler.setThreadNamePrefix("spring-cloud-aws-s3-ThreadPoolTaskScheduler-");
		threadPoolTaskScheduler.setDaemon(true);

		return new TaskSchedulerWrapper<>(threadPoolTaskScheduler);
	}

	@Bean("s3ConfigurationUpdateStrategy")
	@ConditionalOnMissingBean(name = "s3ConfigurationUpdateStrategy")
	public ConfigurationUpdateStrategy s3ConfigurationUpdateStrategy(S3Properties properties,
			Optional<RestartEndpoint> restarter, ContextRefresher refresher) {
		return ConfigurationUpdateStrategy.create(properties.getConfig().getReload(), refresher, restarter);
	}

	@Bean
	@ConditionalOnBean(ConfigurationUpdateStrategy.class)
	public ConfigurationChangeDetector<S3PropertySource> s3PollingAwsPropertySourceChangeDetector(
			S3Properties properties, @Qualifier("s3ConfigurationUpdateStrategy") ConfigurationUpdateStrategy strategy,
			@Qualifier("s3TaskScheduler") TaskSchedulerWrapper<TaskScheduler> taskScheduler,
			ConfigurableEnvironment environment) {

		return new PollingAwsPropertySourceChangeDetector<>(properties.getConfig().getReload(), S3PropertySource.class,
				strategy, taskScheduler.getTaskScheduler(), environment);
	}
}
