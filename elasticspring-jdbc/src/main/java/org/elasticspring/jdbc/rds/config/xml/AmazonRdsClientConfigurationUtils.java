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

package org.elasticspring.jdbc.rds.config.xml;

import com.amazonaws.services.rds.AmazonRDSClient;
import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.MethodInvokingFactoryBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Utility class which configure the {@link com.amazonaws.services.rds.AmazonRDSClient} to make it available in the
 * application context
 *
 * @author Agim Emruli
 * @since 1.0
 */
class AmazonRdsClientConfigurationUtils {

	/**
	 * Default bean name used inside the application context for the Amazon RDS client
	 */
	static final String RDS_CLIENT_BEAN_NAME = "RDS_CLIENT";

	static BeanDefinitionHolder registerAmazonRdsClient(
			BeanDefinitionRegistry registry, Element source, ParserContext parserContext) {

		if (!registry.containsBeanDefinition(RDS_CLIENT_BEAN_NAME)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AmazonRDSClient.class);
			builder.getRawBeanDefinition().setSource(source);
			builder.getRawBeanDefinition().setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			builder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

			if (StringUtils.hasText(source.getAttribute("region-provider")) && StringUtils.hasText(source.getAttribute("region"))) {
				parserContext.getReaderContext().error("region and region-provider attribute must not be used together", source);
			}

			if (StringUtils.hasText(source.getAttribute("region-provider"))) {
				BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingFactoryBean.class);
				beanDefinitionBuilder.addPropertyValue("targetObject", new RuntimeBeanReference(source.getAttribute("region-provider")));
				beanDefinitionBuilder.addPropertyValue("targetMethod", "getRegion");
				builder.addPropertyValue("region", beanDefinitionBuilder.getBeanDefinition());
			} else if (StringUtils.hasText(source.getAttribute("region"))) {
				BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition("com.amazonaws.regions.Region");
				beanDefinitionBuilder.setFactoryMethod("getRegion");
				beanDefinitionBuilder.addConstructorArgValue(source.getAttribute("region"));
				builder.addPropertyValue("region", beanDefinitionBuilder.getBeanDefinition());
			}

			registry.registerBeanDefinition(RDS_CLIENT_BEAN_NAME, builder.getBeanDefinition());
		}

		return new BeanDefinitionHolder(registry.getBeanDefinition(RDS_CLIENT_BEAN_NAME), RDS_CLIENT_BEAN_NAME);
	}
}