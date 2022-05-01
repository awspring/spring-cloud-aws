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

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Enables autoconfiguration for token provided by Cognito.
 *
 * @author Eddú Meléndez
 * @since 2.3
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(OAuth2ResourceServerAutoConfiguration.class)
@EnableConfigurationProperties(CognitoAuthenticationProperties.class)
@ConditionalOnProperty(prefix = "spring.cloud.aws.security.cognito", name = "enabled", matchIfMissing = true)
@Conditional(CognitoAuthenticationAutoConfiguration.OnUserPoolIdAndRegionPropertiesCondition.class)
public class CognitoAuthenticationAutoConfiguration {

	private final CognitoAuthenticationProperties properties;

	public CognitoAuthenticationAutoConfiguration(CognitoAuthenticationProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(name = "cognitoJwtDelegatingValidator")
	public DelegatingOAuth2TokenValidator<Jwt> cognitoJwtDelegatingValidator() {
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(JwtValidators.createDefaultWithIssuer(this.properties.getIssuer()));
		validators.add(new JwtClaimValidator<List<String>>("aud",
				aud -> aud != null && aud.contains(this.properties.getAppClientId())));

		return new DelegatingOAuth2TokenValidator<>(validators);
	}

	@Bean
	@ConditionalOnMissingBean
	public JwtDecoder cognitoJwtDecoder(
			@Qualifier("cognitoJwtDelegatingValidator") DelegatingOAuth2TokenValidator<Jwt> validator) {
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(this.properties.getRegistry())
				.jwsAlgorithm(SignatureAlgorithm.from(this.properties.getAlgorithm())).build();
		jwtDecoder.setJwtValidator(validator);

		return jwtDecoder;
	}

	static class OnUserPoolIdAndRegionPropertiesCondition extends AllNestedConditions {

		OnUserPoolIdAndRegionPropertiesCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty("spring.cloud.aws.security.cognito.user-pool-id")
		static class HasUserPoolIdProperty {

		}

		@ConditionalOnProperty("spring.cloud.aws.security.cognito.region")
		static class HasRegionProperty {

		}

	}

}
