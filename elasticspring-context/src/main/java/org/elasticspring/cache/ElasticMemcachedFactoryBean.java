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

package org.elasticspring.cache;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import org.elasticspring.cache.memcached.MemcachedClient;
import org.elasticspring.core.env.ResourceIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.net.InetSocketAddress;

/**
 * @author Agim Emruli
 */
public class ElasticMemcachedFactoryBean extends AbstractFactoryBean<MemcachedClient> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticMemcachedFactoryBean.class);

	private final AmazonElastiCache amazonElastiCache;
	private final String cacheClusterId;
	private final ResourceIdResolver resourceIdResolver;

	public ElasticMemcachedFactoryBean(AmazonElastiCache amazonElastiCache, String cacheClusterId, ResourceIdResolver resourceIdResolver) {
		this.amazonElastiCache = amazonElastiCache;
		this.resourceIdResolver = resourceIdResolver;
		this.cacheClusterId = cacheClusterId;
	}

	public ElasticMemcachedFactoryBean(AmazonElastiCache amazonElastiCache, String cacheClusterId) {
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
		Endpoint configurationEndpoint = cacheCluster.getConfigurationEndpoint();

		// We return every time one configuration endpoint. The amazon memcached client will connect to all nodes.
		return new MemcachedClient(new InetSocketAddress(configurationEndpoint.getAddress(), configurationEndpoint.getPort()));
	}

	private String getCacheClusterName() {
		return this.resourceIdResolver != null ?  this.resourceIdResolver.resolveToPhysicalResourceId(this.cacheClusterId) : this.cacheClusterId;
	}

	@Override
	protected void destroyInstance(MemcachedClient instance) throws Exception {
		instance.shutdown();
	}
}
