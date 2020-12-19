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

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import io.awspring.cloud.autoconfigure.context.properties.AwsCredentialsProperties;
import io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils;
import io.awspring.cloud.core.credentials.CredentialsProviderFactoryBean;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import static io.awspring.cloud.core.credentials.CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME;

/**
 * {@link EnableAutoConfiguration} for {@link AWSCredentialsProvider}.
 *
 * @author Agim Emruli
 * @author Maciej Walkowiak
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AwsCredentialsProperties.class)
@Import(ContextCredentialsAutoConfiguration.Registrar.class)
@ConditionalOnClass(com.amazonaws.auth.AWSCredentialsProvider.class)
public class ContextCredentialsAutoConfiguration {

	static class Registrar implements EnvironmentAware, ImportBeanDefinitionRegistrar {

		private Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			if (!registry.containsBeanDefinition(CREDENTIALS_PROVIDER_BEAN_NAME)) {
				registry.registerBeanDefinition(CREDENTIALS_PROVIDER_BEAN_NAME,
						resolveCredentialsProviderBeanDefinition(
								resolveCredentialsProviders(awsCredentialsProperties())));
				AmazonWebserviceClientConfigurationUtils.replaceDefaultCredentialsProvider(registry,
						CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);
			}
		}

		private BeanDefinition resolveCredentialsProviderBeanDefinition(List<AWSCredentialsProvider> providers) {
			return providers.isEmpty()
					? BeanDefinitionBuilder.genericBeanDefinition(DefaultAWSCredentialsProviderChain.class)
							.getBeanDefinition()
					: BeanDefinitionBuilder.genericBeanDefinition(AWSCredentialsProviderChain.class)
							.addConstructorArgValue(providers).getBeanDefinition();
		}

		private AwsCredentialsProperties awsCredentialsProperties() {
			return Binder.get(this.environment).bindOrCreate(AwsCredentialsProperties.PREFIX,
					AwsCredentialsProperties.class);
		}

		private List<AWSCredentialsProvider> resolveCredentialsProviders(AwsCredentialsProperties properties) {
			List<AWSCredentialsProvider> providers = new ArrayList<>();

			if (StringUtils.hasText(properties.getAccessKey()) && StringUtils.hasText(properties.getSecretKey())) {
				providers.add(new AWSStaticCredentialsProvider(
						new BasicAWSCredentials(properties.getAccessKey(), properties.getSecretKey())));
			}

			if (properties.isInstanceProfile()) {
				providers.add(new EC2ContainerCredentialsProviderWrapper());
			}

			if (properties.getProfileName() != null) {
				providers.add(properties.getProfilePath() != null
						? new ProfileCredentialsProvider(properties.getProfilePath(), properties.getProfileName())
						: new ProfileCredentialsProvider(properties.getProfileName()));
			}

			return providers;
		}

	}

}
