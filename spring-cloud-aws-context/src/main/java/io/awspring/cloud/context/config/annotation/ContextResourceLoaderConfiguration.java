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

package io.awspring.cloud.context.config.annotation;

import com.amazonaws.services.s3.AmazonS3Client;
import io.awspring.cloud.context.support.io.SimpleStorageProtocolResolverConfigurer;
import io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils;
import io.awspring.cloud.core.io.s3.SimpleStorageProtocolResolver;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Agim Emruli
 */
@Configuration(proxyBeanMethods = false)
@Import(ContextResourceLoaderConfiguration.Registrar.class)
public class ContextResourceLoaderConfiguration {

	/**
	 * Registrar for Amazon webservice client.
	 */
	public static class Registrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

		protected Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			BeanDefinitionHolder client = AmazonWebserviceClientConfigurationUtils.registerAmazonWebserviceClient(this,
					registry, AmazonS3Client.class.getName(), null, this.environment.getProperty("cloud.aws.s3.region"),
					this.environment.getProperty("cloud.aws.s3.endpoint"), "s3ClientConfiguration");

			BeanDefinitionBuilder configurer = BeanDefinitionBuilder
					.genericBeanDefinition(SimpleStorageProtocolResolverConfigurer.class);
			configurer.addConstructorArgValue(getProtocolResolver(client));

			BeanDefinitionReaderUtils.registerWithGeneratedName(configurer.getBeanDefinition(), registry);
		}

		protected BeanDefinition getProtocolResolver(BeanDefinitionHolder client) {
			BeanDefinitionBuilder resolver = BeanDefinitionBuilder
					.rootBeanDefinition(SimpleStorageProtocolResolver.class);

			BeanDefinition taskExecutor = getTaskExecutorDefinition();
			if (taskExecutor != null) {
				resolver.addPropertyValue("taskExecutor", taskExecutor);
			}
			return resolver.getBeanDefinition();
		}

		protected BeanDefinition getTaskExecutorDefinition() {
			return null;
		}

	}

}
