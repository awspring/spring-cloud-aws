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

package org.springframework.cloud.aws.cache.config.java;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import net.spy.memcached.MemcachedClientIF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cloud.aws.cache.ElasticMemcachedFactoryBean;
import org.springframework.cloud.aws.cache.SimpleSpringMemcached;
import org.springframework.cloud.aws.context.config.java.ContextCredentialsProviderConfiguration;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextCredentialsProviderConfiguration.class)
public class CacheConfiguration {

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired
	private Environment environment;

	@Bean
	@ConditionalOnMissingBean(name = "amazonElastiCache")
	public AmazonElastiCache amazonElastiCache(AWSCredentialsProvider credentialsProvider) {
		AmazonElastiCacheClient elastiCacheClient = new AmazonElastiCacheClient(credentialsProvider);
		if (this.regionProvider != null) {
			elastiCacheClient.setRegion(this.regionProvider.getRegion());
		}
		return elastiCacheClient;
	}

	@Bean
	@ConditionalOnMissingBean(name = "cacheManager")
	public CacheManager cacheManager(List<Cache> cache){
		SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
		simpleCacheManager.setCaches(cache);
		return simpleCacheManager;
	}

	@Bean
	@ConditionalOnProperty("cloud.aws.cache.name")
	@ConditionalOnMissingBean(name = "memcachedClient")
	public ElasticMemcachedFactoryBean memcachedClient(AmazonElastiCache amazonElastiCache,
													ResourceIdResolver resourceIdResolver) {
		return new ElasticMemcachedFactoryBean(amazonElastiCache,
				this.environment.getProperty("cloud.aws.cache.name"), resourceIdResolver);

	}

	//TODO: Add support for expiration
	@Bean
	@ConditionalOnClass(MemcachedClientIF.class)
	@ConditionalOnProperty("cloud.aws.cache.name")
	public Cache clusterCache(MemcachedClientIF memcachedClientIF){
		return new SimpleSpringMemcached(memcachedClientIF,
				this.environment.getProperty("cloud.aws.cache.name"));
	}
}