/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.metrics;

import io.micrometer.cloudwatch.CloudWatchConfig;
import io.micrometer.cloudwatch.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link CloudWatchExportAutoConfiguration}.
 *
 * @author Dawid Kublik
 */
public class CloudWatchExportAutoConfigurationTest {

	private MockEnvironment env;

	private AnnotationConfigApplicationContext context;

	@Before
	public void before() {
		this.env = new MockEnvironment();
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(this.env);
	}

	@Test
	public void testWithoutSettingAnyConfigProperties() {
		this.context.register(CloudWatchExportAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(CloudWatchMeterRegistry.class).isEmpty())
				.isTrue();
	}

	@Test
	public void testConfiguration() throws Exception {
		this.env.setProperty("management.metrics.export.cloudwatch.namespace", "test");

		this.context.register(CloudWatchExportAutoConfiguration.class);
		this.context.refresh();

		CloudWatchMeterRegistry metricsExporter = this.context
				.getBean(CloudWatchMeterRegistry.class);
		assertThat(metricsExporter).isNotNull();

		CloudWatchConfig cloudWatchConfig = this.context.getBean(CloudWatchConfig.class);
		assertThat(cloudWatchConfig).isNotNull();

		Clock clock = this.context.getBean(Clock.class);
		assertThat(clock).isNotNull();

		CloudWatchProperties cloudWatchProperties = this.context
				.getBean(CloudWatchProperties.class);
		assertThat(cloudWatchProperties).isNotNull();

		assertThat(cloudWatchProperties.getNamespace())
				.isEqualTo(cloudWatchConfig.namespace());
	}

}
