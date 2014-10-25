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

package org.springframework.cloud.aws.cache.config.xml;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import org.springframework.cloud.aws.cache.config.TestMemcacheServer;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.elasticspring.core.config.AmazonWebserviceClientConfigurationUtils;
import org.elasticspring.core.env.ResourceIdResolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class CacheBeanDefinitionParserTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void parseInternal_manualCacheConfig_returnsConfiguredCache() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-manualCacheConfig.xml", getClass());

		//Act
		CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
		Cache cache = cacheManager.getCache("memc");
		cache.put("foo", "bar");
		cache.evict("foo");

		//Assert
		assertNotNull(cacheManager);
		assertNotNull(cache);
	}

	@Test
	public void parseInternal_manualCacheConfigWithExpiration_returnsConfiguredCacheThatRespectExpiration() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-manualCacheConfigWithExpiration.xml", getClass());

		//Act
		CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
		Cache cache = cacheManager.getCache("memc");
		cache.put("foo", "bar");

		//Assert
		assertNotNull(cacheManager);
		assertNotNull(cache);
		assertNull(cache.get("foo"));
	}

	@Test
	public void parseInternal_customCache_returnsCacheManagerWithCustomCache() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customCache.xml", getClass());

		//Act
		CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
		Cache cache = cacheManager.getCache("memc");
		cache.put("foo", "bar");
		cache.evict("foo");
		//Assert
		assertNotNull(cacheManager);
		assertNotNull(cache);
	}

	@Test
	public void parseInternal_mixedCacheConfig_returnsBothCaches() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-mixedCacheConfig.xml", getClass());

		//Act
		CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
		Cache memc = cacheManager.getCache("memc");
		Cache memcached = cacheManager.getCache("memcached");

		//Assert
		assertNotNull(cacheManager);
		assertNotNull(memcached);

		memc.put("foo", "bar");
		memc.evict("foo");

		memcached.put("foo", "bar");
		memcached.evict("foo");
	}

	@Test
	public void parseInternal_clusterCacheConfiguration_returnsConfiguredClusterCache() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Register a mock object which will be used to replay service calls
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonElastiCache.class);
		beanFactory.registerBeanDefinition(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonElastiCacheClient.class.getName()), beanDefinitionBuilder.getBeanDefinition());

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-elastiCacheConfig.xml", getClass()));


		AmazonElastiCache client = beanFactory.getBean(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonElastiCacheClient.class.getName()), AmazonElastiCache.class);

		//Replay invocation that will be called
		when(client.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).thenReturn(
				new DescribeCacheClustersResult().withCacheClusters(
						new CacheCluster().withCacheClusterId("memcached").
								withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(Integer.parseInt(System.getProperty("memcachedPort")))).
								withCacheClusterStatus("available").withEngine("memcached")
				)
		);

		//Act
		CacheManager cacheManager = beanFactory.getBean(CacheManager.class);
		Cache cache = cacheManager.getCache("memcached");
		cache.put("foo", "bar");
		cache.evict("foo");

		//Assert
		assertNotNull(cacheManager);
		assertNotNull(cache);
	}

	@Test
	public void parseInternal_clusterCacheConfigurationWithLogicalName_returnsConfiguredClusterCacheWithPhysicalName() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Register a mock object which will be used to replay service calls
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonElastiCache.class);
		beanFactory.registerBeanDefinition(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonElastiCacheClient.class.getName()), beanDefinitionBuilder.getBeanDefinition());

		BeanDefinitionBuilder resourceIdBuilder = BeanDefinitionBuilder.rootBeanDefinition(Mockito.class);
		resourceIdBuilder.setFactoryMethod("mock");
		resourceIdBuilder.addConstructorArgValue(ResourceIdResolver.class);
		beanFactory.registerBeanDefinition(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME, resourceIdBuilder.getBeanDefinition());

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-elastiCacheConfigStackConfigured.xml", getClass()));


		AmazonElastiCache client = beanFactory.getBean(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonElastiCacheClient.class.getName()), AmazonElastiCache.class);

		ResourceIdResolver resourceIdResolver = beanFactory.getBean(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME, ResourceIdResolver.class);

		when(resourceIdResolver.resolveToPhysicalResourceId("testMemcached")).thenReturn("memcached");

		//Replay invocation that will be called
		when(client.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).thenReturn(
				new DescribeCacheClustersResult().withCacheClusters(
						new CacheCluster().withCacheClusterId("memcached").
								withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(Integer.parseInt(System.getProperty("memcachedPort")))).
								withCacheClusterStatus("available").withEngine("memcached")
				)
		);

		//Act
		CacheManager cacheManager = beanFactory.getBean(CacheManager.class);
		Cache cache = cacheManager.getCache("testMemcached");
		cache.put("foo", "bar");
		cache.evict("foo");

		//Assert
		assertNotNull(cacheManager);
		assertNotNull(cache);
	}

	@Test
	public void parseInternal_clusterCacheConfigurationWithRegion_returnsConfiguredClusterCacheWithRegion() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-elastiCacheConfigRegionConfigured.xml", getClass()));


		//Act
		BeanDefinition beanDefinition = beanFactory.getBeanDefinition(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonElastiCacheClient.class.getName()));

		//Assert
		assertNotNull(beanDefinition);
		assertNotNull(beanDefinition.getPropertyValues().get("region"));
	}

	@Test
	public void parseInternal_clusterCacheConfigurationWithCustomElastiCacheClient_returnsConfigurationWithCustomClient() throws Exception {
		//Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-elastiCacheConfigWithCustomElastiCacheClient.xml", getClass()));

		AmazonElastiCache amazonElastiCacheMock = beanFactory.getBean("customClient", AmazonElastiCache.class);

		when(amazonElastiCacheMock.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).thenReturn(
				new DescribeCacheClustersResult().withCacheClusters(
						new CacheCluster().withCacheClusterId("memcached").
								withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(Integer.parseInt(System.getProperty("memcachedPort")))).
								withCacheClusterStatus("available").withEngine("memcached")
				)
		);

		//Act
		CacheManager cacheManager = beanFactory.getBean(CacheManager.class);
		Cache cache = cacheManager.getCache("memcached");
		cache.put("foo", "bar");
		cache.evict("foo");

		//Assert
		assertNotNull(cacheManager);
		assertNotNull(cache);
	}

	@BeforeClass
	public static void setupMemcachedServerAndClient() throws Exception {
		// Get next free port for the test server
		int availableTcpPort = TestMemcacheServer.startServer();

		// Set the port as system property to easily fetch it in the Spring config
		System.setProperty("memcachedPort", String.valueOf(availableTcpPort));
	}

	@AfterClass
	public static void tearDownMemcachedServerAndClient() throws Exception {
		TestMemcacheServer.stopServer();
		System.clearProperty("memcachedPort");
	}
}
