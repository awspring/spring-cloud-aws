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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.MemcachedClient;

import org.springframework.cloud.aws.cache.AbstractCacheFactory;

/**
 * @author Agim Emruli
 */
public class MemcachedCacheFactory extends AbstractCacheFactory<MemcachedClient> {

	@Override
	public boolean isSupportingCacheArchitecture(String architecture) {
		return "memcached".equals(architecture);
	}

	@Override
	public SimpleSpringMemcached createCache(String cacheName, String host, int port)
			throws Exception {
		SimpleSpringMemcached springMemcached = new SimpleSpringMemcached(
				getConnectionFactory(host, port), cacheName);
		springMemcached.setExpiration(getExpiryTime(cacheName));
		return springMemcached;
	}

	@Override
	protected MemcachedClient createConnectionClient(String hostName, int port)
			throws IOException {
		return new MemcachedClient(new InetSocketAddress(hostName, port));
	}

	@Override
	protected void destroyConnectionClient(MemcachedClient connectionClient) {
		connectionClient.shutdown(10, TimeUnit.SECONDS);
	}

}
