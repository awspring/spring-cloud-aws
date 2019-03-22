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

package org.springframework.cloud.aws.cache.config.xml;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

import static org.springframework.cloud.aws.core.config.xml.XmlWebserviceConfigurationUtils.getCustomClientOrDefaultClientBeanName;

/**
 * Parser for the {@code <aws-cache:cache-manager />} element.
 *
 * @author Alain Sahli
 * @author Agim Emruli
 * @since 1.0
 */
class CacheBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	private static final String CACHE_MANAGER = "cacheManager";

	private static final String CACHE_CLUSTER_ELEMENT_NAME = "cache-cluster";

	private static final String CACHE_REF_ELEMENT_NAME = "cache-ref";

	private static final String ELASTICACHE_FACTORY_BEAN = "org.springframework.cloud.aws.cache.ElastiCacheFactoryBean";

	// @checkstyle:off
	private static final String ELASTI_CACHE_CLIENT_CLASS_NAME = "com.amazonaws.services.elasticache.AmazonElastiCacheClient";

	// @checkstyle:on

	// @checkstyle:off
	private static final String MEMCACHED_FACTORY_CLASS_NAME = "org.springframework.cloud.aws.cache.memcached.MemcachedCacheFactory";

	// @checkstyle:on

	// @checkstyle:off
	private static final String REDIS_CONNECTION_FACTORY_CLASS_NAME = "org.springframework.cloud.aws.cache.redis.RedisCacheFactory";

	// @checkstyle:on

	private static String getRequiredAttribute(String attributeName, Element source,
			ParserContext parserContext) {
		if (StringUtils.hasText(source.getAttribute(attributeName))) {
			return source.getAttribute(attributeName);
		}
		else {
			parserContext.getReaderContext()
					.error("Attribute '" + attributeName + "' is required", source);
			return null;
		}
	}

	private static ManagedList<BeanDefinition> createDefaultCacheFactories(
			Element element, ParserContext parserContext) {
		ManagedList<BeanDefinition> result = new ManagedList<>();
		result.setSource(parserContext.extractSource(element));
		if (ClassUtils.isPresent("net.spy.memcached.MemcachedClient",
				parserContext.getReaderContext().getBeanClassLoader())) {
			result.add(getCacheFactory(MEMCACHED_FACTORY_CLASS_NAME, element));
		}

		if (ClassUtils.isPresent(
				"org.springframework.data.redis.connection.RedisConnectionFactory",
				parserContext.getReaderContext().getBeanClassLoader())) {
			result.add(getCacheFactory(REDIS_CONNECTION_FACTORY_CLASS_NAME, element));
		}
		return result;
	}

	private static BeanDefinition createElastiCacheFactoryBean(Element source,
			ParserContext parserContext, String clusterId,
			ManagedList<BeanDefinition> cacheFactories) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(ELASTICACHE_FACTORY_BEAN);
		beanDefinitionBuilder.addConstructorArgReference(
				getCustomClientOrDefaultClientBeanName(source, parserContext,
						"amazon-elasti-cache", ELASTI_CACHE_CLIENT_CLASS_NAME));
		beanDefinitionBuilder.addConstructorArgValue(clusterId);
		beanDefinitionBuilder.addConstructorArgReference(GlobalBeanDefinitionUtils
				.retrieveResourceIdResolverBeanName(parserContext.getRegistry()));
		beanDefinitionBuilder.addConstructorArgValue(cacheFactories);
		return beanDefinitionBuilder.getBeanDefinition();
	}

	private static BeanDefinition getCacheFactory(String className, Element element) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(className);
		if (StringUtils.hasText(element.getAttribute("expiration"))) {
			builder.addPropertyValue("expiryTime", element.getAttribute("expiration"));
		}
		return builder.getBeanDefinition();
	}

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.cache.support.SimpleCacheManager";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		if (parserContext.getRegistry().containsBeanDefinition(CACHE_MANAGER)) {
			parserContext.getReaderContext()
					.error("Only one cache manager can be defined", element);
		}

		builder.addPropertyValue("caches", createCacheCollection(element, parserContext));
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition,
			ParserContext parserContext) throws BeanDefinitionStoreException {
		return CACHE_MANAGER;
	}

	private ManagedList<Object> createCacheCollection(Element element,
			ParserContext parserContext) {
		ManagedList<Object> caches = new ManagedList<>();
		List<Element> cacheElements = DomUtils.getChildElements(element);

		for (Element cacheElement : cacheElements) {
			String elementName = cacheElement.getLocalName();

			switch (elementName) {
			case CACHE_REF_ELEMENT_NAME:
				caches.add(new RuntimeBeanReference(cacheElement.getAttribute("ref")));
				break;
			case CACHE_CLUSTER_ELEMENT_NAME:
				String cacheClusterId = getRequiredAttribute("name", cacheElement,
						parserContext);
				caches.add(createElastiCacheFactoryBean(cacheElement, parserContext,
						cacheClusterId,
						createDefaultCacheFactories(cacheElement, parserContext)));
				break;
			default:
				parserContext.getReaderContext().error("Unknown element detected",
						parserContext.extractSource(cacheElement));
			}
		}
		return caches;
	}

}
