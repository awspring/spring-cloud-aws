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

import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.elasticspring.core.region.StaticRegionProvider;
import org.elasticspring.jdbc.rds.AmazonRdsClientFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Utility class which configure the {@link AmazonRdsClientFactoryBean} to make it available in the application context
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
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AmazonRdsClientFactoryBean.class);
			builder.getRawBeanDefinition().setSource(source);
			builder.getRawBeanDefinition().setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			builder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

			if (StringUtils.hasText(source.getAttribute("region-provider")) && StringUtils.hasText(source.getAttribute("region"))) {
				parserContext.getReaderContext().error("region and region-provider attribute must not be used together", source);
			}

			if (StringUtils.hasText(source.getAttribute("region-provider"))) {
				builder.addPropertyReference("regionProvider", source.getAttribute("region-provider"));
			} else {
				if (StringUtils.hasText(source.getAttribute("region"))) {
					BeanDefinitionBuilder regionProvider = BeanDefinitionBuilder.rootBeanDefinition(StaticRegionProvider.class);
					regionProvider.addConstructorArgValue(source.getAttribute("region"));
					builder.addPropertyValue("regionProvider", regionProvider.getBeanDefinition());
				}
			}

			registry.registerBeanDefinition(RDS_CLIENT_BEAN_NAME, builder.getBeanDefinition());
		}

		return new BeanDefinitionHolder(registry.getBeanDefinition(RDS_CLIENT_BEAN_NAME), RDS_CLIENT_BEAN_NAME);
	}
}