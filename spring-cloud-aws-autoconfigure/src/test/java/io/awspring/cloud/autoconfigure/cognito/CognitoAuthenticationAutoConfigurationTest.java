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
package io.awspring.cloud.autoconfigure.cognito;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Test for {@link CognitoAuthenticationAutoConfiguration}
 *
 * @author Eddú Meléndez
 * @since 2.3
 */
class CognitoAuthenticationAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CognitoAuthenticationAutoConfiguration.class));

	@Test
	void autoConfigurationIsDisabledWithoutUserPoolAndRegionProperties() {
		this.contextRunner.run(context -> assertThat(context).doesNotHaveBean(JwtDecoder.class));
	}

	@Test
	void enableAutoConfigurationWithUserPoolIdAndRegionProperties() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(CognitoAuthenticationAutoConfiguration.class,
						OAuth2ResourceServerAutoConfiguration.class))
				.withPropertyValues("spring.cloud.aws.security.cognito.region:us-west-1",
						"spring.cloud.aws.security.cognito.user-pool-id:my-user-pool-123")
				.run(context -> {
					JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
					assertThat(jwtDecoder).isNotNull();
					assertThat(jwtDecoder).isInstanceOf(NimbusJwtDecoder.class);
				});
	}

}
