/*
 * Copyright 2013-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.appconfig.AmazonAppConfigAsync;
import com.amazonaws.services.appconfig.AmazonAppConfigClient;
import io.awspring.cloud.appconfig.AppConfigPropertySourceLocator;
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
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author jarpz
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppConfigProperties.class)
@ConditionalOnClass({ AmazonAppConfigAsync.class, AppConfigPropertySourceLocator.class })
@ConditionalOnProperty(prefix = "spring.cloud.aws.appconfig", name = "enabled", matchIfMissing = true)
public class AppConfigBootstrapConfiguration {

	private final AppConfigProperties properties;

	private final AWSCredentialsProvider credentialsProvider;

	private final RegionProvider regionProvider;

	private final Environment environment;

	public AppConfigBootstrapConfiguration(AppConfigProperties properties,
			ObjectProvider<RegionProvider> regionProvider, ObjectProvider<AWSCredentialsProvider> credentialsProvider,
			Environment environment) {
		this.properties = properties;
		this.regionProvider = Objects.isNull(properties.getRegion()) ? regionProvider.getIfAvailable()
				: new StaticRegionProvider(properties.getRegion());
		this.credentialsProvider = credentialsProvider.getIfAvailable();
		this.environment = environment;
	}

	@Bean
	AppConfigPropertySourceLocator awsAppConfigPropertySourceLocator(AmazonAppConfigClient appConfigClient) {
		if (!StringUtils.hasLength(this.properties.getApplication())) {
			this.properties.setApplication(this.environment.getProperty("spring.application.name", "application"));
		}
		if (!StringUtils.hasLength(this.properties.getClientId())) {
			String clientId = this.properties.getApplication() + UUID.randomUUID().toString();
			this.properties.setClientId(clientId);
		}
		if (!StringUtils.hasLength(this.properties.getEnvironment())) {
			this.properties
					.setEnvironment(Arrays.stream(this.environment.getActiveProfiles()).findFirst().orElse("default"));
		}
		return new AppConfigPropertySourceLocator(appConfigClient, this.properties.getClientId(),
				this.properties.getApplication(), this.properties.getConfigurationProfile(),
				this.properties.getEnvironment(), this.properties.getConfigurationVersion(),
				this.properties.isFailFast());
	}

	@Bean
	@ConditionalOnMissingBean
	public AmazonWebserviceClientFactoryBean<AmazonAppConfigClient> appConfigClient() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonAppConfigClient.class, this.credentialsProvider,
				this.regionProvider);
	}

}
