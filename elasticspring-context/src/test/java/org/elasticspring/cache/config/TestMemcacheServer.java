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

package org.elasticspring.cache.config;

import com.thimbleware.jmemcached.CacheImpl;
import com.thimbleware.jmemcached.Key;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;
import com.thimbleware.jmemcached.storage.CacheStorage;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;
import org.springframework.util.SocketUtils;

import java.net.InetSocketAddress;

/**
 * @author Agim Emruli
 */
public class TestMemcacheServer {

	@SuppressWarnings("StaticNonFinalField")
	private static MemCacheDaemon<LocalCacheElement> daemon;

	@SuppressWarnings("StaticNonFinalField")
	private static int portForInstance;

	public static int startServer() {
		if (daemon == null) {
			System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.Log4JLogger");

			// Get next free port for the test server
			portForInstance = SocketUtils.findAvailableTcpPort();

			//noinspection NonThreadSafeLazyInitialization
			daemon = new MemCacheDaemon<LocalCacheElement>();
			CacheStorage<Key, LocalCacheElement> storage = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.FIFO, 1024 * 1024, 1024 * 1024 * 1024);
			daemon.setCache(new CacheImpl(storage));
			daemon.setAddr(new InetSocketAddress(portForInstance));
			daemon.setVerbose(true);
			daemon.start();
		}
		return portForInstance;
	}

	public static void stopServer() {
		try {
			daemon.stop();
		} finally {
			System.clearProperty("net.spy.log.LoggerImpl");
		}
	}
}
