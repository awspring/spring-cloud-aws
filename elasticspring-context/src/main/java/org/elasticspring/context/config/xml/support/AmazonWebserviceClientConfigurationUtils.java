/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.context.config.xml.support;

import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Agim Emruli
 */
public class AmazonWebserviceClientConfigurationUtils {

	private static final String SERVICE_IMPLEMENTATION_SUFFIX = "Client";

	public static BeanDefinitionHolder registerAmazonWebserviceClient(
			BeanDefinitionRegistry registry, String serviceNameClassName,
			String customRegionProvider, String customRegion) {

		String beanName = getBeanName(serviceNameClassName);
		if (registry.containsBeanDefinition(beanName)) {
			return new BeanDefinitionHolder(registry.getBeanDefinition(beanName), beanName);
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(serviceNameClassName);

		//Configure constructor parameters
		builder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

		//Configure destroy method
		builder.setDestroyMethodName("shutdown");

		//Configure region properties (either custom region provider or custom region)
		if (StringUtils.hasText(customRegionProvider)) {
			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingFactoryBean.class);
			beanDefinitionBuilder.addPropertyValue("targetObject", new RuntimeBeanReference(customRegionProvider));
			beanDefinitionBuilder.addPropertyValue("targetMethod", "getRegion");
			builder.addPropertyValue("region", beanDefinitionBuilder.getBeanDefinition());
		} else if (StringUtils.hasText(customRegion)) {
			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition("com.amazonaws.regions.Region");
			beanDefinitionBuilder.setFactoryMethod("getRegion");
			beanDefinitionBuilder.addConstructorArgValue(customRegion);
			builder.addPropertyValue("region", beanDefinitionBuilder.getBeanDefinition());
		}

		BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(builder.getBeanDefinition(), beanName);
		BeanDefinitionReaderUtils.registerBeanDefinition(beanDefinitionHolder, registry);
		return beanDefinitionHolder;
	}

	public static String getBeanName(String serviceClassName) {
		String shortClassName = ClassUtils.getShortName(serviceClassName);
		return StringUtils.delete(shortClassName, SERVICE_IMPLEMENTATION_SUFFIX);
	}
}