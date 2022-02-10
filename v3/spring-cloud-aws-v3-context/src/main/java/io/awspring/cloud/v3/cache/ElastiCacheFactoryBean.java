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

package io.awspring.cloud.v3.cache;

import java.util.List;

import io.awspring.cloud.v3.core.env.ResourceIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.cache.Cache;
import software.amazon.awssdk.services.elasticache.ElastiCacheClient;
import software.amazon.awssdk.services.elasticache.model.CacheCluster;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheClustersRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheClustersResponse;
import software.amazon.awssdk.services.elasticache.model.Endpoint;

/**
 * @author Agim Emruli
 */
public class ElastiCacheFactoryBean extends AbstractFactoryBean<Cache> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElastiCacheFactoryBean.class);

	private final ElastiCacheClient amazonElastiCache;

	private final String cacheClusterId;

	private final ResourceIdResolver resourceIdResolver;

	private final List<? extends CacheFactory> cacheFactories;

	public ElastiCacheFactoryBean(ElastiCacheClient amazonElastiCache,
								  String cacheClusterId,
								  ResourceIdResolver resourceIdResolver,
								  List<? extends CacheFactory> cacheFactories) {
		this.amazonElastiCache = amazonElastiCache;
		this.resourceIdResolver = resourceIdResolver;
		this.cacheClusterId = cacheClusterId;
		this.cacheFactories = cacheFactories;
	}

	public ElastiCacheFactoryBean(ElastiCacheClient amazonElastiCache, String cacheClusterId,
			List<CacheFactory> cacheFactories) {
		this(amazonElastiCache, cacheClusterId, null, cacheFactories);
	}

	private static Endpoint getEndpointForCache(CacheCluster cacheCluster) {
		if (cacheCluster.configurationEndpoint() != null) {
			return cacheCluster.configurationEndpoint();
		}

		if (!cacheCluster.cacheNodes().isEmpty()) {
			return cacheCluster.cacheNodes().get(0).endpoint();
		}

		throw new IllegalArgumentException("No Configuration Endpoint or Cache Node available to "
				+ "receive address information for cluster:'" + cacheCluster.cacheClusterId() + "'");
	}

	@Override
	public Class<Cache> getObjectType() {
		return Cache.class;
	}

	@Override
	protected Cache createInstance() throws Exception {
		DescribeCacheClustersRequest describeCacheClustersRequest =
			DescribeCacheClustersRequest.builder()
				.cacheClusterId(getCacheClusterName())
				.showCacheNodeInfo(true)
				.build();

		DescribeCacheClustersResponse describeCacheClustersResult = this.amazonElastiCache
				.describeCacheClusters(describeCacheClustersRequest);

		CacheCluster cacheCluster = describeCacheClustersResult.cacheClusters().get(0);
		if (!"available".equals(cacheCluster.cacheClusterStatus())) {
			LOGGER.warn(
					"Cache cluster is not available now. Connection may fail during cache access. Current status is {}",
					cacheCluster.cacheClusterStatus());
		}

		Endpoint configurationEndpoint = getEndpointForCache(cacheCluster);

		for (CacheFactory cacheFactory : this.cacheFactories) {
			if (cacheFactory.isSupportingCacheArchitecture(cacheCluster.engine())) {
				return cacheFactory.createCache(this.cacheClusterId, configurationEndpoint.address(),
						configurationEndpoint.port());
			}
		}

		throw new IllegalArgumentException("No CacheFactory configured for engine: " + cacheCluster.engine());
	}

	@Override
	protected void destroyInstance(Cache instance) throws Exception {
		if (instance instanceof DisposableBean) {
			((DisposableBean) instance).destroy();
		}
	}

	private String getCacheClusterName() {
		return this.resourceIdResolver != null
				? this.resourceIdResolver.resolveToPhysicalResourceId(this.cacheClusterId) : this.cacheClusterId;
	}

}
