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

package org.springframework.cloud.aws.autoconfigure.cache;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class CacheConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		this.context.close();
	}

	@Test
	public void cacheManager_configuredWithCluster_configuresCacheManager() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MockCacheConfiguration.class);
		this.context.register(CacheConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "cloud.aws.cache.name:sampleCache");

		//Act
		this.context.refresh();

		//Assert
		CacheInterceptor cacheInterceptor = this.context.getBean(CacheInterceptor.class);
		Collection<? extends Cache> caches = getCachesFromInterceptor(cacheInterceptor, "sampleCache");
		assertEquals(1, caches.size());
	}

	@Test
	public void cacheManager_configuredMultipleCachesWithStack_configuresCacheManager() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MockCacheConfigurationWithStackCaches.class);
		this.context.register(CacheConfiguration.class);

		//Act
		this.context.refresh();

		//Assert
		CacheInterceptor cacheInterceptor = this.context.getBean(CacheInterceptor.class);
		Collection<? extends Cache> caches = getCachesFromInterceptor(cacheInterceptor, "sampleCacheOne", "sampleCacheTwo");
		assertEquals(2, caches.size());
	}

	@Test
	public void cacheManager_configuredNoCachesWithNoStack_configuresNoCacheManager() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(CacheConfiguration.class);

		//Act
		this.context.refresh();

		//Assert
		CacheInterceptor cacheInterceptor = this.context.getBean(CacheInterceptor.class);
		Collection<? extends Cache> caches = getCachesFromInterceptor(cacheInterceptor);
		assertEquals(0, caches.size());
	}

	@AfterClass
	public static void shutdownCacheServer() throws Exception {
		TestMemcacheServer.stopServer();
	}

	@Configuration
	public static class MockCacheConfiguration {

		@Bean
		public AmazonElastiCache amazonElastiCache() {
			AmazonElastiCache amazonElastiCache = Mockito.mock(AmazonElastiCache.class);
			int port = TestMemcacheServer.startServer();
			Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("sampleCache"))).
					thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().
							withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(port)).
							withEngine("memcached")));
			return amazonElastiCache;
		}
	}

	private static Collection<? extends Cache> getCachesFromInterceptor(CacheInterceptor cacheInterceptor, final String... cacheNames) {
		return cacheInterceptor.getCacheResolver().resolveCaches(new CacheOperationInvocationContext<BasicOperation>() {

			@Override
			public BasicOperation getOperation() {
				CacheableOperation cacheableOperation = new CacheableOperation();
				cacheableOperation.setCacheNames(cacheNames);
				return cacheableOperation;
			}

			@Override
			public Object getTarget() {
				return null;
			}

			@Override
			public Method getMethod() {
				return null;
			}

			@Override
			public Object[] getArgs() {
				return new Object[0];
			}
		});
	}

	@Configuration
	public static class MockCacheConfigurationWithStackCaches {

		@Bean
		public AmazonElastiCache amazonElastiCache() {
			AmazonElastiCache amazonElastiCache = Mockito.mock(AmazonElastiCache.class);
			int port = TestMemcacheServer.startServer();
			Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("sampleCacheOne"))).
					thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().
							withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(port)).
							withEngine("memcached")));

			Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("sampleCacheTwo"))).
					thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().
							withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(port)).
							withEngine("memcached")));
			return amazonElastiCache;
		}

		@Bean
		public ListableStackResourceFactory stackResourceFactory() {
			ListableStackResourceFactory resourceFactory = Mockito.mock(ListableStackResourceFactory.class);
			Mockito.when(resourceFactory.resourcesByType("AWS::ElastiCache::CacheCluster")).thenReturn(Arrays.asList(
					new StackResource("sampleCacheOneLogical", "sampleCacheOne", "AWS::ElastiCache::CacheCluster"),
					new StackResource("sampleCacheTwoLogical", "sampleCacheTwo", "AWS::ElastiCache::CacheCluster")));
			return resourceFactory;
		}
	}
}