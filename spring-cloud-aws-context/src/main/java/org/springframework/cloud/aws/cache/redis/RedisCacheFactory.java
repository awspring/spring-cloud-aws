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

package org.springframework.cloud.aws.cache.redis;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.Cache;
import org.springframework.cloud.aws.cache.AbstractCacheFactory;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.ClassUtils;

/**
 * @author Agim Emruli
 */
public class RedisCacheFactory extends AbstractCacheFactory<RedisConnectionFactory> {

	private static final boolean JEDIS_AVAILABLE = ClassUtils
			.isPresent("redis.clients.jedis.Jedis", ClassUtils.getDefaultClassLoader());

	private static final boolean LETTUCE_AVAILABLE = ClassUtils
			.isPresent("io.lettuce.core.RedisClient", ClassUtils.getDefaultClassLoader());

	@Override
	public boolean isSupportingCacheArchitecture(String architecture) {
		return "redis".equalsIgnoreCase(architecture);
	}

	@Override
	public Cache createCache(String cacheName, String host, int port) throws Exception {
		return RedisCacheManager.builder(getConnectionFactory(host, port)).build()
				.getCache(cacheName);
	}

	@Override
	protected void destroyConnectionClient(RedisConnectionFactory connectionClient)
			throws Exception {
		if (connectionClient instanceof DisposableBean) {
			((DisposableBean) connectionClient).destroy();
		}
	}

	@Override
	protected RedisConnectionFactory createConnectionClient(String hostName, int port) {
		RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
		configuration.setHostName(hostName);
		configuration.setPort(port);
		if (JEDIS_AVAILABLE) {
			return new JedisConnectionFactory(configuration);
		}
		else if (LETTUCE_AVAILABLE) {
			return new LettuceConnectionFactory(configuration);
		}
		else {
			throw new IllegalArgumentException("No Jedis or lettuce client on classpath. "
					+ "Please add one of the implementation to your classpath");
		}
	}

}
