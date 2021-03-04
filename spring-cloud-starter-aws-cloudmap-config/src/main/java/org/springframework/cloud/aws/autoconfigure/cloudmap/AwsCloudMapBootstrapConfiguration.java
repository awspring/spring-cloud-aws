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
import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.AWSServiceDiscoveryClientBuilder;
import com.amazonaws.util.StringUtils;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.cloud.aws.cloudmap.CloudMapProperties;
import org.springframework.cloud.aws.cloudmap.AwsCloudMapPropertySourceLocator;
import org.springframework.cloud.aws.cloudmap.CloudMapDiscoverService;
import org.springframework.cloud.aws.cloudmap.CloudMapRegistryAnnotationScanner;
import org.springframework.cloud.aws.cloudmap.CloudMapRegistryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CloudMapProperties.class)
@ConditionalOnClass({ AWSServiceDiscovery.class })
@ConditionalOnProperty(prefix = CloudMapProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
@ComponentScan
public class AwsCloudMapBootstrapConfiguration {

	@Bean
	AwsCloudMapPropertySourceLocator awscloudMapPropertySourceLocator(AWSServiceDiscovery serviceDiscovery,
			CloudMapProperties properties, CloudMapDiscoverService instanceDiscovery) {
		return new AwsCloudMapPropertySourceLocator(serviceDiscovery, properties.getDiscovery(), instanceDiscovery);
	}

	@Bean
	CloudMapRegistryService registerInstance(AWSServiceDiscovery serviceDiscovery, CloudMapProperties properties) {
		CloudMapRegistryService registryService = new CloudMapRegistryService(serviceDiscovery,
				properties.getRegistry());
		registryService.registerInstances();
		return registryService;
	}

	@Bean
	CloudMapRegistryAnnotationScanner scanRegistryAnnotation(AWSServiceDiscovery serviceDiscovery,
			CloudMapProperties properties) {
		CloudMapRegistryAnnotationScanner annotationScanner = new CloudMapRegistryAnnotationScanner(serviceDiscovery,
				properties.getAnnotationBasePackage());
		annotationScanner.scanAndRegister();
		return annotationScanner;
	}

	@Bean
	@ConditionalOnMissingBean
	AWSServiceDiscovery serviceDiscovery(CloudMapProperties properties) {
		return createServiceDiscoveryClient(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	CloudMapDiscoverService createInstanceDiscovery() {
		return new CloudMapDiscoverService();
	}

	@Bean
	ConfigurableServletWebServerFactory webServerFactory(final CloudMapRegistryService cloudMapRegistryService) {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
		factory.addConnectorCustomizers(cloudMapRegistryService);
		return factory;
	}

	public static AWSServiceDiscovery createServiceDiscoveryClient(CloudMapProperties properties) {
		AWSServiceDiscoveryClientBuilder builder = AWSServiceDiscoveryClientBuilder.standard()
				.withCredentials(new DefaultAWSCredentialsProviderChain());

		if (!StringUtils.isNullOrEmpty(properties.getRegion())) {
			builder.withRegion(properties.getRegion());
		}

		return builder.build();
	}

}
