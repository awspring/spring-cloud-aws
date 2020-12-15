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

package org.springframework.cloud.aws.autoconfigure.paramstore;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.util.StringUtils;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.SpringCloudClientConfiguration;
import org.springframework.cloud.aws.paramstore.AwsParamStoreProperties;
import org.springframework.cloud.aws.paramstore.AwsParamStorePropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring Cloud Bootstrap Configuration for setting up an
 * {@link AwsParamStorePropertySourceLocator} and its dependencies.
 *
 * @author Joris Kuipers
 * @author Matej Nedic
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsParamStoreProperties.class)
@ConditionalOnClass({ AWSSimpleSystemsManagement.class, AwsParamStorePropertySourceLocator.class })
@ConditionalOnProperty(prefix = AwsParamStoreProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
public class AwsParamStoreBootstrapConfiguration {

	private final Environment environment;

	public AwsParamStoreBootstrapConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Bean
	AwsParamStorePropertySourceLocator awsParamStorePropertySourceLocator(AWSSimpleSystemsManagement ssmClient,
			AwsParamStoreProperties properties) {
		if (StringUtils.isNullOrEmpty(properties.getName())) {
			properties.setName(this.environment.getProperty("spring.application.name"));
		}
		return new AwsParamStorePropertySourceLocator(ssmClient, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	AWSSimpleSystemsManagement ssmClient(AwsParamStoreProperties properties) {
		return createSimpleSystemManagementClient(properties);
	}

	public static AWSSimpleSystemsManagement createSimpleSystemManagementClient(AwsParamStoreProperties properties) {
		AWSSimpleSystemsManagementClientBuilder builder = AWSSimpleSystemsManagementClientBuilder.standard()
				.withClientConfiguration(SpringCloudClientConfiguration.getClientConfiguration());
		if (!StringUtils.isNullOrEmpty(properties.getRegion())) {
			builder.withRegion(properties.getRegion());
		}
		if (properties.getEndpoint() != null) {
			AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
					properties.getEndpoint().toString(), null);
			builder.withEndpointConfiguration(endpointConfiguration);
		}
		return builder.build();
	}

}
