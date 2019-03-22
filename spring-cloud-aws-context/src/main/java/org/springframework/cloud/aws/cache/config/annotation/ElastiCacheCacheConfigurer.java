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
import java.util.List;

import com.amazonaws.services.elasticache.AmazonElastiCache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.aws.cache.CacheFactory;
import org.springframework.cloud.aws.cache.ElastiCacheFactoryBean;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;

/**
 * @author Agim Emruli
 */
public class ElastiCacheCacheConfigurer extends CachingConfigurerSupport {

	private final AmazonElastiCache amazonElastiCache;

	private final ResourceIdResolver resourceIdResolver;

	private final List<String> cacheNames;

	private final List<CacheFactory> cacheFactories;

	public ElastiCacheCacheConfigurer(AmazonElastiCache amazonElastiCache,
			ResourceIdResolver resourceIdResolver, List<String> cacheNames,
			List<CacheFactory> cacheFactories) {
		this.cacheNames = cacheNames;
		this.amazonElastiCache = amazonElastiCache;
		this.resourceIdResolver = resourceIdResolver;
		this.cacheFactories = cacheFactories;
	}

	@Override
	public CacheManager cacheManager() {
		List<Cache> caches = new ArrayList<>(this.cacheNames.size());
		for (String cacheName : this.cacheNames) {
			caches.add(clusterCache(cacheName));
		}

		SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
		simpleCacheManager.setCaches(caches);
		simpleCacheManager.afterPropertiesSet();
		return simpleCacheManager;
	}

	protected Cache clusterCache(String cacheName) {
		try {
			ElastiCacheFactoryBean cacheFactoryBean = new ElastiCacheFactoryBean(
					this.amazonElastiCache, cacheName, this.resourceIdResolver,
					this.cacheFactories);
			cacheFactoryBean.afterPropertiesSet();
			return cacheFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new RuntimeException("Error creating cache", e);
		}
	}

}
