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
package io.awspring.cloud.autoconfigure.config.parameterstore;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * {@link AutoConfiguration Auto-Configuration} for AWS Parameter Store integration.
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ParameterStoreProperties.class)
@ConditionalOnClass({ SsmClient.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class,
		AwsAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.parameterstore.enabled", havingValue = "true", matchIfMissing = true)
public class ParameterStoreAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SsmClient ssmClient(ParameterStoreProperties properties,
			AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<SsmClientCustomizer> ssmClientCustomizers,
			ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		return awsClientBuilderConfigurer
				.configureSyncClient(SsmClient.builder(), properties, connectionDetails.getIfAvailable(),
						ssmClientCustomizers.orderedStream(), awsSyncClientCustomizers.orderedStream())
				.build();
	}

}
