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

package org.springframework.cloud.aws.autoconfigure.cloudmap;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.AWSServiceDiscoveryClientBuilder;
import com.amazonaws.util.StringUtils;
import io.awspring.cloud.core.SpringCloudClientConfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.cloudmap.discovery.CloudMapDiscoveryClient;
import org.springframework.cloud.aws.cloudmap.model.CloudMapProperties;
import org.springframework.cloud.aws.cloudmap.model.registration.ServiceRegistration;
import org.springframework.cloud.aws.cloudmap.registration.CloudMapAutoRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloudmap BootstrapConfiguration configuration class to create the required beans.
 *
 * @author Hari Ohm Prasath
 * @since 2.3.2
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CloudMapProperties.class)
@ConditionalOnClass({ AWSServiceDiscovery.class, ServiceRegistration.class, CloudMapAutoRegistration.class })
@ConditionalOnProperty(prefix = CloudMapProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
public class CloudMapBootstrapConfiguration {

	private final ApplicationContext context;

	private final AWSServiceDiscovery serviceDiscovery;

	private final CloudMapProperties properties;

	public CloudMapBootstrapConfiguration(CloudMapProperties properties, ApplicationContext context) {
		AWSServiceDiscoveryClientBuilder builder = AWSServiceDiscoveryClientBuilder.standard()
				.withClientConfiguration(SpringCloudClientConfiguration.getClientConfiguration())
				.withCredentials(new DefaultAWSCredentialsProviderChain());

		if (!StringUtils.isNullOrEmpty(properties.getRegion())) {
			builder.withRegion(properties.getRegion());
		}

		if (properties.getEndpoint() != null) {
			AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
				properties.getEndpoint().toString(), null);
			builder.withEndpointConfiguration(endpointConfiguration);
		}

		this.serviceDiscovery = builder.build();
		this.properties = properties;
		this.context = context;
	}

	@Bean
	@ConditionalOnMissingBean
	CloudMapAutoRegistration createAutoRegistration() {
		return new CloudMapAutoRegistration(context, serviceDiscovery, properties.getRegistry());
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudMapDiscoveryClient discoveryClient() {
		return new CloudMapDiscoveryClient(serviceDiscovery, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public ServiceRegistration serviceRegistration() {
		return new ServiceRegistration(properties.getRegistry());
	}

}
