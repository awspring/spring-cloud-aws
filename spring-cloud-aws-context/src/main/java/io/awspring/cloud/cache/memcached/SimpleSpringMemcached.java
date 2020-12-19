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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import net.spy.memcached.MemcachedClientIF;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 */
public class SimpleSpringMemcached implements Cache {

	private final MemcachedClientIF memcachedClientIF;

	private final String cacheName;

	private int expiration;

	public SimpleSpringMemcached(MemcachedClientIF memcachedClientIF, String cacheName) {
		Assert.notNull(memcachedClientIF, "memcachedClient is mandatory");
		Assert.notNull(cacheName, "cacheName is mandatory");
		this.memcachedClientIF = memcachedClientIF;
		this.cacheName = cacheName;
	}

	@Override
	public String getName() {
		return this.cacheName;
	}

	@Override
	public Object getNativeCache() {
		return this.memcachedClientIF;
	}

	@Override
	public ValueWrapper get(Object key) {
		Assert.notNull(key, "key parameter is mandatory");
		Assert.isAssignable(String.class, key.getClass());
		Object result = this.memcachedClientIF.get((String) key);
		return result != null ? new SimpleValueWrapper(result) : null;
	}

	@Override
	public <T> T get(Object key, Class<T> type) {
		Assert.notNull(key, "key parameter is mandatory");
		Assert.isAssignable(String.class, key.getClass());
		Object result = this.memcachedClientIF.get((String) key);
		if (result == null) {
			return null;
		}
		Assert.isAssignable(type, result.getClass());
		return type.cast(result);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		ValueWrapper valueWrapper = get(key);
		if (valueWrapper != null) {
			return (T) valueWrapper.get();
		}
		else {
			T newValue;
			try {
				newValue = valueLoader.call();
			}
			catch (Throwable ex) {
				throw new ValueRetrievalException(key, valueLoader, ex);
			}
			put(key, newValue);
			return newValue;
		}
	}

	@Override
	public void put(Object key, Object value) {
		Assert.notNull(key, "key parameter is mandatory");
		Assert.isAssignable(String.class, key.getClass());
		try {
			this.memcachedClientIF.set((String) key, this.expiration, value).get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException e) {
			throw new IllegalArgumentException("Error writing key" + key, e);
		}
	}

	/**
	 * <b>IMPORTANT:</b> This operation is not atomic as the underlying implementation
	 * (memcached) does not provide a way to do it.
	 */
	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		Assert.notNull(key, "key parameter is mandatory");
		Assert.isAssignable(String.class, key.getClass());

		ValueWrapper valueWrapper = get(key);
		if (valueWrapper == null) {
			try {
				this.memcachedClientIF.add((String) key, this.expiration, value).get();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (ExecutionException e) {
				throw new IllegalArgumentException("Error writing key" + key, e);
			}
			return null;
		}
		else {
			return valueWrapper;
		}
	}

	@Override
	public void evict(Object key) {
		Assert.notNull(key, "key parameter is mandatory");
		Assert.isAssignable(String.class, key.getClass());
		try {
			this.memcachedClientIF.delete((String) key).get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException e) {
			throw new IllegalArgumentException("Error evicting items" + key);
		}
	}

	@Override
	public void clear() {
		this.memcachedClientIF.flush();
	}

	public void setExpiration(int expiration) {
		this.expiration = expiration;
	}

}
