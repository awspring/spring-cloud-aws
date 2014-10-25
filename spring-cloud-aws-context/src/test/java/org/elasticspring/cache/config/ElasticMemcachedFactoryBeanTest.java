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

package org.elasticspring.cache.config;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import org.elasticspring.cache.ElasticMemcachedFactoryBean;
import org.elasticspring.cache.memcached.MemcachedClient;
import org.elasticspring.core.env.ResourceIdResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticMemcachedFactoryBeanTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getObject_availableCluster_returnsConfiguredMemcachedClient() throws Exception {
		// Arrange
		int memcachedServerPort = TestMemcacheServer.startServer();
		AmazonElastiCache amazonElastiCache = mock(AmazonElastiCacheClient.class);

		when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).
				thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().withConfigurationEndpoint(
						new Endpoint().withAddress("localhost").withPort(memcachedServerPort)).withCacheClusterStatus("available").withEngine("memcached")));
		ElasticMemcachedFactoryBean elastiCacheAddressProvider = new ElasticMemcachedFactoryBean(amazonElastiCache, "memcached");

		// Act
		elastiCacheAddressProvider.afterPropertiesSet();
		MemcachedClient memcachedClient = elastiCacheAddressProvider.getObject();

		// Assert
		assertNotNull(memcachedClient);
		assertNull(memcachedClient.get("getObject_availableCluster_returnsConfiguredMemcachedClient"));

		memcachedClient.shutdown();

	}

	@Test
	public void getObject_availableClusterWithLogicalName_returnsConfigurationMemcachedClientWithPhysicalName() throws Exception {
		// Arrange
		int memcachedServerPort = TestMemcacheServer.startServer();

		AmazonElastiCache amazonElastiCache = mock(AmazonElastiCacheClient.class);
		when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).
				thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().withConfigurationEndpoint(
						new Endpoint().withAddress("localhost").withPort(memcachedServerPort)).withCacheClusterStatus("available").withEngine("memcached")));

		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId("test")).thenReturn("memcached");

		ElasticMemcachedFactoryBean elastiCacheAddressProvider = new ElasticMemcachedFactoryBean(amazonElastiCache, "test", resourceIdResolver);

		// Act
		elastiCacheAddressProvider.afterPropertiesSet();
		MemcachedClient memcachedClient = elastiCacheAddressProvider.getObject();

		// Assert
		assertNotNull(memcachedClient);
		assertNull(memcachedClient.get("getObject_availableClusterWithLogicalName_returnsConfigurationEndpointWithPhysicalName"));

		memcachedClient.shutdown();
	}

	@Test
	public void getObject_clusterWithRedisEngineConfigured_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("Currently only memcached");

		AmazonElastiCache amazonElastiCache = mock(AmazonElastiCacheClient.class);
		when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).
				thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().withEngine("redis")));

		ElasticMemcachedFactoryBean elastiCacheAddressProvider = new ElasticMemcachedFactoryBean(amazonElastiCache, "memcached");

		// Act
		elastiCacheAddressProvider.afterPropertiesSet();


		// Assert
	}
}