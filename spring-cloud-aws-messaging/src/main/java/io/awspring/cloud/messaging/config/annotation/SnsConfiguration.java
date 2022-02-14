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

package io.awspring.cloud.messaging.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.message.SnsMessageManager;
import io.awspring.cloud.context.annotation.ConditionalOnMissingAmazonClient;
import io.awspring.cloud.core.config.AmazonWebserviceClientFactoryBean;
import io.awspring.cloud.core.region.RegionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alain Sahli
 * @author Agim Emruli
 * @author Eddú Meléndez
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@Deprecated
public class SnsConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(SnsConfiguration.class);

	private final AWSCredentialsProvider awsCredentialsProvider;

	private final RegionProvider regionProvider;

	public SnsConfiguration(ObjectProvider<AWSCredentialsProvider> awsCredentialsProvider,
			ObjectProvider<RegionProvider> regionProvider) {
		this.awsCredentialsProvider = awsCredentialsProvider.getIfAvailable();
		this.regionProvider = regionProvider.getIfAvailable();
	}

	@ConditionalOnMissingAmazonClient(AmazonSNS.class)
	@Bean
	public AmazonWebserviceClientFactoryBean<AmazonSNSClient> amazonSNS() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonSNSClient.class, this.awsCredentialsProvider,
				this.regionProvider);
	}

	@ConditionalOnMissingAmazonClient(SnsMessageManager.class)
	@Bean
	public SnsMessageManager snsMessageManager() {
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

}
