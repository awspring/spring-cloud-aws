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

package org.elasticspring.context.config.xml;

import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.w3c.dom.Element;

class AmazonElastiCacheClientConfigurationUtils {

	static final String ELASTICACHE_CLIENT_BEAN_NAME = "ELC_CLIENT";

	static BeanDefinitionHolder registerElastiCacheClient(
			BeanDefinitionRegistry registry, Element source) {

		if (!registry.containsBeanDefinition(ELASTICACHE_CLIENT_BEAN_NAME)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AmazonElastiCacheClient.class);
			builder.getRawBeanDefinition().setSource(source);
			builder.getRawBeanDefinition().setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			builder.addConstructorArgReference(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

			registry.registerBeanDefinition(ELASTICACHE_CLIENT_BEAN_NAME, builder.getBeanDefinition());
		}

		return new BeanDefinitionHolder(registry.getBeanDefinition(ELASTICACHE_CLIENT_BEAN_NAME), ELASTICACHE_CLIENT_BEAN_NAME);
	}
}