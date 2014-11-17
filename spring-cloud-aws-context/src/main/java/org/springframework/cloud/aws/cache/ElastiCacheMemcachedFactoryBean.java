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

package org.springframework.cloud.aws.cache;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author Agim Emruli
 */
public class ElastiCacheMemcachedFactoryBean extends AbstractFactoryBean<MemcachedClient> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElastiCacheMemcachedFactoryBean.class);
	private static final int SHUTDOWN_DELAY = 10;

	private final AmazonElastiCache amazonElastiCache;
	private final String cacheClusterId;
	private final ResourceIdResolver resourceIdResolver;

	public ElastiCacheMemcachedFactoryBean(AmazonElastiCache amazonElastiCache, String cacheClusterId, ResourceIdResolver resourceIdResolver) {
		this.amazonElastiCache = amazonElastiCache;
		this.resourceIdResolver = resourceIdResolver;
		this.cacheClusterId = cacheClusterId;
	}

	public ElastiCacheMemcachedFactoryBean(AmazonElastiCache amazonElastiCache, String cacheClusterId) {
		this(amazonElastiCache, cacheClusterId, null);
	}

	@Override
	public Class<MemcachedClient> getObjectType() {
		return MemcachedClient.class;
	}

	@Override
	protected MemcachedClient createInstance() throws Exception {
		DescribeCacheClustersResult describeCacheClustersResult = this.amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId(getCacheClusterName()));

		CacheCluster cacheCluster = describeCacheClustersResult.getCacheClusters().get(0);
		if (!"available".equals(cacheCluster.getCacheClusterStatus())) {
			LOGGER.warn("Cache cluster is not available now. Connection may fail during cache access. Current status is {}", cacheCluster.getCacheClusterStatus());
		}

		if (!"memcached".equals(cacheCluster.getEngine())) {
			throw new IllegalStateException("Currently only memcached is supported as the cache cluster engine");
		}

		Endpoint configurationEndpoint = cacheCluster.getConfigurationEndpoint();

		// We return every time one configuration endpoint. The amazon memcached client will connect to all nodes.
		return new MemcachedClient(new InetSocketAddress(configurationEndpoint.getAddress(), configurationEndpoint.getPort()));
	}

	@Override
	protected void destroyInstance(MemcachedClient instance) throws Exception {
		boolean shutdownCompleted = instance.shutdown(SHUTDOWN_DELAY, TimeUnit.SECONDS);
		if (!shutdownCompleted) {
			LOGGER.warn("Error shutting down memcached client after :'" + SHUTDOWN_DELAY + "' seconds");
		}
	}

	private String getCacheClusterName() {
		return this.resourceIdResolver != null ? this.resourceIdResolver.resolveToPhysicalResourceId(this.cacheClusterId) : this.cacheClusterId;
	}
}
