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

package org.elasticspring.context.config.xml;

import com.thimbleware.jmemcached.CacheImpl;
import com.thimbleware.jmemcached.Key;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;
import com.thimbleware.jmemcached.storage.CacheStorage;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;
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
	public void parseInternal_manualCacheConfigWithMissingName_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("Attribute 'name' is required for a manual cache configuration");

		//Act
		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-manualConfigurationMissingName.xml", getClass());

		//Assert
	}

	@Test
	public void parseInternal_manualCacheConfigWithMissingAddress_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("Attribute 'address' is required for a manual cache configuration");

		//Act
		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-manualConfigurationMissingAddress.xml", getClass());

		//Assert
	}

	@Test
	public void parseInternal_manualCacheConfigWithMissingExpiration_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("Attribute 'expiration' is required for a manual cache configuration");

		//Act
		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-manualConfigurationMissingExpiration.xml", getClass());

		//Assert
	}

	@Test
	public void parseInternal_manualCacheConfigWithMissingAllowClear_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("Attribute 'allowClear' is required for a manual cache configuration");

		//Act
		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-manualConfigurationMissingAllowClear.xml", getClass());

		//Assert
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
	public void parseInternal_customAndManualCacheConfig_reportsError() throws Exception {
		//Arrange
		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("You can only configure a custom cache or a new cache but not both");

		//Act
		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customAndManualCache.xml", getClass());

		//Assert
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
}
