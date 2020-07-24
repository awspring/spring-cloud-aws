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

package org.springframework.cloud.aws.autoconfigure.messaging;

import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.cloud.aws.messaging.endpoint.config.NotificationHandlerMethodArgumentResolverConfigurationUtils.getNotificationHandlerMethodArgumentResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SNS integration.
 *
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(AmazonSNS.class)
@ConditionalOnProperty(name = "cloud.aws.sns.enabled", havingValue = "true",
		matchIfMissing = true)
public class SnsAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	static class SnsConfiguration {

		private final AWSCredentialsProvider awsCredentialsProvider;

		private final RegionProvider regionProvider;

		SnsConfiguration(ObjectProvider<AWSCredentialsProvider> awsCredentialsProvider,
				ObjectProvider<RegionProvider> regionProvider) {
			this.awsCredentialsProvider = awsCredentialsProvider.getIfAvailable();
			this.regionProvider = regionProvider.getIfAvailable();
		}

		@ConditionalOnMissingAmazonClient(AmazonSNS.class)
		@Bean
		public AmazonWebserviceClientFactoryBean<AmazonSNSClient> amazonSNS() {
			return new AmazonWebserviceClientFactoryBean<>(AmazonSNSClient.class,
					this.awsCredentialsProvider, this.regionProvider);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebMvcConfigurer.class)
	static class SnsWebConfiguration {

		@Bean
		public WebMvcConfigurer snsWebMvcConfigurer(AmazonSNS amazonSns) {
			return new WebMvcConfigurer() {
				@Override
				public void addArgumentResolvers(
						List<HandlerMethodArgumentResolver> resolvers) {
					resolvers
							.add(getNotificationHandlerMethodArgumentResolver(amazonSns));
				}
			};
		}

	}

}
