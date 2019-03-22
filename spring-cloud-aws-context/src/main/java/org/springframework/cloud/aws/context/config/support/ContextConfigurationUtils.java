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

package org.springframework.cloud.aws.context.config.support;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean;
import org.springframework.cloud.aws.core.region.Ec2MetadataRegionProvider;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 */
public final class ContextConfigurationUtils {

	/**
	 * Name of the region provider bean.
	 */
	public static final String REGION_PROVIDER_BEAN_NAME = "regionProvider";

	// @checkstyle:off
	private static final String POST_PROCESSOR_CLASS_NAME = "org.springframework.cloud.aws.context.config.AmazonEc2InstanceDataPropertySourcePostProcessor";

	// @checkstyle:on
	private static final String POST_PROCESSOR_BEAN_NAME = "AmazonEc2InstanceDataPropertySourcePostProcessor";

	private ContextConfigurationUtils() {
	}

	public static void registerRegionProvider(BeanDefinitionRegistry registry,
			boolean autoDetect, String configuredRegion) {
		if (autoDetect && StringUtils.hasText(configuredRegion)) {
			throw new IllegalArgumentException(
					"No region must be configured if autoDetect is defined as true");
		}

		AbstractBeanDefinition beanDefinition;

		if (autoDetect) {
			beanDefinition = BeanDefinitionBuilder
					.genericBeanDefinition(Ec2MetadataRegionProvider.class)
					.getBeanDefinition();
		}
		else if (StringUtils.hasText(configuredRegion)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(StaticRegionProvider.class);
			builder.addConstructorArgValue(configuredRegion);
			beanDefinition = builder.getBeanDefinition();
		}
		else {
			throw new IllegalArgumentException(
					"Region must be manually configured or autoDetect enabled");
		}

		BeanDefinitionReaderUtils.registerBeanDefinition(
				new BeanDefinitionHolder(beanDefinition, REGION_PROVIDER_BEAN_NAME),
				registry);
		AmazonWebserviceClientConfigurationUtils.replaceDefaultRegionProvider(registry,
				REGION_PROVIDER_BEAN_NAME);
	}

	public static void registerDefaultAWSCredentialsProvider(
			BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(DefaultAWSCredentialsProviderChain.class);
		registry.registerBeanDefinition(
				CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME,
				builder.getBeanDefinition());
		AmazonWebserviceClientConfigurationUtils.replaceDefaultCredentialsProvider(
				registry, CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);
	}

	public static void registerCredentialsProvider(BeanDefinitionRegistry registry,
			String accessKey, String secretKey, boolean instanceProfile,
			String profileName, String profilePath) {
		BeanDefinitionBuilder factoryBeanBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(CredentialsProviderFactoryBean.class);

		ManagedList<BeanDefinition> awsCredentialsProviders = new ManagedList<>();

		if (StringUtils.hasText(accessKey)) {
			BeanDefinitionBuilder credentials = BeanDefinitionBuilder
					.rootBeanDefinition(BasicAWSCredentials.class);
			credentials.addConstructorArgValue(accessKey);
			credentials.addConstructorArgValue(secretKey);

			BeanDefinitionBuilder provider = BeanDefinitionBuilder
					.rootBeanDefinition(AWSStaticCredentialsProvider.class);
			provider.addConstructorArgValue(credentials.getBeanDefinition());

			awsCredentialsProviders.add(provider.getBeanDefinition());
		}

		if (instanceProfile) {
			awsCredentialsProviders.add(BeanDefinitionBuilder
					.rootBeanDefinition(EC2ContainerCredentialsProviderWrapper.class)
					.getBeanDefinition());
		}

		if (StringUtils.hasText(profileName)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(ProfileCredentialsProvider.class);
			if (StringUtils.hasText(profilePath)) {
				builder.addConstructorArgValue(profilePath);
			}

			builder.addConstructorArgValue(profileName);
			awsCredentialsProviders.add(builder.getBeanDefinition());
		}

		factoryBeanBuilder.addConstructorArgValue(awsCredentialsProviders);

		registry.registerBeanDefinition(
				CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME,
				factoryBeanBuilder.getBeanDefinition());

		AmazonWebserviceClientConfigurationUtils.replaceDefaultCredentialsProvider(
				registry, CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);
	}

	public static void registerInstanceDataPropertySource(BeanDefinitionRegistry registry,
			String valueSeparator, String attributeSeparator) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(POST_PROCESSOR_CLASS_NAME);
		builder.addPropertyValue("valueSeparator", valueSeparator);
		builder.addPropertyValue("attributeSeparator", attributeSeparator);

		registry.registerBeanDefinition(POST_PROCESSOR_BEAN_NAME,
				builder.getBeanDefinition());
	}

}
