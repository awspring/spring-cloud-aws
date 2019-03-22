/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.context;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.context.properties.AwsS3ResourceLoaderProperties;
import org.springframework.cloud.aws.context.config.annotation.ContextResourceLoaderConfiguration;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextResourceLoaderAutoConfiguration.Registrar.class)
@ConditionalOnClass(name = "com.amazonaws.services.s3.AmazonS3Client")
public class ContextResourceLoaderAutoConfiguration {

	/**
	 * The prefix used for properties related to S3 resource loading via the
	 * ResourceLoader.
	 */
	public static final String AWS_LOADER_PROPERTY_PREFIX = "cloud.aws.loader";

	/**
	 * Bind AWS resource loader related properties to a property instance.
	 * @return An {@link AwsS3ResourceLoaderProperties} instance
	 */
	@Bean
	@ConfigurationProperties(prefix = AWS_LOADER_PROPERTY_PREFIX)
	public AwsS3ResourceLoaderProperties awsS3ResourceLoaderProperties() {
		return new AwsS3ResourceLoaderProperties();
	}

	/**
	 * Sets additional properties for the task executor definition.
	 */
	public static class Registrar extends ContextResourceLoaderConfiguration.Registrar
			implements EnvironmentAware {

		private static final String CORE_POOL_SIZE_PROPERTY_NAME = "corePoolSize";

		private static final String MAX_POOL_SIZE_PROPERTY_NAME = "maxPoolSize";

		private static final String QUEUE_CAPACITY_PROPERTY_NAME = "queueCapacity";

		private Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		protected BeanDefinition getTaskExecutorDefinition() {
			if (containsProperty(CORE_POOL_SIZE_PROPERTY_NAME)
					|| containsProperty(MAX_POOL_SIZE_PROPERTY_NAME)
					|| containsProperty(QUEUE_CAPACITY_PROPERTY_NAME)) {
				BeanDefinitionBuilder builder = BeanDefinitionBuilder
						.rootBeanDefinition(ThreadPoolTaskExecutor.class);

				setPropertyIfConfigured(builder, CORE_POOL_SIZE_PROPERTY_NAME);
				setPropertyIfConfigured(builder, MAX_POOL_SIZE_PROPERTY_NAME);
				setPropertyIfConfigured(builder, QUEUE_CAPACITY_PROPERTY_NAME);

				return builder.getBeanDefinition();
			}
			return super.getTaskExecutorDefinition();
		}

		private boolean containsProperty(String name) {
			return this.environment
					.containsProperty(AWS_LOADER_PROPERTY_PREFIX + "." + name);
		}

		private String getProperty(String name) {
			return this.environment.getProperty(AWS_LOADER_PROPERTY_PREFIX + "." + name);
		}

		private void setPropertyIfConfigured(BeanDefinitionBuilder builder, String name) {
			if (containsProperty(name)) {
				builder.addPropertyValue(name, getProperty(name));
			}
		}

	}

}
