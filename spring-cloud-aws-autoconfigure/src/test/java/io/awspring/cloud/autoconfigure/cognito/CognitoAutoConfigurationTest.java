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
package io.awspring.cloud.autoconfigure.cognito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * Test for {@link CognitoAutoConfiguration}
 */
class CognitoAutoConfigurationTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, CognitoAutoConfiguration.class,
					AwsAutoConfiguration.class));

	@Test
	void cognitoAutoConfigurationIsDisabled() {
		this.runner.withPropertyValues("spring.cloud.aws.cognito.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(CognitoIdentityProviderClient.class));
	}

	@Test
	void cognitoAutoConfigurationIsEnabled() {
		this.runner.withPropertyValues("spring.cloud.aws.cognito.enabled:true")
				.run(context -> assertThat(context).hasSingleBean(CognitoIdentityProviderClient.class));
	}

	@Test
	void createCognitoClientBeanByDefault() {
		this.runner.run(context -> assertThat(context).hasSingleBean(CognitoAutoConfiguration.class));
	}

	@Test
	void usesCustomBeanWhenProvided() {
		this.runner.withUserConfiguration(CustomCognitoConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(CognitoIdentityProviderClient.class);
			assertThat(context.getBean(CognitoIdentityProviderClient.class)).isNotNull();
		});
	}

	@TestConfiguration
	static class CustomCognitoConfiguration {
		@Bean
		CognitoIdentityProviderClient cognitoIdentityProviderClient() {
			return mock(CognitoIdentityProviderClient.class);
		}
	}
}
