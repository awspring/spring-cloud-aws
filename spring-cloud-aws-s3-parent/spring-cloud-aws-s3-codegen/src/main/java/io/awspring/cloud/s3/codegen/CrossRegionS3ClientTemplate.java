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
package io.awspring.cloud.s3.codegen;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseRequest;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class CrossRegionS3ClientTemplate implements S3Client {

	private static final Logger LOGGER = LoggerFactory.getLogger("io.awspring.cloud.s3.CrossRegionS3Client");

	private static final int DEFAULT_BUCKET_CACHE_SIZE = 20;
	public static final String BUCKET_REDIRECT_HEADER = "x-amz-bucket-region";
	private static final int HTTP_CODE_PERMANENT_REDIRECT = 301;

	private final Map<Region, S3Client> clientCache = new ConcurrentHashMap<>(Region.regions().size());

	private final S3Client defaultS3Client;

	private final S3ClientBuilder clientBuilder;

	private final ConcurrentLruMap<String, S3Client> bucketCache;

	public CrossRegionS3ClientTemplate(S3ClientBuilder clientBuilder) {
		this(DEFAULT_BUCKET_CACHE_SIZE, clientBuilder);
	}

	public CrossRegionS3ClientTemplate(int bucketCacheSize, S3ClientBuilder clientBuilder) {
		this.defaultS3Client = clientBuilder.build();
		this.clientBuilder = clientBuilder;
		this.bucketCache = new ConcurrentLruMap<>(bucketCacheSize);
	}

	@Override
	public ListBucketsResponse listBuckets(ListBucketsRequest request) throws AwsServiceException, SdkClientException {
		return defaultS3Client.listBuckets(request);
	}

	@Override
	public WriteGetObjectResponseResponse writeGetObjectResponse(WriteGetObjectResponseRequest request,
			RequestBody requestBody) throws AwsServiceException, SdkClientException {
		return defaultS3Client.writeGetObjectResponse(request, requestBody);
	}

	// visible for testing
	Map<Region, S3Client> getClientCache() {
		return clientCache;
	}

	protected S3Client getClient(Region region) {
		return clientCache.computeIfAbsent(region, r -> {
			LOGGER.debug("Creating new S3 client for region: {}", r);
			return clientBuilder.region(r).build();
		});
	}

	private <Result> Result executeInBucketRegion(String bucket, Function<S3Client, Result> fn) {
		try {
			if (bucketCache.contains(bucket)) {
				return fn.apply(bucketCache.get(bucket));
			}
			else {
				return fn.apply(defaultS3Client);
			}
		}
		catch (S3Exception e) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Exception when requesting S3: {}", e.awsErrorDetails().errorCode(), e);
			}
			else {
				LOGGER.debug("Exception when requesting S3 for bucket: {}: details=[{}, {}], httpcode={}", bucket,
						e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(),
						e.awsErrorDetails().sdkHttpResponse().statusCode());
			}
			// A 301 error code ("PermanentRedirect") means that the bucket is in different region than the
			// defaultS3Client is configured for
			if (e.awsErrorDetails().sdkHttpResponse().statusCode() == HTTP_CODE_PERMANENT_REDIRECT) {
				S3Client newClient = e.awsErrorDetails().sdkHttpResponse().firstMatchingHeader(BUCKET_REDIRECT_HEADER)
						.map(Region::of).map(this::getClient)
						.orElseThrow(() -> RegionDiscoveryException.ofMissingHeader(e));

				bucketCache.put(bucket, newClient);
				return fn.apply(newClient);
			}
			else {
				throw e;
			}
		}
	}

	@Override
	public String serviceName() {
		return S3Client.SERVICE_NAME;
	}

	@Override
	public void close() {
		this.clientCache.values().forEach(SdkAutoCloseable::close);
	}

	public static class RegionDiscoveryException extends RuntimeException {
		private static final String HEADER_ERROR_TEMPLATE = "Not able to find the '%s' header in the S3Exception http details (%s)";

		public RegionDiscoveryException(String message, S3Exception e) {
			super(message, e);
		}

		public static RegionDiscoveryException ofMissingHeader(S3Exception e) {
			return new RegionDiscoveryException(String.format(HEADER_ERROR_TEMPLATE, BUCKET_REDIRECT_HEADER,
					e.awsErrorDetails().sdkHttpResponse().headers()), e);
		}
	}

	/**
	 * Simple LRU (Least Recently Used) map, bounded by a specified cache limit.
	 *
	 * Based on {@link ConcurrentLruCache} with the difference that instead of generating values using generator
	 * function, entries can be added with {@link #put(Object, Object)} method.
	 *
	 * <p>
	 * This implementation is backed by a {@code ConcurrentHashMap} for storing the cached values and a
	 * {@code ConcurrentLinkedDeque} for ordering the keys and choosing the least recently used key when the cache is at
	 * full capacity.
	 *
	 * @author Brian Clozel
	 * @author Juergen Hoeller
	 * @author Maciej Walkowiak
	 * @param <K> the type of the key used for cache retrieval
	 * @param <V> the type of the cached values
	 */
	static class ConcurrentLruMap<K, V> {

		private final int sizeLimit;

		private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();

		private final ConcurrentLinkedDeque<K> queue = new ConcurrentLinkedDeque<>();

		private final ReadWriteLock lock = new ReentrantReadWriteLock();

		private volatile int size;

		/**
		 * Create a new cache instance with the given limit.
		 * @param sizeLimit the maximum number of entries in the cache (0 indicates no caching, always generating a new
		 *     value)
		 */
		ConcurrentLruMap(int sizeLimit) {
			Assert.isTrue(sizeLimit >= 0, "Cache size limit must not be negative");
			this.sizeLimit = sizeLimit;
		}

		/**
		 * Retrieve an entry from the cache.
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
		 * @param key the key to check for
		 * @return {@code true} if the key is present, {@code false} if there was no matching key
		 */
		public boolean contains(K key) {
			return this.cache.containsKey(key);
		}

		/**
		 * Immediately remove the given key and any associated value.
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
		 * @see #sizeLimit()
		 */
		public int size() {
			return this.size;
		}

		/**
		 * Return the maximum number of entries in the cache.
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

}
