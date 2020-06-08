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

package org.springframework.cloud.aws.autoconfigure.cache;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cloud.aws.autoconfigure.context.MetaDataServer;
import org.springframework.cloud.aws.context.support.env.AwsCloudEnvironmentCheckUtils;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ElastiCacheAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@AfterAll
	static void shutDownHttpServer() {
		MetaDataServer.shutdownHttpServer();
	}

	@AfterAll
	static void shutdownCacheServer() throws Exception {
		TestMemcacheServer.stopServer();
	}

	@BeforeEach
	void restContextInstanceDataCondition() throws IllegalAccessException {
		Field field = ReflectionUtils.findField(AwsCloudEnvironmentCheckUtils.class,
				"isCloudEnvironment");
		assertThat(field).isNotNull();
		ReflectionUtils.makeAccessible(field);
		field.set(null, null);
	}

	@AfterEach
	void tearDown() throws Exception {
		this.context.close();
	}

	@Test
	void cacheManager_configuredMultipleCachesWithStack_configuresCacheManager()
			throws Exception {
		// Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext instanceIdHttpContext = httpServer.createContext(
				"/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("testInstanceId"));

		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MockCacheConfigurationWithStackCaches.class);
		this.context.register(ElastiCacheAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		CacheManager cacheManager = this.context.getBean(CachingConfigurer.class)
				.cacheManager();
		assertThat(cacheManager.getCacheNames().contains("sampleCacheOneLogical"))
				.isTrue();
		assertThat(cacheManager.getCacheNames().contains("sampleCacheTwoLogical"))
				.isTrue();
		assertThat(cacheManager.getCacheNames().size()).isEqualTo(2);

		httpServer.removeContext(instanceIdHttpContext);
	}

	@Test
	void cacheManager_configuredNoCachesWithNoStack_configuresNoCacheManager()
			throws Exception {
		// Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext instanceIdHttpContext = httpServer.createContext(
				"/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("testInstanceId"));

		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ElastiCacheAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		CacheManager cacheManager = this.context.getBean(CachingConfigurer.class)
				.cacheManager();
		assertThat(cacheManager.getCacheNames().size()).isEqualTo(0);

		httpServer.removeContext(instanceIdHttpContext);
	}

	@Configuration(proxyBeanMethods = false)
	static class MockCacheConfigurationWithStackCaches {

		@Bean
		AmazonElastiCache amazonElastiCache() {
			AmazonElastiCache amazonElastiCache = Mockito.mock(AmazonElastiCache.class);
			int port = TestMemcacheServer.startServer();
			DescribeCacheClustersRequest sampleCacheOneLogical = new DescribeCacheClustersRequest()
					.withCacheClusterId("sampleCacheOneLogical");
			sampleCacheOneLogical.setShowCacheNodeInfo(true);

			Mockito.when(amazonElastiCache.describeCacheClusters(sampleCacheOneLogical))
					.thenReturn(
							new DescribeCacheClustersResult()
									.withCacheClusters(
											new CacheCluster()
													.withConfigurationEndpoint(
															new Endpoint()
																	.withAddress(
																			"localhost")
																	.withPort(port))
													.withEngine("memcached")));

			DescribeCacheClustersRequest sampleCacheTwoLogical = new DescribeCacheClustersRequest()
					.withCacheClusterId("sampleCacheTwoLogical");
			sampleCacheTwoLogical.setShowCacheNodeInfo(true);

			Mockito.when(amazonElastiCache.describeCacheClusters(sampleCacheTwoLogical))
					.thenReturn(
							new DescribeCacheClustersResult()
									.withCacheClusters(
											new CacheCluster()
													.withConfigurationEndpoint(
															new Endpoint()
																	.withAddress(
																			"localhost")
																	.withPort(port))
													.withEngine("memcached")));
			return amazonElastiCache;
		}

		@Bean
		ListableStackResourceFactory stackResourceFactory() {
			ListableStackResourceFactory resourceFactory = Mockito
					.mock(ListableStackResourceFactory.class);
			Mockito.when(
					resourceFactory.resourcesByType("AWS::ElastiCache::CacheCluster"))
					.thenReturn(Arrays.asList(
							new StackResource("sampleCacheOneLogical", "sampleCacheOne",
									"AWS::ElastiCache::CacheCluster"),
							new StackResource("sampleCacheTwoLogical", "sampleCacheTwo",
									"AWS::ElastiCache::CacheCluster")));
			return resourceFactory;
		}

	}

}
