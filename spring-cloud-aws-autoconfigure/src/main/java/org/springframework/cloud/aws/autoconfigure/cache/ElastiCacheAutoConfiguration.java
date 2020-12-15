/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.cache;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import net.spy.memcached.MemcachedClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cloud.aws.autoconfigure.context.ContextCredentialsAutoConfiguration;
import org.springframework.cloud.aws.cache.CacheFactory;
import org.springframework.cloud.aws.cache.config.annotation.ElastiCacheCacheConfigurer;
import org.springframework.cloud.aws.cache.memcached.MemcachedCacheFactory;
import org.springframework.cloud.aws.cache.redis.RedisCacheFactory;
import org.springframework.cloud.aws.context.config.annotation.ContextDefaultConfigurationRegistrar;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientFactoryBean;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;

/**
 * @author Agim Emruli
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 */
@Configuration(proxyBeanMethods = false)
@Import({ ContextCredentialsAutoConfiguration.class, ContextDefaultConfigurationRegistrar.class })
@ConditionalOnClass(com.amazonaws.services.elasticache.AmazonElastiCache.class)
@ConditionalOnProperty(name = "cloud.aws.elasticache.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ElastiCacheProperties.class)
public class ElastiCacheAutoConfiguration {

	private final ElastiCacheProperties properties;

	private final ListableStackResourceFactory stackResourceFactory;

	private final ClientConfiguration clientConfiguration;

	public ElastiCacheAutoConfiguration(ElastiCacheProperties properties,
			ObjectProvider<ListableStackResourceFactory> stackResourceFactory,
			@Qualifier(GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME) ObjectProvider<ClientConfiguration> globalClientConfiguration,
			@Qualifier("elastiCacheClientConfiguration") ObjectProvider<ClientConfiguration> elastiCacheClientConfiguration) {
		this.properties = properties;
		this.stackResourceFactory = stackResourceFactory.getIfAvailable();
		this.clientConfiguration = elastiCacheClientConfiguration
				.getIfAvailable(globalClientConfiguration::getIfAvailable);
	}

	@Bean
	@ConditionalOnMissingBean(AmazonElastiCache.class)
	public AmazonWebserviceClientFactoryBean<AmazonElastiCacheClient> amazonElastiCache(
			ObjectProvider<RegionProvider> regionProvider, ObjectProvider<AWSCredentialsProvider> credentialsProvider) {
		return new AmazonWebserviceClientFactoryBean<>(AmazonElastiCacheClient.class,
				credentialsProvider.getIfAvailable(), regionProvider.getIfAvailable(), clientConfiguration);
	}

	@Bean
	@ConditionalOnMissingBean(CachingConfigurer.class)
	public CachingConfigurer cachingConfigurer(AmazonElastiCache amazonElastiCache,
			ResourceIdResolver resourceIdResolver, List<CacheFactory> cacheFactories) {
		if (!this.properties.getClusters().isEmpty()) {
			return new ElastiCacheCacheConfigurer(amazonElastiCache, resourceIdResolver,
					this.properties.getCacheNames(), cacheFactories);
		}
		else {
			return new ElastiCacheCacheConfigurer(amazonElastiCache, resourceIdResolver, getConfiguredCachesInStack(),
					cacheFactories);
		}
	}

	@Bean
	@ConditionalOnClass(RedisConnectionFactory.class)
	public RedisCacheFactory redisCacheFactory() {
		return new RedisCacheFactory(this.properties.getExpiryTimePerCache(), this.properties.getDefaultExpiration());
	}

	@Bean
	@ConditionalOnClass(MemcachedClient.class)
	public MemcachedCacheFactory memcachedCacheFactory() {
		return new MemcachedCacheFactory(this.properties.getExpiryTimePerCache(),
				this.properties.getDefaultExpiration());
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
