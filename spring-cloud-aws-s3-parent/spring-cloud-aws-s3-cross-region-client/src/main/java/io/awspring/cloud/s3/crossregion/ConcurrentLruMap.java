/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.s3.crossregion;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;

/**
 * Simple LRU (Least Recently Used) map, bounded by a specified cache limit.
 * <p>
 * Based on {@link ConcurrentLruCache} with the difference that instead of generating values using generator function,
 * entries can be added with {@link #put(Object, Object)} method.
 *
 * <p>
 * This implementation is backed by a {@code ConcurrentHashMap} for storing the cached values and a
 * {@code ConcurrentLinkedDeque} for ordering the keys and choosing the least recently used key when the cache is at
 * full capacity.
 *
 * @param <K> the type of the key used for cache retrieval
 * @param <V> the type of the cached values
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Maciej Walkowiak
 */
class ConcurrentLruMap<K, V> {

	private final int sizeLimit;

	private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();

	private final ConcurrentLinkedDeque<K> queue = new ConcurrentLinkedDeque<>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private volatile int size;

	/**
	 * Create a new cache instance with the given limit.
	 *
	 * @param sizeLimit the maximum number of entries in the cache (0 indicates no caching, always generating a new
	 *     value)
	 */
	ConcurrentLruMap(int sizeLimit) {
		Assert.isTrue(sizeLimit >= 0, "Cache size limit must not be negative");
		this.sizeLimit = sizeLimit;
	}

	/**
	 * Retrieve an entry from the cache.
	 *
	 * @param key the key to retrieve the entry for
	 * @return the cached or {@code null}
	 */
	@Nullable
	public V get(K key) {
		V cached = this.cache.get(key);
		if (cached != null) {
			this.lock.readLock().lock();
			try {
				if (this.queue.removeLastOccurrence(key)) {
					this.queue.offer(key);
				}
				return cached;
			}
			finally {
				this.lock.readLock().unlock();
			}
		}
		return null;
	}

	/**
	 * Puts an entry to the cache.
	 *
	 * @param key the entry key
	 * @param value the entry value
	 */
	public void put(K key, V value) {
		this.lock.writeLock().lock();

		try {
			if (this.size == this.sizeLimit) {
				K leastUsed = this.queue.poll();
				if (leastUsed != null) {
					this.cache.remove(leastUsed);
				}
			}
			this.queue.offer(key);
			this.cache.put(key, value);
			this.size = this.cache.size();
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Determine whether the given key is present in this cache.
	 *
	 * @param key the key to check for
	 * @return {@code true} if the key is present, {@code false} if there was no matching key
	 */
	public boolean contains(K key) {
		return this.cache.containsKey(key);
	}

	/**
	 * Immediately remove the given key and any associated value.
	 *
	 * @param key the key to evict the entry for
	 * @return {@code true} if the key was present before, {@code false} if there was no matching key
	 */
	public boolean remove(K key) {
		this.lock.writeLock().lock();
		try {
			boolean wasPresent = (this.cache.remove(key) != null);
			this.queue.remove(key);
			this.size = this.cache.size();
			return wasPresent;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Immediately remove all entries from this cache.
	 */
	public void clear() {
		this.lock.writeLock().lock();
		try {
			this.cache.clear();
			this.queue.clear();
			this.size = 0;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Return the current size of the cache.
	 *
	 * @see #sizeLimit()
	 */
	public int size() {
		return this.size;
	}

	/**
	 * Return the maximum number of entries in the cache.
	 *
	 * @see #size()
	 */
	public int sizeLimit() {
		return this.sizeLimit;
	}

	Object[] queue() {
		return queue.toArray();
	}

	ConcurrentHashMap<K, V> cache() {
		return new ConcurrentHashMap<>(cache);
	}
}
