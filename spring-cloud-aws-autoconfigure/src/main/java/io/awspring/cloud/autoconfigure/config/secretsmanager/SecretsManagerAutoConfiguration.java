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

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

/**
 * {@link AutoConfiguration Auto-Configuration} for Secrets Manager integration.
 *
 * @author Maciej Walkowiak
 * @since 3.2.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SecretsManagerProperties.class)
@ConditionalOnClass({ SecretsManagerClient.class })
@AutoConfigureAfter(AwsAutoConfiguration.class)
@ConditionalOnProperty(name = "spring.cloud.aws.secretsmanager.enabled", havingValue = "true", matchIfMissing = true)
public class SecretsManagerAutoConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public SecretsManagerClient secretsManagerClient(SecretsManagerProperties properties,
			AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsClientCustomizer<SecretsManagerClientBuilder>> customizer,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer.configure(SecretsManagerClient.builder(), properties,
				connectionDetails.getIfAvailable(), customizer.getIfAvailable()).build();
	}
}
