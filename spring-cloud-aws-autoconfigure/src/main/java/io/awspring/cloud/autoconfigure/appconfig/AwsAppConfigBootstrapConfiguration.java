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

package io.awspring.cloud.autoconfigure.appconfig;

import java.util.Objects;

import com.amazonaws.services.appconfig.AmazonAppConfigAsync;
import com.amazonaws.services.appconfig.AmazonAppConfigClient;
import io.awspring.cloud.appconfig.AwsAppConfigPropertySourceLocator;
import io.awspring.cloud.core.config.AmazonWebserviceClientFactoryBean;
import io.awspring.cloud.core.region.RegionProvider;
import io.awspring.cloud.core.region.StaticRegionProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author jarpz
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsAppConfigProperties.class)
@ConditionalOnClass({ AmazonAppConfigAsync.class, AwsAppConfigPropertySourceLocator.class })
@ConditionalOnProperty(prefix = "spring.cloud.aws.appconfig", name = "enabled", matchIfMissing = true)
public class AwsAppConfigBootstrapConfiguration {

	private final AwsAppConfigProperties properties;

	private final RegionProvider regionProvider;

	public AwsAppConfigBootstrapConfiguration(AwsAppConfigProperties properties,
			ObjectProvider<RegionProvider> regionProvider) {
		this.properties = properties;
		this.regionProvider = Objects.isNull(properties.getRegion()) ? regionProvider.getIfAvailable()
				: new StaticRegionProvider(properties.getRegion());
	}

	@Bean
	AwsAppConfigPropertySourceLocator awsAppConfigPropertySourceLocator(AmazonAppConfigClient appConfigClient) {
		return new AwsAppConfigPropertySourceLocator(appConfigClient, properties.getAccountId(),
				properties.getApplication(), properties.getConfigurationProfile(), properties.getEnvironment(),
				properties.getConfigurationVersion(), properties.isFailFast());
	}

	@Bean
	@ConditionalOnMissingBean
	public AmazonWebserviceClientFactoryBean<AmazonAppConfigClient> appConfigClient() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonAppConfigClient.class, null, regionProvider);
	}

}
