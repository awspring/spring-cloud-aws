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

package io.awspring.cloud.autoconfigure.context;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2;
import io.awspring.cloud.autoconfigure.context.properties.AwsStackProperties;
import io.awspring.cloud.context.annotation.ConditionalOnMissingAmazonClient;
import io.awspring.cloud.context.config.annotation.ContextDefaultConfigurationRegistrar;
import io.awspring.cloud.core.config.AmazonWebserviceClientFactoryBean;
import io.awspring.cloud.core.env.stack.StackResourceRegistry;
import io.awspring.cloud.core.env.stack.config.AutoDetectingStackNameProvider;
import io.awspring.cloud.core.env.stack.config.StackNameProvider;
import io.awspring.cloud.core.env.stack.config.StackResourceRegistryFactoryBean;
import io.awspring.cloud.core.env.stack.config.StaticStackNameProvider;
import io.awspring.cloud.core.region.RegionProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Agim Emruli
 * @author Maciej Walkowiak
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@Import({ ContextCredentialsAutoConfiguration.class, ContextDefaultConfigurationRegistrar.class })
@ConditionalOnClass(name = "com.amazonaws.services.cloudformation.AmazonCloudFormation")
@EnableConfigurationProperties(AwsStackProperties.class)
@ConditionalOnProperty(name = "cloud.aws.stack.enabled", havingValue = "true", matchIfMissing = true)
public class ContextStackAutoConfiguration {

	private final AwsStackProperties properties;

	public ContextStackAutoConfiguration(AwsStackProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("cloud.aws.stack.name")
	public StackNameProvider staticStackNameProvider() {
		return new StaticStackNameProvider(properties.getName());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "cloud.aws.stack.auto", havingValue = "true", matchIfMissing = true)
	public StackNameProvider autoDetectingStackNameProvider(AmazonCloudFormation amazonCloudFormation,
			ObjectProvider<AmazonEC2> amazonEC2) {
		return new AutoDetectingStackNameProvider(amazonCloudFormation, amazonEC2.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean(StackResourceRegistry.class)
	@ConditionalOnBean(StackNameProvider.class)
	public StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean(AmazonCloudFormation amazonCloudFormation,
			StackNameProvider stackNameProvider) {
		return new StackResourceRegistryFactoryBean(amazonCloudFormation, stackNameProvider);
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonCloudFormation.class)
	public AmazonWebserviceClientFactoryBean<AmazonCloudFormationClient> amazonCloudFormation(
			ObjectProvider<RegionProvider> regionProvider, ObjectProvider<AWSCredentialsProvider> credentialsProvider) {
		return new AmazonWebserviceClientFactoryBean<>(AmazonCloudFormationClient.class,
				credentialsProvider.getIfAvailable(), regionProvider.getIfAvailable());
	}

}
