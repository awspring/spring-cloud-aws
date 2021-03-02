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

package io.awspring.cloud.v3.autoconfigure.paramstore;

import io.awspring.cloud.v3.core.SpringCloudClientConfiguration;
import io.awspring.cloud.v3.paramstore.AwsParamStoreProperties;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.utils.StringUtils;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author Joris Kuipers
 * @author Matej Nedic
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsParamStoreProperties.class)
@ConditionalOnClass({ SsmClient.class })
@ConditionalOnProperty(prefix = AwsParamStoreProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
public class AwsParamStoreBootstrapConfiguration {

	private final Environment environment;

	public AwsParamStoreBootstrapConfiguration(Environment environment) {
		this.environment = environment;
	}

	@Bean
	@ConditionalOnMissingBean
	SsmClient ssmClient(AwsParamStoreProperties properties) {
		return createSimpleSystemManagementClient(properties);
	}

	public static SsmClient createSimpleSystemManagementClient(AwsParamStoreProperties properties) {
		SsmClientBuilder builder = SsmClient.builder()
				.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());
		if (!StringUtils.isEmpty(properties.getRegion())) {
			builder.region(Region.of(properties.getRegion()));
		}
		if (properties.getEndpoint() != null) {
			builder.endpointOverride(properties.getEndpoint());
		}
		return builder.build();
	}

}
