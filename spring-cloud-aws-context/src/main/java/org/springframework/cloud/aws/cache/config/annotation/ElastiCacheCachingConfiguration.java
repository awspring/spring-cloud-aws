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

package org.springframework.cloud.aws.cache.config.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cloud.aws.cache.CacheFactory;
import org.springframework.cloud.aws.cache.memcached.MemcachedCacheFactory;
import org.springframework.cloud.aws.cache.redis.RedisCacheFactory;
import org.springframework.cloud.aws.context.annotation.ConditionalOnClass;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextDefaultConfigurationRegistrar.class)
public class ElastiCacheCachingConfiguration implements ImportAware {

	private static final String CACHE_CLUSTER_CONFIG_ATTRIBUTE_NAME = AnnotationUtils.VALUE;

	private static final String DEFAULT_EXPIRY_TIME_ATTRIBUTE_NAME = "defaultExpiration";

	private AnnotationAttributes annotationAttributes;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired(required = false)
	private AWSCredentialsProvider credentialsProvider;

	@Autowired(required = false)
	private ListableStackResourceFactory stackResourceFactory;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.annotationAttributes = AnnotationAttributes.fromMap(importMetadata
				.getAnnotationAttributes(EnableElastiCache.class.getName(), false));
		Assert.notNull(this.annotationAttributes,
				"@EnableElasticache is not present on importing class "
						+ importMetadata.getClassName());
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonElastiCache.class)
	public AmazonWebserviceClientFactoryBean<AmazonElastiCacheClient> amazonElastiCache() {
		return new AmazonWebserviceClientFactoryBean<>(AmazonElastiCacheClient.class,
				this.credentialsProvider, this.regionProvider);
	}

	@Bean
	public CachingConfigurer cachingConfigurer(AmazonElastiCache amazonElastiCache,
			ResourceIdResolver resourceIdResolver, List<CacheFactory> cacheFactories) {
		if (this.annotationAttributes != null
				&& this.annotationAttributes.getAnnotationArray("value").length > 0) {
			return new ElastiCacheCacheConfigurer(amazonElastiCache, resourceIdResolver,
					getCacheNamesFromCacheClusterConfigs(
							this.annotationAttributes.getAnnotationArray("value")),
					cacheFactories);
		}
		else {
			return new ElastiCacheCacheConfigurer(amazonElastiCache, resourceIdResolver,
					getConfiguredCachesInStack(), cacheFactories);
		}
	}

	@Bean
	@ConditionalOnClass("org.springframework.data.redis.connection.RedisConnectionFactory")
	public RedisCacheFactory redisCacheFactory() {
		RedisCacheFactory redisCacheFactory = new RedisCacheFactory();
		redisCacheFactory.setExpiryTimePerCache(
				getExpiryTimePerCacheFromAnnotationConfig(this.annotationAttributes
						.getAnnotationArray(CACHE_CLUSTER_CONFIG_ATTRIBUTE_NAME)));
		redisCacheFactory.setExpiryTime(this.annotationAttributes
				.<Integer>getNumber(DEFAULT_EXPIRY_TIME_ATTRIBUTE_NAME));
		return redisCacheFactory;
	}

	@Bean
	@ConditionalOnClass("net.spy.memcached.MemcachedClient")
	public MemcachedCacheFactory memcachedCacheFactory() {
		MemcachedCacheFactory redisCacheFactory = new MemcachedCacheFactory();
		redisCacheFactory.setExpiryTimePerCache(
				getExpiryTimePerCacheFromAnnotationConfig(this.annotationAttributes
						.getAnnotationArray(CACHE_CLUSTER_CONFIG_ATTRIBUTE_NAME)));
		redisCacheFactory.setExpiryTime(this.annotationAttributes
				.<Integer>getNumber(DEFAULT_EXPIRY_TIME_ATTRIBUTE_NAME));
		return redisCacheFactory;
	}

	private List<String> getCacheNamesFromCacheClusterConfigs(
			AnnotationAttributes[] annotationAttributes) {
		List<String> cacheNames = new ArrayList<>(annotationAttributes.length);
		for (AnnotationAttributes annotationAttribute : annotationAttributes) {
			cacheNames.add(annotationAttribute.getString("name"));
		}
		return cacheNames;
	}

	private Map<String, Integer> getExpiryTimePerCacheFromAnnotationConfig(
			AnnotationAttributes[] annotationAttributes) {
		Map<String, Integer> expiryTimePerCache = new HashMap<>(
				annotationAttributes.length);
		for (AnnotationAttributes annotationAttribute : annotationAttributes) {
			expiryTimePerCache.put(annotationAttribute.getString("name"),
					annotationAttribute.<Integer>getNumber("expiration"));
		}
		return expiryTimePerCache;
	}

	private List<String> getConfiguredCachesInStack() {
		List<String> cacheNames = new ArrayList<>();
		if (this.stackResourceFactory != null) {
			for (StackResource stackResource : this.stackResourceFactory
					.resourcesByType("AWS::ElastiCache::CacheCluster")) {
				cacheNames.add(stackResource.getLogicalId());
			}
		}
		return cacheNames;
	}

}
