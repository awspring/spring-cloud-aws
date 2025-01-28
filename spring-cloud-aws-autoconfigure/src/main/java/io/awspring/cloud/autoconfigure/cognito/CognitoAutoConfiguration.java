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

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.cognito.CognitoTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * {@link AutoConfiguration Auto-Configuration} for AWS Cognito integration.
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */

@AutoConfiguration
@EnableConfigurationProperties(CognitoProperties.class)
@ConditionalOnClass({ CognitoIdentityProviderClient.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.cognito.enabled", havingValue = "true", matchIfMissing = true)
public class CognitoAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public CognitoIdentityProviderClient cognitoIdentityProviderClient(CognitoProperties cognitoProperties,
			AwsClientBuilderConfigurer awsClientBuilderConfigurer, ObjectProvider<CognitoClientCustomizer> customizers,
			ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer.configureSyncClient(CognitoIdentityProviderClient.builder(),
				cognitoProperties, connectionDetails.getIfAvailable(), customizers.orderedStream(),
				awsSyncClientCustomizers.orderedStream()).build();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = { "spring.cloud.aws.cognito.client-id", "spring.cloud.aws.cognito.user-pool-id" })
	public CognitoTemplate cognitoTemplate(CognitoProperties cognitoProperties,
			CognitoIdentityProviderClient cognitoIdentityProviderClient) {
		return new CognitoTemplate(cognitoIdentityProviderClient, cognitoProperties.getClientId(),
				cognitoProperties.getUserPoolId(), cognitoProperties.getClientSecret());
	}
}
