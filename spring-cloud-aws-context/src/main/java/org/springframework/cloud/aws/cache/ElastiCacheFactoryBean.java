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

package org.springframework.cloud.aws.cache;

import java.util.List;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cache.Cache;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;

/**
 * @author Agim Emruli
 */
public class ElastiCacheFactoryBean extends AbstractFactoryBean<Cache> {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ElastiCacheFactoryBean.class);

	private final AmazonElastiCache amazonElastiCache;

	private final String cacheClusterId;

	private final ResourceIdResolver resourceIdResolver;

	private final List<? extends CacheFactory> cacheFactories;

	public ElastiCacheFactoryBean(AmazonElastiCache amazonElastiCache,
			String cacheClusterId, ResourceIdResolver resourceIdResolver,
			List<? extends CacheFactory> cacheFactories) {
		this.amazonElastiCache = amazonElastiCache;
		this.resourceIdResolver = resourceIdResolver;
		this.cacheClusterId = cacheClusterId;
		this.cacheFactories = cacheFactories;
	}

	public ElastiCacheFactoryBean(AmazonElastiCache amazonElastiCache,
			String cacheClusterId, List<CacheFactory> cacheFactories) {
		this(amazonElastiCache, cacheClusterId, null, cacheFactories);
	}

	private static Endpoint getEndpointForCache(CacheCluster cacheCluster) {
		if (cacheCluster.getConfigurationEndpoint() != null) {
			return cacheCluster.getConfigurationEndpoint();
		}

		if (!cacheCluster.getCacheNodes().isEmpty()) {
			return cacheCluster.getCacheNodes().get(0).getEndpoint();
		}

		throw new IllegalArgumentException(
				"No Configuration Endpoint or Cache Node available to "
						+ "receive address information for cluster:'"
						+ cacheCluster.getCacheClusterId() + "'");
	}

	@Override
	public Class<Cache> getObjectType() {
		return Cache.class;
	}

	@Override
	protected Cache createInstance() throws Exception {
		DescribeCacheClustersRequest describeCacheClustersRequest = new DescribeCacheClustersRequest()
				.withCacheClusterId(getCacheClusterName());
		describeCacheClustersRequest.setShowCacheNodeInfo(true);

		DescribeCacheClustersResult describeCacheClustersResult = this.amazonElastiCache
				.describeCacheClusters(describeCacheClustersRequest);

		CacheCluster cacheCluster = describeCacheClustersResult.getCacheClusters().get(0);
		if (!"available".equals(cacheCluster.getCacheClusterStatus())) {
			LOGGER.warn(
					"Cache cluster is not available now. Connection may fail during cache access. Current status is {}",
					cacheCluster.getCacheClusterStatus());
		}

		Endpoint configurationEndpoint = getEndpointForCache(cacheCluster);

		for (CacheFactory cacheFactory : this.cacheFactories) {
			if (cacheFactory.isSupportingCacheArchitecture(cacheCluster.getEngine())) {
				return cacheFactory.createCache(this.cacheClusterId,
						configurationEndpoint.getAddress(),
						configurationEndpoint.getPort());
			}
		}

		throw new IllegalArgumentException(
				"No CacheFactory configured for engine: " + cacheCluster.getEngine());
	}

	@Override
	protected void destroyInstance(Cache instance) throws Exception {
		if (instance instanceof DisposableBean) {
			((DisposableBean) instance).destroy();
		}
	}

	private String getCacheClusterName() {
		return this.resourceIdResolver != null
				? this.resourceIdResolver.resolveToPhysicalResourceId(this.cacheClusterId)
				: this.cacheClusterId;
	}

}
