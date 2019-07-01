/*
 * Copyright 2013-2019 the original author or authors.
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

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerProperties;
import org.springframework.cloud.aws.secretsmanager.AwsSecretsManagerPropertySourceLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud Bootstrap Configuration for setting up an
 * {@link AwsSecretsManagerPropertySourceLocator} and its dependencies.
 *
 * @author Fabio Maia
 * @since 2.0.0
 */
@Configuration
@EnableConfigurationProperties(AwsSecretsManagerProperties.class)
@ConditionalOnClass({ AWSSecretsManager.class,
		AwsSecretsManagerPropertySourceLocator.class })
@ConditionalOnProperty(prefix = AwsSecretsManagerProperties.CONFIG_PREFIX,
		name = "enabled", matchIfMissing = true)
public class AwsSecretsManagerBootstrapConfiguration {

	@Bean
	AwsSecretsManagerPropertySourceLocator awsSecretsManagerPropertySourceLocator(
			AWSSecretsManager smClient, AwsSecretsManagerProperties properties) {
		return new AwsSecretsManagerPropertySourceLocator(smClient, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	AWSSecretsManager smClient() {
		return AWSSecretsManagerClientBuilder.defaultClient();
	}

}
