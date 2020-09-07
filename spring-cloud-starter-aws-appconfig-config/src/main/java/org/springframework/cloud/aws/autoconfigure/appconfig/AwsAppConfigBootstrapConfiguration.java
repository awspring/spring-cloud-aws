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

package org.springframework.cloud.aws.autoconfigure.appconfig;

import com.amazonaws.services.appconfig.AmazonAppConfig;
import com.amazonaws.services.appconfig.AmazonAppConfigAsync;
import com.amazonaws.services.appconfig.AmazonAppConfigClientBuilder;
import com.amazonaws.util.StringUtils;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.appconfig.AwsAppConfigProperties;
import org.springframework.cloud.aws.appconfig.AwsAppConfigPropertySourceLocator;
import org.springframework.cloud.aws.core.SpringCloudClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jarpz
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ AwsAppConfigProperties.class })
@ConditionalOnClass({ AmazonAppConfigAsync.class,
		AwsAppConfigPropertySourceLocator.class })
@ConditionalOnProperty(prefix = "aws.appconfig", name = { "enabled" },
		matchIfMissing = true)
public class AwsAppConfigBootstrapConfiguration {

	@Bean
	AwsAppConfigPropertySourceLocator awsAppConfigPropertySourceLocator(
			AmazonAppConfig appConfigClient, AwsAppConfigProperties properties) {
		return new AwsAppConfigPropertySourceLocator(appConfigClient, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public AmazonAppConfig appConfigClient(
			AwsAppConfigProperties awsAppConfigProperties) {

		AmazonAppConfigClientBuilder builder = AmazonAppConfigClientBuilder.standard()
				.withClientConfiguration(
						SpringCloudClientConfiguration.getClientConfiguration());

		return StringUtils.isNullOrEmpty(awsAppConfigProperties.getRegion())
				? builder.build()
				: builder.withRegion(awsAppConfigProperties.getRegion()).build();
	}

}
