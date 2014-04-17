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

package org.elasticspring.cache.config;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import net.spy.memcached.MemcachedClient;
import org.elasticspring.cache.ElasticMemcachedFactoryBean;
import org.elasticspring.core.env.ResourceIdResolver;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ElasticMemcachedFactoryBeanTest {

	@Test
	public void getObject_availableCluster_returnsConfiguredMemcachedClient() throws Exception {
		// Arrange
		int memcacheServerPort = TestMemcacheServer.startServer();
		AmazonElastiCache amazonElastiCache = Mockito.mock(AmazonElastiCacheClient.class);

		Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).
				thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(memcacheServerPort)).withCacheClusterStatus("available")));
		ElasticMemcachedFactoryBean elastiCacheAddressProvider = new ElasticMemcachedFactoryBean(amazonElastiCache, "memcached");

		// Act
		elastiCacheAddressProvider.afterPropertiesSet();
		MemcachedClient memcachedClient = elastiCacheAddressProvider.getObject();

		// Assert
		Assert.assertNotNull(memcachedClient);
		Assert.assertNull(memcachedClient.get("getObject_availableCluster_returnsConfiguredMemcachedClient"));

		memcachedClient.shutdown();

	}

	@Test
	public void getObject_availableClusterWithLogicalName_returnsConfigurationMemcacheClientWithPhysicalName() throws Exception {
		// Arrange
		int memcacheServerPort = TestMemcacheServer.startServer();

		AmazonElastiCache amazonElastiCache = Mockito.mock(AmazonElastiCacheClient.class);
		Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).
				thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(memcacheServerPort)).withCacheClusterStatus("available")));

		ResourceIdResolver resourceIdResolver = Mockito.mock(ResourceIdResolver.class);
		Mockito.when(resourceIdResolver.resolveToPhysicalResourceId("test")).thenReturn("memcached");

		ElasticMemcachedFactoryBean elastiCacheAddressProvider = new ElasticMemcachedFactoryBean(amazonElastiCache, "test", resourceIdResolver);

		// Act
		elastiCacheAddressProvider.afterPropertiesSet();
		MemcachedClient memcachedClient = elastiCacheAddressProvider.getObject();

		// Assert
		Assert.assertNotNull(memcachedClient);
		Assert.assertNull(memcachedClient.get("getObject_availableClusterWithLogicalName_returnsConfigurationEndpointWithPhysicalName"));

		memcachedClient.shutdown();
	}
}