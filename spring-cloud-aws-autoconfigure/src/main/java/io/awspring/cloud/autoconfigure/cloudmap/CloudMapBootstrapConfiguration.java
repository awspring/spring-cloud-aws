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
package io.awspring.cloud.autoconfigure.cloudmap;

import io.awspring.cloud.autoconfigure.cloudmap.discovery.CloudMapDiscoveryClient;
import io.awspring.cloud.autoconfigure.cloudmap.properties.CloudMapProperties;
import io.awspring.cloud.autoconfigure.cloudmap.properties.registration.ServiceRegistration;
import io.awspring.cloud.autoconfigure.cloudmap.registration.CloudMapAutoRegistration;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClient;
import software.amazon.awssdk.services.servicediscovery.ServiceDiscoveryClientBuilder;

/**
 * Cloudmap BootstrapConfiguration configuration class to create the required beans.
 *
 * @author Hari Ohm Prasath
 * @since 3.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CloudMapProperties.class)
@ConditionalOnClass({ ServiceDiscoveryClient.class, ServiceRegistration.class, CloudMapAutoRegistration.class })
@ConditionalOnProperty(prefix = CloudMapProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
public class CloudMapBootstrapConfiguration {

	private final ApplicationContext context;
	private final CloudMapProperties properties;

	public CloudMapBootstrapConfiguration(CloudMapProperties properties, ApplicationContext context) {
		this.properties = properties;
		this.context = context;
	}

	@ConditionalOnMissingBean
	@Bean
	public ServiceDiscoveryClient discoveryClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsClientCustomizer<ServiceDiscoveryClientBuilder>> configurer) {
		return awsClientBuilderConfigurer
				.configure(ServiceDiscoveryClient.builder(), this.properties, configurer.getIfAvailable()).build();
	}

	@Bean
	@ConditionalOnMissingBean
	CloudMapAutoRegistration createAutoRegistration(ServiceDiscoveryClient serviceDiscovery) {
		return new CloudMapAutoRegistration(context, serviceDiscovery, properties.getRegistry());
	}

	@Bean
	@ConditionalOnMissingBean
	CloudMapDiscoveryClient createDiscoveryClient(ServiceDiscoveryClient serviceDiscovery) {
		return new CloudMapDiscoveryClient(serviceDiscovery, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	ServiceRegistration serviceRegistration() {
		return new ServiceRegistration(properties.getRegistry());
	}

}
