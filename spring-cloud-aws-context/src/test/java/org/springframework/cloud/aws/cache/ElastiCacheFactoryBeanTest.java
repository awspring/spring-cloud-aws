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

import java.util.Collections;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.CacheNode;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cache.Cache;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElastiCacheFactoryBeanTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getObject_availableCluster_returnsConfiguredMemcachedClient()
			throws Exception {
		// Arrange
		AmazonElastiCache amazonElastiCache = mock(AmazonElastiCacheClient.class);

		DescribeCacheClustersRequest testCache = new DescribeCacheClustersRequest()
				.withCacheClusterId("testCache");
		testCache.setShowCacheNodeInfo(true);

		when(amazonElastiCache.describeCacheClusters(testCache)).thenReturn(
				new DescribeCacheClustersResult().withCacheClusters(new CacheCluster()
						.withConfigurationEndpoint(
								new Endpoint().withAddress("localhost").withPort(45678))
						.withCacheClusterStatus("available").withEngine("memcached")));
		ElastiCacheFactoryBean elasticCacheFactoryBean = new ElastiCacheFactoryBean(
				amazonElastiCache, "testCache", Collections.singletonList(
						new TestCacheFactory("testCache", "localhost", 45678)));

		// Act
		elasticCacheFactoryBean.afterPropertiesSet();
		Cache cache = elasticCacheFactoryBean.getObject();

		// Assert
		assertThat(cache).isNotNull();
	}

	@Test
	public void getObject_availableClusterWithLogicalName_returnsConfigurationMemcachedClientWithPhysicalName()
			throws Exception {
		// Arrange
		AmazonElastiCache amazonElastiCache = mock(AmazonElastiCacheClient.class);
		DescribeCacheClustersRequest testCache = new DescribeCacheClustersRequest()
				.withCacheClusterId("testCache");
		testCache.setShowCacheNodeInfo(true);

		when(amazonElastiCache.describeCacheClusters(testCache)).thenReturn(
				new DescribeCacheClustersResult().withCacheClusters(new CacheCluster()
						.withConfigurationEndpoint(
								new Endpoint().withAddress("localhost").withPort(45678))
						.withCacheClusterStatus("available").withEngine("memcached")));

		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		when(resourceIdResolver.resolveToPhysicalResourceId("test"))
				.thenReturn("testCache");

		ElastiCacheFactoryBean elastiCacheFactoryBean = new ElastiCacheFactoryBean(
				amazonElastiCache, "test", resourceIdResolver,
				Collections.<CacheFactory>singletonList(
						new TestCacheFactory("test", "localhost", 45678)));

		// Act
		elastiCacheFactoryBean.afterPropertiesSet();
		Cache cache = elastiCacheFactoryBean.getObject();

		// Assert
		assertThat(cache).isNotNull();
	}

	@Test
	public void getObject_clusterWithRedisEngineConfigured_reportsError()
			throws Exception {
		// Arrange
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("engine");

		AmazonElastiCache amazonElastiCache = mock(AmazonElastiCacheClient.class);
		DescribeCacheClustersRequest memcached = new DescribeCacheClustersRequest()
				.withCacheClusterId("memcached");
		memcached.setShowCacheNodeInfo(true);

		when(amazonElastiCache.describeCacheClusters(memcached))
				.thenReturn(new DescribeCacheClustersResult().withCacheClusters(
						new CacheCluster().withEngine("redis").withCacheNodes(
								new CacheNode().withEndpoint(new Endpoint()
										.withAddress("localhost").withPort(45678)))));

		ElastiCacheFactoryBean elastiCacheFactoryBean = new ElastiCacheFactoryBean(
				amazonElastiCache, "memcached", Collections.singletonList(
						new TestCacheFactory("testCache", "localhost", 45678)));

		// Act
		elastiCacheFactoryBean.afterPropertiesSet();

		// Assert
	}

	private static final class TestCacheFactory implements CacheFactory {

		private final String expectedCacheName;

		private final String expectedHostName;

		private final int expectedPort;

		private TestCacheFactory(String expectedCacheName, String expectedHostName,
				int expectedPort) {
			this.expectedCacheName = expectedCacheName;
			this.expectedHostName = expectedHostName;
			this.expectedPort = expectedPort;
		}

		@Override
		public boolean isSupportingCacheArchitecture(String architecture) {
			return "memcached".equals(architecture);
		}

		@Override
		public Cache createCache(String cacheName, String host, int port) {
			assertThat(this.expectedCacheName).isEqualTo(cacheName);
			assertThat(this.expectedHostName).isEqualTo(host);
			assertThat(this.expectedPort).isEqualTo(port);
			return mock(Cache.class);
		}

	}

}
