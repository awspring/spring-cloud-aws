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

package org.springframework.cloud.aws.cache.redis;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.cache.Cache;
import org.springframework.cloud.aws.cache.AbstractCacheFactory;
import org.springframework.data.redis.cache.PublicRedisCache;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.jredis.JredisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.srp.SrpConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.ClassUtils;
import redis.clients.jedis.JedisShardInfo;

/**
 * @author Agim Emruli
 */
public class RedisCacheFactory extends AbstractCacheFactory<RedisConnectionFactory> {

	private static final boolean JEDIS_AVAILABLE = ClassUtils.isPresent("redis.clients.jedis.Jedis", ClassUtils.getDefaultClassLoader());
	private static final boolean JREDIS_AVAILABLE = ClassUtils.isPresent("org.jredis.JRedis", ClassUtils.getDefaultClassLoader());
	private static final boolean SRP_AVAILABLE = ClassUtils.isPresent("redis.client.RedisClient", ClassUtils.getDefaultClassLoader());
	private static final boolean LETTUCE_AVAILABLE = ClassUtils.isPresent("com.lambdaworks.redis.RedisClient", ClassUtils.getDefaultClassLoader());

	@Override
	public boolean isSupportingCacheArchitecture(String architecture) {
		return "redis".equalsIgnoreCase(architecture);
	}

	@Override
	public Cache createCache(String cacheName, String host, int port) throws Exception {
		return new PublicRedisCache(cacheName, null, getRedisTemplate(getConnectionFactory(host, port)), getExpiryTime(cacheName));
	}

	@Override
	protected void destroyConnectionClient(RedisConnectionFactory connectionClient) throws Exception {
		if (connectionClient instanceof DisposableBean) {
			((DisposableBean) connectionClient).destroy();
		}
	}

	//TODO: Add and test other connection factories
	@Override
	protected RedisConnectionFactory createConnectionClient(String hostName, int port) {
		if (JEDIS_AVAILABLE) {
			return new JedisConnectionFactory(new JedisShardInfo(hostName, port));
		} else if (JREDIS_AVAILABLE) {
			return new JredisConnectionFactory();
		} else if (SRP_AVAILABLE) {
			return new SrpConnectionFactory();
		} else if (LETTUCE_AVAILABLE) {
			return new LettuceConnectionFactory();
		} else {
			throw new IllegalArgumentException("No Jedis, Jredis, SRP or lettuce redis client on classpath. " +
					"Please add one of the implementation to your classpath");
		}
	}

	protected RedisTemplate<?, ?> getRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<?, ?> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}