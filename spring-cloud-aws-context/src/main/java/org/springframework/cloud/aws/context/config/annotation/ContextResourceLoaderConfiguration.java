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

package org.springframework.cloud.aws.context.config.annotation;

import com.amazonaws.services.s3.AmazonS3Client;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.aws.context.support.io.SimpleStorageProtocolResolverConfigurer;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextResourceLoaderConfiguration.Registrar.class)
public class ContextResourceLoaderConfiguration {

	/**
	 * Registrar for Amazon webservice client.
	 */
	public static class Registrar implements ImportBeanDefinitionRegistrar {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			BeanDefinitionHolder client = AmazonWebserviceClientConfigurationUtils
					.registerAmazonWebserviceClient(this, registry,
							AmazonS3Client.class.getName(), null, null);

			BeanDefinitionBuilder configurer = BeanDefinitionBuilder
					.genericBeanDefinition(SimpleStorageProtocolResolverConfigurer.class);
			configurer.addConstructorArgValue(getProtocolResolver(client));

			BeanDefinitionReaderUtils
					.registerWithGeneratedName(configurer.getBeanDefinition(), registry);
		}

		protected BeanDefinition getProtocolResolver(BeanDefinitionHolder client) {
			BeanDefinitionBuilder resolver = BeanDefinitionBuilder
					.rootBeanDefinition(SimpleStorageProtocolResolver.class);
			resolver.addConstructorArgReference(client.getBeanName());

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
