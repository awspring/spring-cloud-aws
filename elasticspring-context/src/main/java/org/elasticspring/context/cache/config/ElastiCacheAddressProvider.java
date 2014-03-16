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

package org.elasticspring.context.cache.config;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import com.google.code.ssm.config.AddressProvider;
import org.elasticspring.core.env.ResourceIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

/**
 * @author Agim Emruli
 */
public class ElastiCacheAddressProvider implements AddressProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElastiCacheAddressProvider.class);

	private final AmazonElastiCache amazonElastiCache;
	private final String cacheClusterId;

	public ElastiCacheAddressProvider(AmazonElastiCache amazonElastiCache, ResourceIdResolver resourceIdResolver, String cacheClusterId) {
		this(amazonElastiCache, resourceIdResolver.resolveToPhysicalResourceId(cacheClusterId));
	}

	public ElastiCacheAddressProvider(AmazonElastiCache amazonElastiCache, String cacheClusterId) {
		this.amazonElastiCache = amazonElastiCache;
		this.cacheClusterId = cacheClusterId;
	}

	@Override
	public List<InetSocketAddress> getAddresses() {
		DescribeCacheClustersResult describeCacheClustersResult = this.amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId(this.cacheClusterId));

		CacheCluster cacheCluster = describeCacheClustersResult.getCacheClusters().get(0);
		if (!"available".equals(cacheCluster.getCacheClusterStatus())) {
			LOGGER.warn("Cache cluster is not available now. Connection may fail during cache access. Current status is {}", cacheCluster.getCacheClusterStatus());
		}
		Endpoint configurationEndpoint = cacheCluster.getConfigurationEndpoint();

		// We return every time one configuration endpoint. The amazon memcached client will connect to all nodes.
		return Collections.singletonList(new InetSocketAddress(configurationEndpoint.getAddress(),configurationEndpoint.getPort()));
	}
}