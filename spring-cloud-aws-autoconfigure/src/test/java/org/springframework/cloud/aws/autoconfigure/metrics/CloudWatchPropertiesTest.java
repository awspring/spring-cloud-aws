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

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertiesPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Bernardo Martins
 */
class CloudWatchPropertiesTest {

	@Test
	void properties_notSet_shouldHaveDefaultValues() {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				CloudWatchPropertiesConfiguration.class);

		CloudWatchProperties cloudWatchProperties = applicationContext.getBean(CloudWatchProperties.class);
		assertThat(cloudWatchProperties.getNamespace()).isEqualTo("");
		assertThat(cloudWatchProperties.getBatchSize()).isEqualTo(20);
	}

	@Test
	void properties_set_shouldOverrideValues() {
		Properties properties = new Properties();
		properties.setProperty("management.metrics.export.cloudwatch.namespace", "test");
		properties.setProperty("management.metrics.export.cloudwatch.batch-size", "5");

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.getEnvironment().getPropertySources()
				.addLast(new PropertiesPropertySource("test", properties));
		applicationContext.register(CloudWatchPropertiesConfiguration.class);
		applicationContext.refresh();

		CloudWatchProperties cloudWatchProperties = applicationContext.getBean(CloudWatchProperties.class);
		assertThat(cloudWatchProperties.getNamespace()).isEqualTo("test");
		assertThat(cloudWatchProperties.getBatchSize()).isEqualTo(5);
	}

	@Configuration
	@EnableConfigurationProperties(CloudWatchProperties.class)
	protected static class CloudWatchPropertiesConfiguration {

	}

}
