/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.cache;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import net.spy.memcached.MemcachedClientIF;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.aws.cache.ElasticMemcachedFactoryBean;
import org.springframework.cloud.aws.cache.SimpleSpringMemcached;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Agim Emruli
 */
public class ElasticCacheConfigurer extends CachingConfigurerSupport {

	private final AmazonElastiCache amazonElastiCache;

	private final ResourceIdResolver resourceIdResolver;

	private final List<String> cacheNames;

	public ElasticCacheConfigurer(AmazonElastiCache amazonElastiCache, ResourceIdResolver resourceIdResolver, List<String> cacheNames) {
		this.cacheNames = cacheNames;
		this.amazonElastiCache = amazonElastiCache;
		this.resourceIdResolver = resourceIdResolver;
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

	public Cache clusterCache(String cacheName) {
		try {
			ElasticMemcachedFactoryBean memcachedFactoryBean = new ElasticMemcachedFactoryBean(this.amazonElastiCache,
					cacheName, this.resourceIdResolver);
			memcachedFactoryBean.afterPropertiesSet();
			return new DisposableSpringSpringMemcached(memcachedFactoryBean.getObject(),
					cacheName);
		} catch (Exception e) {
			throw new RuntimeException("Error creating cache", e);
		}
	}

	private static class DisposableSpringSpringMemcached extends SimpleSpringMemcached implements DisposableBean {

		private final MemcachedClientIF memcachedClient;

		private DisposableSpringSpringMemcached(MemcachedClientIF memcachedClient, String cacheName) {
			super(memcachedClient, cacheName);
			this.memcachedClient = memcachedClient;
		}

		@Override
		public void destroy() throws Exception {
			this.memcachedClient.shutdown();
		}
	}
}
