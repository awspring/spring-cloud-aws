/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.context.config.java;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextCredentialsProviderConfiguration.class)
public class ContextStackConfiguration {

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired
	private Environment environment;

	@Bean
	@ConditionalOnProperty("cloud.aws.stack.auto")
	public StackResourceRegistryFactoryBean autoDetectingStackResourceRegistry(AmazonCloudFormation amazonCloudFormationClient) {
		return new StackResourceRegistryFactoryBean(amazonCloudFormationClient);
	}

	@Bean
	@ConditionalOnProperty("cloud.aws.stack.name")
	public StackResourceRegistryFactoryBean staticStackResourceRegistry(AmazonCloudFormation amazonCloudFormationClient) {
		return new StackResourceRegistryFactoryBean(amazonCloudFormationClient, this.environment.getProperty("cloud.aws.stack.name"));
	}

	@Bean
	@ConditionalOnMissingBean(name = "amazonCloudFormation")
	public AmazonCloudFormation amazonCloudFormation(AWSCredentialsProvider credentialsProvider) {
		AmazonCloudFormationClient formationClient = new AmazonCloudFormationClient(credentialsProvider);
		if (this.regionProvider != null) {
			formationClient.setRegion(this.regionProvider.getRegion());
		}
		return formationClient;
	}
}
