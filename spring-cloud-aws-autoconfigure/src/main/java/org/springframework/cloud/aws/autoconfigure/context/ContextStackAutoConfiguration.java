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

package org.springframework.cloud.aws.autoconfigure.context;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.ec2.AmazonEC2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.context.annotation.ConditionalOnAwsCloudEnvironment;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.cloud.aws.context.config.annotation.ContextStackConfiguration;
import org.springframework.cloud.aws.core.env.stack.config.AutoDetectingStackNameProvider;
import org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean;
import org.springframework.cloud.aws.core.env.stack.config.StaticStackNameProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @author Agim Emruli
 */
@Configuration
@Import({ContextCredentialsAutoConfiguration.class, ContextDefaultConfigurationRegistrar.class})
@ConditionalOnClass(name = "com.amazonaws.services.cloudformation.AmazonCloudFormation")
public class ContextStackAutoConfiguration {

	@Configuration
	@ConditionalOnProperty(prefix = "cloud.aws", name = "stack.name")
	public static class StackManualDetectConfiguration extends ContextStackConfiguration {

		@Autowired
		private Environment environment;

		@Override
		@Bean
		public StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean(AmazonCloudFormation amazonCloudFormation) {
			return new StackResourceRegistryFactoryBean(amazonCloudFormation, new StaticStackNameProvider(this.environment.getProperty("cloud.aws.stack.name")));
		}
	}


	@Configuration
	@ConditionalOnProperty(prefix = "cloud.aws", name = "stack.auto", havingValue = "true", matchIfMissing = true)
	@ConditionalOnAwsCloudEnvironment
	@ConditionalOnMissingBean(StackResourceRegistryFactoryBean.class)
	public static class StackAutoDetectConfiguration extends ContextStackConfiguration {

		@Autowired(required = false)
		private AmazonEC2 amazonEC2;

		@Override
		@Bean
		public StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean(AmazonCloudFormation amazonCloudFormation) {
			return new StackResourceRegistryFactoryBean(amazonCloudFormation, new AutoDetectingStackNameProvider(amazonCloudFormation, this.amazonEC2));
		}

	}
}
