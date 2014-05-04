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

package org.elasticspring.cache;

import org.elasticspring.cache.memcached.MemcachedClientIF;
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
		Assert.notNull(memcachedClientIF,"memcachedClient is mandatory");
		Assert.notNull(cacheName,"cacheName is mandatory");
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

	public void setExpiration(int expiration) {
		this.expiration = expiration;
	}

	@Override
	public ValueWrapper get(Object key) {
		Assert.notNull(key,"key parameter is mandatory");
		Assert.isAssignable(String.class, key.getClass());
		Object result = this.memcachedClientIF.get((String) key);
		return result != null ?  new SimpleValueWrapper(result) : null;
	}

	@Override
	public <T> T get(Object key, Class<T> type) {
		Assert.notNull(key,"key parameter is mandatory");
		Assert.isAssignable(String.class, key.getClass());
		Object result = this.memcachedClientIF.get((String) key);
		if (result == null) {
			return null;
		}
		Assert.isAssignable(type, result.getClass());
		return type.cast(result);
	}

	@Override
	public void put(Object key, Object value) {
		Assert.notNull(key,"key parameter is mandatory");
		Assert.isAssignable(String.class, key.getClass());
		this.memcachedClientIF.add((String) key, this.expiration, value);
	}

	@Override
	public void evict(Object key) {
		Assert.notNull(key,"key parameter is mandatory");
		Assert.isAssignable(String.class, key.getClass());
		this.memcachedClientIF.delete((String) key);
	}

	@Override
	public void clear() {
		this.memcachedClientIF.flush();
	}
}
