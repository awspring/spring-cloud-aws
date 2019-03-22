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

package org.springframework.cloud.aws.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @param <T> connection client type
 * @author Agim Emruli
 */
public abstract class AbstractCacheFactory<T> implements CacheFactory, DisposableBean {

	private final Map<String, T> nativeConnectionClients = new HashMap<>();

	private final Map<String, Integer> expiryTimePerCache = new HashMap<>();

	private int expiryTime;

	protected AbstractCacheFactory() {
	}

	@Override
	public void destroy() throws Exception {
		synchronized (this.nativeConnectionClients) {
			for (T connectionClient : this.nativeConnectionClients.values()) {
				destroyConnectionClient(connectionClient);
			}
		}
	}

	protected abstract void destroyConnectionClient(T connectionClient) throws Exception;

	protected final T getConnectionFactory(String hostName, int port) throws Exception {
		synchronized (this.nativeConnectionClients) {
			if (!this.nativeConnectionClients.containsKey(hostName)) {
				T nativeConnectionClient = createConnectionClient(hostName, port);
				if (nativeConnectionClient instanceof InitializingBean) {
					((InitializingBean) nativeConnectionClient).afterPropertiesSet();
				}
				this.nativeConnectionClients.put(hostName, nativeConnectionClient);
			}
			return this.nativeConnectionClients.get(hostName);
		}
	}

	protected abstract T createConnectionClient(String hostName, int port)
			throws IOException;

	protected int getExpiryTime() {
		return this.expiryTime;
	}

	public void setExpiryTime(int expiryTime) {
		this.expiryTime = expiryTime;
	}

	public void setExpiryTimePerCache(Map<String, Integer> expiryTimePerCache) {
		this.expiryTimePerCache.putAll(expiryTimePerCache);
	}

	@SuppressWarnings("UnusedParameters")
	protected int getExpiryTime(String cacheName) {
		if (this.expiryTimePerCache.containsKey(cacheName)
				&& this.expiryTimePerCache.get(cacheName) != null
				&& this.expiryTimePerCache.get(cacheName) != 0) {
			return this.expiryTimePerCache.get(cacheName);
		}
		return getExpiryTime();
	}

}
