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
import org.elasticspring.core.env.ResourceIdResolver;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.util.List;

public class ElastiCacheAddressProviderTest {

	@Test
	public void getAddresses_availableCluster_returnsConfigurationEndpoint() throws Exception {
		// Arrange
		AmazonElastiCache amazonElastiCache = Mockito.mock(AmazonElastiCacheClient.class);
		Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).
				thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().withConfigurationEndpoint(new Endpoint().withAddress("someHost").withPort(23)).withCacheClusterStatus("available")));
		ElastiCacheAddressProvider elastiCacheAddressProvider = new ElastiCacheAddressProvider(amazonElastiCache, "memcached");

		// Act
		List<InetSocketAddress> addresses = elastiCacheAddressProvider.getAddresses();

		// Assert
		Assert.assertNotNull(addresses);
		Assert.assertEquals(new InetSocketAddress("someHost", 23), addresses.get(0));
	}

	@Test
	public void getAddresses_availableClusterWithLogicalName_returnsConfigurationEndpointWithPhysicalName() throws Exception {
		// Arrange
		AmazonElastiCache amazonElastiCache = Mockito.mock(AmazonElastiCacheClient.class);
		Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).
				thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().withConfigurationEndpoint(new Endpoint().withAddress("someHost").withPort(23)).withCacheClusterStatus("available")));

		ResourceIdResolver resourceIdResolver = Mockito.mock(ResourceIdResolver.class);
		Mockito.when(resourceIdResolver.resolveToPhysicalResourceId("test")).thenReturn("memcached");

		ElastiCacheAddressProvider elastiCacheAddressProvider = new ElastiCacheAddressProvider(amazonElastiCache, resourceIdResolver, "test");

		// Act
		List<InetSocketAddress> addresses = elastiCacheAddressProvider.getAddresses();

		// Assert
		Assert.assertNotNull(addresses);
		Assert.assertEquals(new InetSocketAddress("someHost", 23), addresses.get(0));
	}
}
