/*
 * Copyright 2013-2020 the original author or authors.
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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cloud.aws.cache.memcached.SimpleSpringMemcached;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;

/**
 * Tests for {@link ElastiCacheAutoConfiguration}.
 *
 * @author Maciej Walkowiak
 */
class ElastiCacheAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ElastiCacheAutoConfiguration.class));

	private static int getExpirationFromCache(Cache cache) throws IllegalAccessException {
		assertThat(cache instanceof SimpleSpringMemcached).isTrue();
		Field expiration = ReflectionUtils.findField(SimpleSpringMemcached.class, "expiration");
		assertThat(expiration).isNotNull();
		ReflectionUtils.makeAccessible(expiration);
		return expiration.getInt(cache);
	}

	@Test
	void cacheManager_configuredMultipleCachesWithStack_configuresCacheManager() {
		this.contextRunner.withUserConfiguration(MockCacheConfigurationWithStackCaches.class).run(context -> {
			CacheManager cacheManager = context.getBean(CachingConfigurer.class).cacheManager();
			assertThat(cacheManager.getCacheNames().contains("sampleCacheOneLogical")).isTrue();
			assertThat(cacheManager.getCacheNames().contains("sampleCacheTwoLogical")).isTrue();
			assertThat(cacheManager.getCacheNames()).hasSize(2);
		});
	}

	@Test
	void cacheManager_configuredNoCachesWithNoStack_configuresNoCacheManager() {
		this.contextRunner.run(context -> {
			CacheManager cacheManager = context.getBean(CachingConfigurer.class).cacheManager();
			assertThat(cacheManager.getCacheNames()).isEmpty();
		});
	}

	@Test
	public void elastiCacheIsDisabled() {
		this.contextRunner.withPropertyValues("cloud.aws.elasticache.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(CachingConfigurer.class));
	}

	@Test
	void enableElasticache_configuredWithExplicitCluster_configuresExplicitlyConfiguredCaches() {
		this.contextRunner
				.withPropertyValues("cloud.aws.elasticache.clusters[0].name=sampleCacheOneLogical",
						"cloud.aws.elasticache.clusters[1].name=sampleCacheTwoLogical")
				.withUserConfiguration(MockCacheConfigurationWithStackCaches.class).run(context -> {
					// Assert
					CacheManager cacheManager = context.getBean(CachingConfigurer.class).cacheManager();
					assertThat(cacheManager.getCacheNames()).hasSize(2);
					Cache firstCache = cacheManager.getCache("sampleCacheOneLogical");
					assertThat(firstCache.getName()).isNotNull();
					assertThat(getExpirationFromCache(firstCache)).isEqualTo(0);

					Cache secondCache = cacheManager.getCache("sampleCacheTwoLogical");
					assertThat(secondCache.getName()).isNotNull();
					assertThat(getExpirationFromCache(secondCache)).isEqualTo(0);
				});
	}

	@Test
	void enableElasticache_configuredWithExplicitClusterAndExpiration_configuresExplicitlyConfiguredCachesWithCustomExpirationTimes() {
		this.contextRunner
				.withPropertyValues("cloud.aws.elasticache.clusters[0].name=sampleCacheOneLogical",
						"cloud.aws.elasticache.clusters[0].expiration=23",
						"cloud.aws.elasticache.clusters[1].name=sampleCacheTwoLogical",
						"cloud.aws.elasticache.clusters[1].expiration=42")
				.withUserConfiguration(MockCacheConfigurationWithStackCaches.class).run(context -> {
					CacheManager cacheManager = context.getBean(CachingConfigurer.class).cacheManager();
					assertThat(cacheManager.getCacheNames()).hasSize(2);
					Cache firstCache = cacheManager.getCache("sampleCacheOneLogical");
					assertThat(firstCache.getName()).isNotNull();
					assertThat(getExpirationFromCache(firstCache)).isEqualTo(23);

					Cache secondCache = cacheManager.getCache("sampleCacheTwoLogical");
					assertThat(secondCache.getName()).isNotNull();
					assertThat(getExpirationFromCache(secondCache)).isEqualTo(42);
				});
	}

	@Test
	void enableElasticache_configuredWithExplicitClusterAndExpiration_configuresExplicitlyConfiguredCachesWithMixedExpirationTimes() {
		this.contextRunner
				.withPropertyValues("cloud.aws.elasticache.default-expiration=12",
						"cloud.aws.elasticache.clusters[0].name=sampleCacheOneLogical",
						"cloud.aws.elasticache.clusters[1].name=sampleCacheTwoLogical",
						"cloud.aws.elasticache.clusters[1].expiration=42")
				.withUserConfiguration(MockCacheConfigurationWithStackCaches.class).run(context -> {
					CacheManager cacheManager = context.getBean(CachingConfigurer.class).cacheManager();
					assertThat(cacheManager.getCacheNames()).hasSize(2);
					Cache firstCache = cacheManager.getCache("sampleCacheOneLogical");
					assertThat(firstCache.getName()).isNotNull();
					assertThat(getExpirationFromCache(firstCache)).isEqualTo(12);

					Cache secondCache = cacheManager.getCache("sampleCacheTwoLogical");
					assertThat(secondCache.getName()).isNotNull();
					assertThat(getExpirationFromCache(secondCache)).isEqualTo(42);
				});
	}

	@Test
	void enableElasticache_configuredWithoutExplicitCluster_configuresImplicitlyConfiguredCaches() {
		this.contextRunner.withUserConfiguration(MockCacheConfigurationWithStackCaches.class).run(context -> {
			CacheManager cacheManager = context.getBean(CachingConfigurer.class).cacheManager();
			assertThat(cacheManager.getCacheNames()).hasSize(2);
			Cache firstCache = cacheManager.getCache("sampleCacheOneLogical");
			assertThat(firstCache.getName()).isNotNull();
			assertThat(getExpirationFromCache(firstCache)).isEqualTo(0);

			Cache secondCache = cacheManager.getCache("sampleCacheTwoLogical");
			assertThat(secondCache.getName()).isNotNull();
			assertThat(getExpirationFromCache(secondCache)).isEqualTo(0);
		});
	}

	@Test
	void enableElasticache_configuredWithoutExplicitClusterButDefaultExpiryTime_configuresImplicitlyConfiguredCachesWithDefaultExpiryTimeOnAllCaches() {
		this.contextRunner.withPropertyValues("cloud.aws.elasticache.default-expiration=23")
				.withUserConfiguration(MockCacheConfigurationWithStackCaches.class).run(context -> {
					CacheManager cacheManager = context.getBean(CachingConfigurer.class).cacheManager();
					assertThat(cacheManager.getCacheNames()).hasSize(2);
					Cache firstCache = cacheManager.getCache("sampleCacheOneLogical");
					assertThat(firstCache.getName()).isNotNull();
					assertThat(getExpirationFromCache(firstCache)).isEqualTo(23);

					Cache secondCache = cacheManager.getCache("sampleCacheTwoLogical");
					assertThat(secondCache.getName()).isNotNull();
					assertThat(getExpirationFromCache(secondCache)).isEqualTo(23);
				});
	}

	@Test
	void customCachingConfigurerProvided_doesNotCreateDefaultOne() {
		this.contextRunner.withUserConfiguration(MockCacheConfigurationWithStackCaches.class,
				CustomCacheConfigurerConfiguration.class).run(context -> {
					assertThat(context.containsBean("cachingConfigurer")).isFalse();
					assertThat(context.getBean("customCachingConfigurer", CachingConfigurer.class)).isNotNull();
				});
	}

	@Test
	void customElastiCacheClientProvided_doesNotCreateDefaultOne() {
		this.contextRunner.withUserConfiguration(CustomElastiCacheClientConfiguration.class).run(context -> {
			assertThat(context.containsBean("amazonElastiCache")).isFalse();
			assertThat(context.getBean("customAmazonElastiCache", AmazonElastiCache.class)).isNotNull();
		});
	}

	@Test
	void configuration_withGlobalClientConfiguration_shouldUseItForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithGlobalClientConfiguration.class).run((context) -> {
			AmazonElastiCache client = context.getBean(AmazonElastiCache.class);

			// Assert
			ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
					"clientConfiguration");
			assertThat(clientConfiguration.getProxyHost()).isEqualTo("global");
		});
	}

	@Test
	void configuration_withElastiCacheClientConfiguration_shouldUseItForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithElastiCacheClientConfiguration.class)
				.run((context) -> {
					AmazonElastiCache client = context.getBean(AmazonElastiCache.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
							"clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("elastiCache");
				});
	}

	@Test
	void configuration_withGlobalAndElastiCacheClientConfigurations_shouldUseSqsConfigurationForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithGlobalAndElastiCacheClientConfiguration.class)
				.run((context) -> {
					AmazonElastiCache client = context.getBean(AmazonElastiCache.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
							"clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("elastiCache");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class MockCacheConfigurationWithStackCaches {

		@Bean
		AmazonElastiCache amazonElastiCache() {
			AmazonElastiCache amazonElastiCache = mock(AmazonElastiCache.class);
			int port = TestMemcacheServer.startServer();
			DescribeCacheClustersRequest sampleCacheOneLogical = new DescribeCacheClustersRequest()
					.withCacheClusterId("sampleCacheOneLogical");
			sampleCacheOneLogical.setShowCacheNodeInfo(true);

			Mockito.when(amazonElastiCache.describeCacheClusters(sampleCacheOneLogical))
					.thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster()
							.withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(port))
							.withEngine("memcached")));

			DescribeCacheClustersRequest sampleCacheTwoLogical = new DescribeCacheClustersRequest()
					.withCacheClusterId("sampleCacheTwoLogical");
			sampleCacheTwoLogical.setShowCacheNodeInfo(true);

			Mockito.when(amazonElastiCache.describeCacheClusters(sampleCacheTwoLogical))
					.thenReturn(new DescribeCacheClustersResult().withCacheClusters(new CacheCluster()
							.withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(port))
							.withEngine("memcached")));
			return amazonElastiCache;
		}

		@Bean
		ListableStackResourceFactory stackResourceFactory() {
			ListableStackResourceFactory resourceFactory = mock(ListableStackResourceFactory.class);
			Mockito.when(resourceFactory.resourcesByType("AWS::ElastiCache::CacheCluster")).thenReturn(Arrays.asList(
					new StackResource("sampleCacheOneLogical", "sampleCacheOne", "AWS::ElastiCache::CacheCluster"),
					new StackResource("sampleCacheTwoLogical", "sampleCacheTwo", "AWS::ElastiCache::CacheCluster")));
			return resourceFactory;
		}

	}

	@Configuration
	static class CustomCacheConfigurerConfiguration {

		@Bean
		CachingConfigurer customCachingConfigurer() {
			return mock(CachingConfigurer.class);
		}

	}

	@Configuration
	static class CustomElastiCacheClientConfiguration {

		@Bean
		AmazonElastiCache customAmazonElastiCache() {
			return mock(AmazonElastiCache.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalClientConfiguration {

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithElastiCacheClientConfiguration {

		@Bean
		ClientConfiguration elastiCacheClientConfiguration() {
			return new ClientConfiguration().withProxyHost("elastiCache");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalAndElastiCacheClientConfiguration {

		@Bean
		ClientConfiguration elastiCacheClientConfiguration() {
			return new ClientConfiguration().withProxyHost("elastiCache");
		}

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

}
