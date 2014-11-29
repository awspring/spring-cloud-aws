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

package org.springframework.cloud.aws.cache.config.annotation;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.cloud.aws.cache.config.TestMemcacheServer;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ElastiCacheCachingConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void enableElasticache_configuredWithExplicitCluster_configuresExplicitlyConfiguredCaches() throws Exception {
		//Arrange


		//Act
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithExplicitStackConfiguration.class);

		//Assert
		CacheInterceptor cacheInterceptor = this.context.getBean(CacheInterceptor.class);
		Collection<? extends Cache> caches = getCachesFromInterceptor(cacheInterceptor, "firstCache", "secondCache");
		assertEquals(2, caches.size());
	}

	@Test
	public void enableElasticache_configuredWithoutExplicitCluster_configuresImplicitlyConfiguredCaches() throws Exception {
		//Arrange


		//Act
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithNoExplicitStackConfiguration.class);

		//Assert
		CacheInterceptor cacheInterceptor = this.context.getBean(CacheInterceptor.class);
		Collection<? extends Cache> caches = getCachesFromInterceptor(cacheInterceptor, "sampleCacheOneLogical", "sampleCacheTwoLogical");
		assertEquals(2, caches.size());
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


	@EnableElastiCache(clusters = {"firstCache", "secondCache"})
	public static class ApplicationConfigurationWithExplicitStackConfiguration {

		@Bean
		public AmazonElastiCache amazonElastiCache() {
			AmazonElastiCache amazonElastiCache = Mockito.mock(AmazonElastiCache.class);
			int port = TestMemcacheServer.startServer();
			Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("firstCache"))).
					thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().
							withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(port)).
							withEngine("memcached")));
			Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("secondCache"))).
					thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().
							withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(port)).
							withEngine("memcached")));
			return amazonElastiCache;
		}
	}

	@EnableElastiCache
	public static class ApplicationConfigurationWithNoExplicitStackConfiguration {

		@Bean
		public AmazonElastiCache amazonElastiCache() {
			AmazonElastiCache amazonElastiCache = Mockito.mock(AmazonElastiCache.class);
			int port = TestMemcacheServer.startServer();
			Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("sampleCacheOneLogical"))).
					thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster().
							withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(port)).
							withEngine("memcached")));

			Mockito.when(amazonElastiCache.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("sampleCacheTwoLogical"))).
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
