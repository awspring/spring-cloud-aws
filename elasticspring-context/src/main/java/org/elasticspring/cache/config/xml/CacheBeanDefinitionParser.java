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

package org.elasticspring.cache.config.xml;

import org.elasticspring.cache.SimpleSpringMemcached;
import org.elasticspring.config.AmazonWebserviceClientConfigurationUtils;
import org.elasticspring.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Parser for the {@code <els-cache:cache-manager />} element.
 *
 * @author Alain Sahli
 * @author Agim Emruli
 * @since 1.0
 */
class CacheBeanDefinitionParser extends AbstractBeanDefinitionParser {

	private static final String CACHE_MANAGER = "cacheManager";
	private static final String CACHE_CLUSTER_ELEMENT_NAME = "cache-cluster";
	private static final String CACHE_REF_ELEMENT_NAME = "cache-ref";
	private static final String CACHE_ELEMENT_NAME = "cache";

	private static final String ELASTICACHE_MEMCACHE_CLIENT_FACTORY_BEAN = "org.elasticspring.cache.ElasticMemcachedFactoryBean";
	private static final String MEMCACHE_CLIENT_FACTORY_BEAN = "net.spy.memcached.spring.MemcachedClientFactoryBean";

	private static String getRequiredAttribute(String attributeName, Element source, ParserContext parserContext) {
		if (StringUtils.hasText(source.getAttribute(attributeName))) {
			return source.getAttribute(attributeName);
		} else {
			parserContext.getReaderContext().error("Attribute '" + attributeName + "' is required", source);
			return null;
		}
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		if (!parserContext.getRegistry().containsBeanDefinition(CACHE_MANAGER)) {
			BeanDefinitionBuilder cacheManagerDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(SimpleCacheManager.class);
			cacheManagerDefinitionBuilder.addPropertyValue("caches", createCacheCollection(element, parserContext));
			parserContext.getRegistry().registerBeanDefinition(CACHE_MANAGER, cacheManagerDefinitionBuilder.getBeanDefinition());
		} else {
			parserContext.getReaderContext().error("Only one cache manager can be defined", element);
		}
		return null;
	}

	private ManagedList<Object> createCacheCollection(Element element, ParserContext parserContext) {
		ManagedList<Object> caches = new ManagedList<Object>();
		List<Element> cacheElements = DomUtils.getChildElements(element);

		for (Element cacheElement : cacheElements) {
			String elementName = cacheElement.getLocalName();

			if (CACHE_REF_ELEMENT_NAME.equals(elementName)) {
				caches.add(new RuntimeBeanReference(cacheElement.getAttribute("ref")));
			} else if (CACHE_CLUSTER_ELEMENT_NAME.equals(elementName)) {
				String cacheClusterId = getRequiredAttribute("cacheCluster", cacheElement, parserContext);
				caches.add(createCache(cacheClusterId, createElastiCacheFactoryBean(parserContext.getRegistry(), cacheElement,
						cacheClusterId), cacheElement.getAttribute("expiration")));
			} else if (CACHE_ELEMENT_NAME.equals(elementName)) {
				String name = getRequiredAttribute("name", cacheElement, parserContext);
				String address = getRequiredAttribute("address", cacheElement, parserContext);
				caches.add(createCache(name, createStaticMemcachedFactoryBean(address), cacheElement.getAttribute("expiration")));
			}
		}
		return caches;
	}

	private static BeanDefinition createCache(String name, BeanDefinition memCachedClient, String expiration) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(SimpleSpringMemcached.class);
		beanDefinitionBuilder.addConstructorArgValue(memCachedClient);
		beanDefinitionBuilder.addConstructorArgValue(name);
		if (StringUtils.hasText(expiration)) {
			beanDefinitionBuilder.addPropertyValue("expiration", expiration);
		}
		return beanDefinitionBuilder.getBeanDefinition();
	}

	private static BeanDefinition createStaticMemcachedFactoryBean(String address) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(MEMCACHE_CLIENT_FACTORY_BEAN);
		beanDefinitionBuilder.addPropertyValue("servers", address);
		return beanDefinitionBuilder.getBeanDefinition();
	}

	private static BeanDefinition createElastiCacheFactoryBean(BeanDefinitionRegistry beanDefinitionRegistry, Element source, String clusterId) {
		BeanDefinitionHolder elastiCacheClient = AmazonWebserviceClientConfigurationUtils.registerAmazonWebserviceClient(beanDefinitionRegistry,
				"com.amazonaws.services.elasticache.AmazonElastiCacheClient", source.getAttribute("region-provider"), source.getAttribute("region"));
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(ELASTICACHE_MEMCACHE_CLIENT_FACTORY_BEAN);
		beanDefinitionBuilder.addConstructorArgReference(elastiCacheClient.getBeanName());
		beanDefinitionBuilder.addConstructorArgValue(clusterId);
		beanDefinitionBuilder.addConstructorArgReference(GlobalBeanDefinitionUtils.retrieveResourceIdResolverBeanName(beanDefinitionRegistry));
		return beanDefinitionBuilder.getBeanDefinition();
	}
}
