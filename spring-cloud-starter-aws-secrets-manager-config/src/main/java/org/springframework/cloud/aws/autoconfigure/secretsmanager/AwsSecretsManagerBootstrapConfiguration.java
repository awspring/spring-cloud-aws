/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.secretsmanager;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.util.StringUtils;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.SpringCloudClientConfiguration;
import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerProperties;
import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerPropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring Cloud Bootstrap Configuration for setting up an
 * {@link AwsSecretsManagerPropertySourceLocator} and its dependencies.
 *
 * @author Fabio Maia
 * @author Matej Nedic
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsSecretsManagerProperties.class)
@ConditionalOnClass({ AWSSecretsManager.class, AwsSecretsManagerPropertySourceLocator.class })
@ConditionalOnProperty(prefix = AwsSecretsManagerProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
public class AwsSecretsManagerBootstrapConfiguration {

	private final Environment environment;

	public AwsSecretsManagerBootstrapConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Bean
	AwsSecretsManagerPropertySourceLocator awsSecretsManagerPropertySourceLocator(AWSSecretsManager smClient,
			AwsSecretsManagerProperties properties) {
		if (StringUtils.isNullOrEmpty(properties.getName())) {
			properties.setName(this.environment.getProperty("spring.application.name"));
		}
		return new AwsSecretsManagerPropertySourceLocator(smClient, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	AWSSecretsManager smClient(AwsSecretsManagerProperties properties) {
		return createSecretsManagerClient(properties);
	}

	public static AWSSecretsManager createSecretsManagerClient(AwsSecretsManagerProperties properties) {
		AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard()
				.withClientConfiguration(SpringCloudClientConfiguration.getClientConfiguration());
		if (!StringUtils.isNullOrEmpty(properties.getRegion())) {
			builder.withRegion(properties.getRegion());
		}
		if (properties.getEndpoint() != null) {
			EndpointConfiguration endpointConfiguration = new EndpointConfiguration(properties.getEndpoint().toString(),
					null);
			builder.withEndpointConfiguration(endpointConfiguration);
		}
		return builder.build();
	}

}
