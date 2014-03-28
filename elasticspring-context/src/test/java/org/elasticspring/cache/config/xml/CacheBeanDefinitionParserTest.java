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

package org.elasticspring.cache.config.xml;

import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersResult;
import com.amazonaws.services.elasticache.model.Endpoint;
import com.thimbleware.jmemcached.CacheImpl;
import com.thimbleware.jmemcached.Key;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;
import com.thimbleware.jmemcached.storage.CacheStorage;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;
import org.elasticspring.context.config.xml.GlobalBeanDefinitionUtils;
import org.elasticspring.context.config.xml.support.AmazonWebserviceClientConfigurationUtils;
import org.elasticspring.core.env.ResourceIdResolver;
import org.junit.AfterClass;
import org.junit.Assert;
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
import org.springframework.util.SocketUtils;

import java.net.InetSocketAddress;

/**
 * @author Agim Emruli
 */
public class CacheBeanDefinitionParserTest {

	@SuppressWarnings("StaticNonFinalField")
	private static MemCacheDaemon<LocalCacheElement> daemon;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@BeforeClass
	public static void setupMemcachedServerAndClient() throws Exception {
		System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.SLF4JLogger");

		// Get next free port for the test server
		int availableTcpPort = SocketUtils.findAvailableTcpPort();

		// Set the port as system property to easily fetch it in the Spring config
		System.setProperty("memcachedPort", String.valueOf(availableTcpPort));

		daemon = new MemCacheDaemon<LocalCacheElement>();
		CacheStorage<Key, LocalCacheElement> storage = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.FIFO, 1024 * 1024, 1024 * 1024 * 1024);
		daemon.setCache(new CacheImpl(storage));
		daemon.setAddr(new InetSocketAddress(availableTcpPort));
		daemon.setVerbose(true);
		daemon.start();
	}

	@AfterClass
	public static void tearDownMemcachedServerAndClient() throws Exception {
		daemon.stop();

		System.clearProperty("net.spy.log.LoggerImpl");
		System.clearProperty("memcachedPort");
	}

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
		Assert.assertNotNull(cacheManager);
		Assert.assertNotNull(cache);
	}

	@Test
	public void parseInternal_manualCacheConfigWithMissingAllowClear_reportsError() throws Exception {
		//Arrange
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-manualConfigurationMissingAllowClear.xml", getClass());

		//Act
		CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
		Cache cache = cacheManager.getCache("memc");
		cache.put("foo", "bar");
		cache.evict("foo");

		//Assert
		Assert.assertNotNull(cacheManager);
		Assert.assertNotNull(cache);
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
		Assert.assertNotNull(cacheManager);
		Assert.assertNotNull(cache);
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
		Assert.assertNotNull(cacheManager);
		Assert.assertNotNull(memcached);

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
		Mockito.when(client.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).thenReturn(
				new DescribeCacheClustersResult().withCacheClusters(
						new CacheCluster().withCacheClusterId("memcached").
								withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(Integer.parseInt(System.getProperty("memcachedPort")))).
								withCacheClusterStatus("available")
				)
		);

		//Act
		CacheManager cacheManager = beanFactory.getBean(CacheManager.class);
		Cache cache = cacheManager.getCache("memcached");
		cache.put("foo", "bar");
		cache.evict("foo");

		//Assert
		Assert.assertNotNull(cacheManager);
		Assert.assertNotNull(cache);
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

		Mockito.when(resourceIdResolver.resolveToPhysicalResourceId("testMemcached")).thenReturn("memcached");

		//Replay invocation that will be called
		Mockito.when(client.describeCacheClusters(new DescribeCacheClustersRequest().withCacheClusterId("memcached"))).thenReturn(
				new DescribeCacheClustersResult().withCacheClusters(
						new CacheCluster().withCacheClusterId("memcached").
								withConfigurationEndpoint(new Endpoint().withAddress("localhost").withPort(Integer.parseInt(System.getProperty("memcachedPort")))).
								withCacheClusterStatus("available")
				)
		);

		//Act
		CacheManager cacheManager = beanFactory.getBean(CacheManager.class);
		Cache cache = cacheManager.getCache("testMemcached");
		cache.put("foo", "bar");
		cache.evict("foo");

		//Assert
		Assert.assertNotNull(cacheManager);
		Assert.assertNotNull(cache);
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
		Assert.assertNotNull(beanDefinition);
		Assert.assertNotNull(beanDefinition.getPropertyValues().get("region"));
	}
}
