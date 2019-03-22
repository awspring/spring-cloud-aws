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

package org.springframework.cloud.aws.core.config;

import java.beans.Introspector;

import com.amazonaws.regions.Regions;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 */
public final class AmazonWebserviceClientConfigurationUtils {

	/**
	 * Name of the bean for region provider.
	 */
	// @checkstyle:off
	public static final String REGION_PROVIDER_BEAN_NAME = "org.springframework.cloud.aws.core.region.RegionProvider.BEAN_NAME";

	// @checkstyle:on

	/**
	 * Name of the bean for credentials provider.
	 */
	// @checkstyle:off
	public static final String CREDENTIALS_PROVIDER_BEAN_NAME = "org.springframework.cloud.aws.core.credentials.CredentialsProviderFactoryBean.BEAN_NAME";

	// @checkstyle:on
	private static final String SERVICE_IMPLEMENTATION_SUFFIX = "Client";

	private AmazonWebserviceClientConfigurationUtils() {
		// Avoid instantiation
	}

	public static BeanDefinitionHolder registerAmazonWebserviceClient(Object source,
			BeanDefinitionRegistry registry, String serviceNameClassName,
			String customRegionProvider, String customRegion) {

		String beanName = getBeanName(serviceNameClassName);

		if (registry.containsBeanDefinition(beanName)) {
			return new BeanDefinitionHolder(registry.getBeanDefinition(beanName),
					beanName);
		}

		BeanDefinition definition = getAmazonWebserviceClientBeanDefinition(source,
				serviceNameClassName, customRegionProvider, customRegion, registry);
		BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, beanName);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);

		return holder;
	}

	public static AbstractBeanDefinition getAmazonWebserviceClientBeanDefinition(
			Object source, String serviceNameClassName, String customRegionProvider,
			String customRegion, BeanDefinitionRegistry beanDefinitionRegistry) {

		if (StringUtils.hasText(customRegionProvider)
				&& StringUtils.hasText(customRegion)) {
			throw new IllegalArgumentException(
					"Only region or regionProvider can be configured, but not both");
		}

		registerCredentialsProviderIfNeeded(beanDefinitionRegistry);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(AmazonWebserviceClientFactoryBean.class);

		// Configure constructor parameters
		builder.addConstructorArgValue(serviceNameClassName);
		builder.addConstructorArgReference(CREDENTIALS_PROVIDER_BEAN_NAME);

		// Configure source of the bean definition
		builder.getRawBeanDefinition().setSource(source);

		// Configure region properties (either custom region provider or custom region)
		if (StringUtils.hasText(customRegionProvider)) {
			builder.addPropertyReference("regionProvider", customRegionProvider);
		}
		else if (StringUtils.hasText(customRegion)) {
			builder.addPropertyValue("customRegion", customRegion);
		}
		else {
			registerRegionProviderBeanIfNeeded(beanDefinitionRegistry);
			builder.addPropertyReference("regionProvider", REGION_PROVIDER_BEAN_NAME);
		}

		return builder.getBeanDefinition();
	}

	public static String getBeanName(String serviceClassName) {
		String clientClassName = ClassUtils.getShortName(serviceClassName);
		String shortenedClassName = StringUtils.delete(clientClassName,
				SERVICE_IMPLEMENTATION_SUFFIX);
		return Introspector.decapitalize(shortenedClassName);
	}

	public static String getRegionProviderBeanName(
			BeanDefinitionRegistry beanDefinitionRegistry) {
		registerRegionProviderBeanIfNeeded(beanDefinitionRegistry);
		return REGION_PROVIDER_BEAN_NAME;
	}

	public static void replaceDefaultRegionProvider(BeanDefinitionRegistry registry,
			String customGlobalRegionProvider) {
		if (registry.containsBeanDefinition(REGION_PROVIDER_BEAN_NAME)) {
			registry.removeBeanDefinition(REGION_PROVIDER_BEAN_NAME);
		}
		registry.registerAlias(customGlobalRegionProvider, REGION_PROVIDER_BEAN_NAME);
	}

	private static void registerRegionProviderBeanIfNeeded(
			BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(REGION_PROVIDER_BEAN_NAME)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(StaticRegionProvider.class);
			builder.addConstructorArgValue(Regions.DEFAULT_REGION.getName());
			builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(REGION_PROVIDER_BEAN_NAME,
					builder.getBeanDefinition());
		}
	}

	private static void registerCredentialsProviderIfNeeded(
			BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(CREDENTIALS_PROVIDER_BEAN_NAME)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(CredentialsProviderFactoryBean.class);
			builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(CREDENTIALS_PROVIDER_BEAN_NAME,
					builder.getBeanDefinition());
		}
	}

	public static void replaceDefaultCredentialsProvider(BeanDefinitionRegistry registry,
			String customGlobalCredentialsProvider) {
		if (registry.containsBeanDefinition(CREDENTIALS_PROVIDER_BEAN_NAME)) {
			registry.removeBeanDefinition(CREDENTIALS_PROVIDER_BEAN_NAME);
		}
		registry.registerAlias(customGlobalCredentialsProvider,
				CREDENTIALS_PROVIDER_BEAN_NAME);
	}

}
