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
package io.awspring.cloud.autoconfigure.config.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import io.awspring.cloud.autoconfigure.config.reload.ConfigurationUpdateStrategy;
import io.awspring.cloud.autoconfigure.config.reload.PollingAwsPropertySourceChangeDetector;
import io.awspring.cloud.s3.config.S3PropertySource;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.autoconfigure.RefreshEndpointAutoConfiguration;
import org.springframework.cloud.commons.util.TaskSchedulerWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

public class S3ReloadAutoConfigurationTests {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(S3ReloadAutoConfiguration.class, RefreshAutoConfiguration.class));

	@Test
	void createsBeansForRefreshStrategy() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.s3.config.reload.strategy:refresh")
				.run(this::createsBeans);
	}

	@Test
	void createsBeansForRestartStrategy() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(EndpointAutoConfiguration.class,
						RefreshEndpointAutoConfiguration.class, ConfigurationPropertiesRebinderAutoConfiguration.class))
				.withPropertyValues("spring.cloud.aws.s3.config.reload.strategy:restart_context",
						"management.endpoint.restart.enabled:true", "management.endpoints.web.exposure.include:restart")
				.run(this::createsBeans);
	}

	@Test
	void doesntCreateBeansWhenStrategyNotSet() {
		this.contextRunner.run(this::doesNotCreateBeans);
	}

	@Test
	void usesCustomTaskScheduler() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.s3.config.reload.strategy:refresh")
				.withUserConfiguration(S3ReloadAutoConfigurationTests.CustomTaskSchedulerConfig.class).run(ctx -> {
					Object taskScheduler = ctx.getBean("s3TaskScheduler");
					assertThat(mockingDetails(taskScheduler).isMock()).isTrue();
				});
	}

	@Test
	void usesCustomConfigurationUpdateStrategy() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.s3.config.reload.strategy:refresh")
				.withUserConfiguration(S3ReloadAutoConfigurationTests.CustomConfigurationUpdateStrategyConfig.class)
				.run(ctx -> {
					ConfigurationUpdateStrategy configurationUpdateStrategy = ctx
							.getBean(ConfigurationUpdateStrategy.class);
					assertThat(mockingDetails(configurationUpdateStrategy).isMock()).isTrue();
				});
	}

	private void doesNotCreateBeans(AssertableApplicationContext ctx) {
		assertThat(ctx).doesNotHaveBean("s3ConfigurationUpdateStrategy");
		assertThat(ctx).doesNotHaveBean("s3PollingAwsPropertySourceChangeDetector");
		assertThat(ctx).doesNotHaveBean("s3TaskScheduler");
	}

	private void createsBeans(AssertableApplicationContext ctx) {
		assertThat(ctx).hasBean("s3ConfigurationUpdateStrategy");
		assertThat(ctx.getBean("s3ConfigurationUpdateStrategy")).isInstanceOf(ConfigurationUpdateStrategy.class);

		assertThat(ctx).hasBean("s3PollingAwsPropertySourceChangeDetector");
		assertThat(ctx).getBean("s3PollingAwsPropertySourceChangeDetector")
				.isInstanceOf(PollingAwsPropertySourceChangeDetector.class);

		PollingAwsPropertySourceChangeDetector<?> changeDetector = ctx
				.getBean(PollingAwsPropertySourceChangeDetector.class);
		assertThat(changeDetector.getPropertySourceClass()).isEqualTo(S3PropertySource.class);

		assertThat(ctx).getBean("s3TaskScheduler").isInstanceOf(TaskSchedulerWrapper.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTaskSchedulerConfig {

		@Bean("s3TaskScheduler")
		public TaskSchedulerWrapper<TaskScheduler> customTaskScheduler() {
			return mock(TaskSchedulerWrapper.class, Answers.RETURNS_DEEP_STUBS);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfigurationUpdateStrategyConfig {

		@Bean("s3ConfigurationUpdateStrategy")
		public ConfigurationUpdateStrategy configurationUpdateStrategy() {
			return mock(ConfigurationUpdateStrategy.class);
		}
	}

}
