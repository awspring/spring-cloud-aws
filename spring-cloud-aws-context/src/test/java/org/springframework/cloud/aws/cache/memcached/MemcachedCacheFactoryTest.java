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

package org.springframework.cloud.aws.cache.memcached;

import org.junit.Test;

import org.springframework.cloud.aws.cache.config.TestMemcacheServer;

import static org.assertj.core.api.Assertions.assertThat;

public class MemcachedCacheFactoryTest {

	@Test
	public void createCache_withLocalMemCachedClient_createSimpleSpringMemcached()
			throws Exception {
		// Arrange
		int memCachedPort = TestMemcacheServer.startServer();
		MemcachedCacheFactory cacheFactory = new MemcachedCacheFactory();

		// Act
		SimpleSpringMemcached cache = cacheFactory.createCache("test", "localhost",
				memCachedPort);

		// Assert
		assertThat(cache).isNotNull();
		assertThat(cache.getNativeCache()).isNotNull();

		cache.put("test", "bar");
		assertThat(cache.get("test", String.class)).isEqualTo("bar");
		cache.clear();

		cacheFactory.destroy();
	}

	@Test
	public void createCache_WithExpiryTime_createSimpleSpringMemcachedWithExpiryTime()
			throws Exception {
		// Arrange
		int memCachedPort = TestMemcacheServer.startServer();
		MemcachedCacheFactory cacheFactory = new MemcachedCacheFactory();
		cacheFactory.setExpiryTime(1);

		// Act
		SimpleSpringMemcached cache = cacheFactory.createCache("test", "localhost",
				memCachedPort);

		// Assert
		assertThat(cache).isNotNull();
		assertThat(cache.getNativeCache()).isNotNull();

		cache.put("testWithTimeOut", "bar");
		Thread.sleep(2000);
		assertThat(cache.get("testWithTimeOut")).isNull();

		cacheFactory.destroy();
	}

}
