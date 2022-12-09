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

package io.awspring.cloud.autoconfigure.messaging;

import java.util.List;
import java.util.Optional;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsyncClient;
import com.amazonaws.services.sns.message.SnsMessageManager;
import io.awspring.cloud.context.annotation.ConditionalOnMissingAmazonClient;
import io.awspring.cloud.core.config.AmazonWebserviceClientFactoryBean;
import io.awspring.cloud.core.region.RegionProvider;
import io.awspring.cloud.core.region.StaticRegionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;
import static io.awspring.cloud.messaging.endpoint.config.NotificationHandlerMethodArgumentResolverConfigurationUtils.getNotificationHandlerMethodArgumentResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SNS integration.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Manuel Wessner
 * @author Matej Nedic
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(AmazonSNS.class)
@EnableConfigurationProperties(SnsProperties.class)
@ConditionalOnProperty(name = "cloud.aws.sns.enabled", havingValue = "true", matchIfMissing = true)
public class SnsAutoConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(SnsAutoConfiguration.class);

	private final AWSCredentialsProvider awsCredentialsProvider;

	private final RegionProvider regionProvider;

	private final ClientConfiguration clientConfiguration;

	SnsAutoConfiguration(ObjectProvider<AWSCredentialsProvider> awsCredentialsProvider,
			ObjectProvider<RegionProvider> regionProvider, SnsProperties properties,
			@Qualifier(GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME) ObjectProvider<ClientConfiguration> globalClientConfiguration,
			@Qualifier("snsClientConfiguration") ObjectProvider<ClientConfiguration> snsClientConfiguration) {
		this.awsCredentialsProvider = awsCredentialsProvider.getIfAvailable();
		this.regionProvider = properties.getRegion() == null ? regionProvider.getIfAvailable()
				: new StaticRegionProvider(properties.getRegion());
		this.clientConfiguration = snsClientConfiguration.getIfAvailable(globalClientConfiguration::getIfAvailable);
	}

	@ConditionalOnMissingAmazonClient(AmazonSNS.class)
	@Bean
	public AmazonWebserviceClientFactoryBean<AmazonSNSAsyncClient> amazonSNS(SnsProperties properties) {
		AmazonWebserviceClientFactoryBean<AmazonSNSAsyncClient> clientFactoryBean = new AmazonWebserviceClientFactoryBean<>(
				AmazonSNSAsyncClient.class, this.awsCredentialsProvider, this.regionProvider, this.clientConfiguration);
		Optional.ofNullable(properties.getEndpoint()).ifPresent(clientFactoryBean::setCustomEndpoint);
		return clientFactoryBean;
	}

	@ConditionalOnProperty(name = "cloud.aws.sns.verification", havingValue = "true", matchIfMissing = true)
	@ConditionalOnMissingBean(SnsMessageManager.class)
	@Bean
	public SnsMessageManager snsMessageManager(SnsProperties snsProperties) {
		if (regionProvider == null) {
			String defaultRegion = Regions.DEFAULT_REGION.getName();
			LOGGER.warn(
					"RegionProvider bean not configured. Configuring SnsMessageManager with region " + defaultRegion);
			return new SnsMessageManager(defaultRegion);
		}
		else {
			return new SnsMessageManager(regionProvider.getRegion().getName());
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebMvcConfigurer.class)
	static class SnsWebConfiguration {

		@Bean
		public WebMvcConfigurer snsWebMvcConfigurer(AmazonSNS amazonSns,
				Optional<SnsMessageManager> snsMessageManager) {
			return new WebMvcConfigurer() {
				@Override
				public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
					resolvers.add(getNotificationHandlerMethodArgumentResolver(amazonSns, snsMessageManager));
				}
			};
		}

	}

}
