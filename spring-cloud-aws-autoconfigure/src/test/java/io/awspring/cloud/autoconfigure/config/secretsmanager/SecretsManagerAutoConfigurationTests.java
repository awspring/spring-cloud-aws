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
package io.awspring.cloud.autoconfigure.config.secretsmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.MockUtil;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Tests for {@link SecretsManagerAutoConfiguration}.
 *
 * @author Maciej Walkowiak
 */
class SecretsManagerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, SecretsManagerAutoConfiguration.class,
					AwsAutoConfiguration.class));

	@Test
	void secretsManagerAutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.secretsmanager.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(SecretsManagerClient.class));
	}

	@Test
	void secretsManagerAutoConfigurationIsEnabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.secretsmanager.enabled:true")
				.run(context -> assertThat(context).hasSingleBean(SecretsManagerClient.class));
	}

	@Test
	void createsClientBeanByDefault() {
		this.contextRunner.run(context -> assertThat(context).hasSingleBean(SecretsManagerClient.class));
	}

	@Test
	void usesCustomBeanWhenProvided() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(SecretsManagerClient.class);
			assertThat(MockUtil.isMock(context.getBean(SecretsManagerClient.class))).isTrue();
		});
	}

	@TestConfiguration
	static class CustomConfiguration {

		@Bean
		SecretsManagerClient secretsManagerClient() {
			return mock();
		}

	}
}
