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

package io.awspring.cloud.autoconfigure.context;

import io.awspring.cloud.autoconfigure.condition.ConditionalOnAwsCloudEnvironment;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import static io.awspring.cloud.context.config.support.ContextConfigurationUtils.registerInstanceDataPropertySource;

/**
 * Enables passing EC2 instance metadata into Spring
 * {@link org.springframework.context.annotation.PropertySource}.
 *
 * @author Agim Emruli
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "cloud.aws.instance.data.enabled", havingValue = "true")
@ConditionalOnAwsCloudEnvironment
@Import(ContextInstanceDataAutoConfiguration.Registrar.class)
public class ContextInstanceDataAutoConfiguration {

	/**
	 * Registrar for additional environment setup.
	 */
	public static class Registrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

		private Environment environment;

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			registerInstanceDataPropertySource(registry,
					this.environment.getProperty("cloud.aws.instance.data.valueSeparator"),
					this.environment.getProperty("cloud.aws.instance.data.attributeSeparator"));
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

	}

}
