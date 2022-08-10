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

import java.util.Map;
import java.util.Optional;
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

public class CrossRegionS3Client implements S3Client {

	private static final Logger LOGGER = LoggerFactory.getLogger("io.awspring.cloud.s3.CrossRegionS3Client");

	private static final int DEFAULT_BUCKET_CACHE_SIZE = 20;

	public static final String BUCKET_REDIRECT_HEADER = "x-amz-bucket-region";

	private static final int HTTP_CODE_PERMANENT_REDIRECT = 301;

	private final Map<Region, S3Client> clientCache = new ConcurrentHashMap<>(Region.regions().size());

	private final S3Client defaultS3Client;

	private final S3ClientBuilder clientBuilder;

	private final ConcurrentLruMap<String, S3Client> bucketCache;

	public CrossRegionS3Client(S3ClientBuilder clientBuilder) {
		this(DEFAULT_BUCKET_CACHE_SIZE, clientBuilder);
	}

	public CrossRegionS3Client(int bucketCacheSize, S3ClientBuilder clientBuilder) {
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
		return this.executeInBucketRegion(bucket, fn, true);
	}

	private <Result> Result executeInBucketRegion(String bucket, Function<S3Client, Result> fn,
			boolean allowRecursiveRegionSearch) {
		try {
			// If the bucket isn't cached, safely attempt to run fn using the default client
			if (!bucketCache.contains(bucket)) {
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
			// defaultS3Client is configured for.
			if (e.awsErrorDetails().sdkHttpResponse().statusCode() != HTTP_CODE_PERMANENT_REDIRECT) {
				throw e;
			}
			Optional<Region> discoveredRegion = e.awsErrorDetails().sdkHttpResponse()
					.firstMatchingHeader(BUCKET_REDIRECT_HEADER).map(Region::of);
			if (discoveredRegion.isPresent()) {
				LOGGER.debug("Region for bucket was discovered to be {} and is being cached", discoveredRegion.get());
				bucketCache.put(bucket, this.getClient(discoveredRegion.get()));
			}
			else if (allowRecursiveRegionSearch) {
				LOGGER.debug(
						"The error headers did not contain the bucket region header, attempting to find the bucket via a HeadBucket request");
				this.executeInBucketRegion(bucket, c -> c.headBucket(b -> b.bucket(bucket)), false);
			}
			else {
				throw RegionDiscoveryException.ofMissingHeader(e);
			}
		}
		// If we have reached this point, either
		// * a client for this bucket was already cached
		// * using the default client failed, the error had the region header, and we cached the bucket's new client.
		// Thus, any error thrown by this call shouldn't be an InvalidRegion error.
		return fn.apply(bucketCache.get(bucket));
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

	@Override
	public software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse abortMultipartUpload(
			software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.abortMultipartUpload(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse completeMultipartUpload(
			software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.completeMultipartUpload(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.CopyObjectResponse copyObject(
			software.amazon.awssdk.services.s3.model.CopyObjectRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.copyObject(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.CreateBucketResponse createBucket(
			software.amazon.awssdk.services.s3.model.CreateBucketRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.createBucket(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse createMultipartUpload(
			software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.createMultipartUpload(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketResponse deleteBucket(
			software.amazon.awssdk.services.s3.model.DeleteBucketRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucket(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationResponse deleteBucketAnalyticsConfiguration(
			software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketAnalyticsConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketCorsResponse deleteBucketCors(
			software.amazon.awssdk.services.s3.model.DeleteBucketCorsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketCors(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketEncryptionResponse deleteBucketEncryption(
			software.amazon.awssdk.services.s3.model.DeleteBucketEncryptionRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketEncryption(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketIntelligentTieringConfigurationResponse deleteBucketIntelligentTieringConfiguration(
			software.amazon.awssdk.services.s3.model.DeleteBucketIntelligentTieringConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketIntelligentTieringConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationResponse deleteBucketInventoryConfiguration(
			software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketInventoryConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleResponse deleteBucketLifecycle(
			software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketLifecycle(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationResponse deleteBucketMetricsConfiguration(
			software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketMetricsConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketOwnershipControlsResponse deleteBucketOwnershipControls(
			software.amazon.awssdk.services.s3.model.DeleteBucketOwnershipControlsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketOwnershipControls(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketPolicyResponse deleteBucketPolicy(
			software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketPolicy(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketReplicationResponse deleteBucketReplication(
			software.amazon.awssdk.services.s3.model.DeleteBucketReplicationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketReplication(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketTaggingResponse deleteBucketTagging(
			software.amazon.awssdk.services.s3.model.DeleteBucketTaggingRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketTagging(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteResponse deleteBucketWebsite(
			software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteBucketWebsite(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteObjectResponse deleteObject(
			software.amazon.awssdk.services.s3.model.DeleteObjectRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteObject(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteObjectTaggingResponse deleteObjectTagging(
			software.amazon.awssdk.services.s3.model.DeleteObjectTaggingRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteObjectTagging(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeleteObjectsResponse deleteObjects(
			software.amazon.awssdk.services.s3.model.DeleteObjectsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deleteObjects(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.DeletePublicAccessBlockResponse deletePublicAccessBlock(
			software.amazon.awssdk.services.s3.model.DeletePublicAccessBlockRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.deletePublicAccessBlock(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationResponse getBucketAccelerateConfiguration(
			software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketAccelerateConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketAclResponse getBucketAcl(
			software.amazon.awssdk.services.s3.model.GetBucketAclRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketAcl(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationResponse getBucketAnalyticsConfiguration(
			software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketAnalyticsConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketCorsResponse getBucketCors(
			software.amazon.awssdk.services.s3.model.GetBucketCorsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketCors(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketEncryptionResponse getBucketEncryption(
			software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketEncryption(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketIntelligentTieringConfigurationResponse getBucketIntelligentTieringConfiguration(
			software.amazon.awssdk.services.s3.model.GetBucketIntelligentTieringConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketIntelligentTieringConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationResponse getBucketInventoryConfiguration(
			software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketInventoryConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse getBucketLifecycleConfiguration(
			software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketLifecycleConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketLocationResponse getBucketLocation(
			software.amazon.awssdk.services.s3.model.GetBucketLocationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketLocation(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketLoggingResponse getBucketLogging(
			software.amazon.awssdk.services.s3.model.GetBucketLoggingRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketLogging(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationResponse getBucketMetricsConfiguration(
			software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketMetricsConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationResponse getBucketNotificationConfiguration(
			software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketNotificationConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketOwnershipControlsResponse getBucketOwnershipControls(
			software.amazon.awssdk.services.s3.model.GetBucketOwnershipControlsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketOwnershipControls(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse getBucketPolicy(
			software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketPolicy(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketPolicyStatusResponse getBucketPolicyStatus(
			software.amazon.awssdk.services.s3.model.GetBucketPolicyStatusRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketPolicyStatus(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketReplicationResponse getBucketReplication(
			software.amazon.awssdk.services.s3.model.GetBucketReplicationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketReplication(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentResponse getBucketRequestPayment(
			software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketRequestPayment(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse getBucketTagging(
			software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketTagging(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse getBucketVersioning(
			software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketVersioning(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetBucketWebsiteResponse getBucketWebsite(
			software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getBucketWebsite(p0));
	}

	@Override
	public software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> getObject(
			software.amazon.awssdk.services.s3.model.GetObjectRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObject(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetObjectResponse getObject(
			software.amazon.awssdk.services.s3.model.GetObjectRequest p0, java.nio.file.Path p1)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObject(p0, p1));
	}

	@Override
	public <ReturnT> ReturnT getObject(software.amazon.awssdk.services.s3.model.GetObjectRequest p0,
			software.amazon.awssdk.core.sync.ResponseTransformer<software.amazon.awssdk.services.s3.model.GetObjectResponse, ReturnT> p1)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObject(p0, p1));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetObjectAclResponse getObjectAcl(
			software.amazon.awssdk.services.s3.model.GetObjectAclRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectAcl(p0));
	}

	@Override
	public software.amazon.awssdk.core.ResponseBytes<software.amazon.awssdk.services.s3.model.GetObjectResponse> getObjectAsBytes(
			software.amazon.awssdk.services.s3.model.GetObjectRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectAsBytes(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetObjectAttributesResponse getObjectAttributes(
			software.amazon.awssdk.services.s3.model.GetObjectAttributesRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectAttributes(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetObjectLegalHoldResponse getObjectLegalHold(
			software.amazon.awssdk.services.s3.model.GetObjectLegalHoldRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectLegalHold(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationResponse getObjectLockConfiguration(
			software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectLockConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetObjectRetentionResponse getObjectRetention(
			software.amazon.awssdk.services.s3.model.GetObjectRetentionRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectRetention(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse getObjectTagging(
			software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectTagging(p0));
	}

	@Override
	public software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectTorrentResponse> getObjectTorrent(
			software.amazon.awssdk.services.s3.model.GetObjectTorrentRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectTorrent(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetObjectTorrentResponse getObjectTorrent(
			software.amazon.awssdk.services.s3.model.GetObjectTorrentRequest p0, java.nio.file.Path p1)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectTorrent(p0, p1));
	}

	@Override
	public <ReturnT> ReturnT getObjectTorrent(software.amazon.awssdk.services.s3.model.GetObjectTorrentRequest p0,
			software.amazon.awssdk.core.sync.ResponseTransformer<software.amazon.awssdk.services.s3.model.GetObjectTorrentResponse, ReturnT> p1)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectTorrent(p0, p1));
	}

	@Override
	public software.amazon.awssdk.core.ResponseBytes<software.amazon.awssdk.services.s3.model.GetObjectTorrentResponse> getObjectTorrentAsBytes(
			software.amazon.awssdk.services.s3.model.GetObjectTorrentRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getObjectTorrentAsBytes(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.GetPublicAccessBlockResponse getPublicAccessBlock(
			software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.getPublicAccessBlock(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.HeadBucketResponse headBucket(
			software.amazon.awssdk.services.s3.model.HeadBucketRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.headBucket(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.HeadObjectResponse headObject(
			software.amazon.awssdk.services.s3.model.HeadObjectRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.headObject(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsResponse listBucketAnalyticsConfigurations(
			software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listBucketAnalyticsConfigurations(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.ListBucketIntelligentTieringConfigurationsResponse listBucketIntelligentTieringConfigurations(
			software.amazon.awssdk.services.s3.model.ListBucketIntelligentTieringConfigurationsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listBucketIntelligentTieringConfigurations(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsResponse listBucketInventoryConfigurations(
			software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listBucketInventoryConfigurations(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsResponse listBucketMetricsConfigurations(
			software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listBucketMetricsConfigurations(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse listMultipartUploads(
			software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listMultipartUploads(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.paginators.ListMultipartUploadsIterable listMultipartUploadsPaginator(
			software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listMultipartUploadsPaginator(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse listObjectVersions(
			software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listObjectVersions(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.paginators.ListObjectVersionsIterable listObjectVersionsPaginator(
			software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listObjectVersionsPaginator(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.ListObjectsResponse listObjects(
			software.amazon.awssdk.services.s3.model.ListObjectsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listObjects(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.ListObjectsV2Response listObjectsV2(
			software.amazon.awssdk.services.s3.model.ListObjectsV2Request p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listObjectsV2(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable listObjectsV2Paginator(
			software.amazon.awssdk.services.s3.model.ListObjectsV2Request p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listObjectsV2Paginator(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.ListPartsResponse listParts(
			software.amazon.awssdk.services.s3.model.ListPartsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listParts(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.paginators.ListPartsIterable listPartsPaginator(
			software.amazon.awssdk.services.s3.model.ListPartsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.listPartsPaginator(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationResponse putBucketAccelerateConfiguration(
			software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketAccelerateConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketAclResponse putBucketAcl(
			software.amazon.awssdk.services.s3.model.PutBucketAclRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketAcl(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketAnalyticsConfigurationResponse putBucketAnalyticsConfiguration(
			software.amazon.awssdk.services.s3.model.PutBucketAnalyticsConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketAnalyticsConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketCorsResponse putBucketCors(
			software.amazon.awssdk.services.s3.model.PutBucketCorsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketCors(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketEncryptionResponse putBucketEncryption(
			software.amazon.awssdk.services.s3.model.PutBucketEncryptionRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketEncryption(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketIntelligentTieringConfigurationResponse putBucketIntelligentTieringConfiguration(
			software.amazon.awssdk.services.s3.model.PutBucketIntelligentTieringConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketIntelligentTieringConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketInventoryConfigurationResponse putBucketInventoryConfiguration(
			software.amazon.awssdk.services.s3.model.PutBucketInventoryConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketInventoryConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationResponse putBucketLifecycleConfiguration(
			software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketLifecycleConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketLoggingResponse putBucketLogging(
			software.amazon.awssdk.services.s3.model.PutBucketLoggingRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketLogging(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketMetricsConfigurationResponse putBucketMetricsConfiguration(
			software.amazon.awssdk.services.s3.model.PutBucketMetricsConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketMetricsConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationResponse putBucketNotificationConfiguration(
			software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketNotificationConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketOwnershipControlsResponse putBucketOwnershipControls(
			software.amazon.awssdk.services.s3.model.PutBucketOwnershipControlsRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketOwnershipControls(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketPolicyResponse putBucketPolicy(
			software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketPolicy(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketReplicationResponse putBucketReplication(
			software.amazon.awssdk.services.s3.model.PutBucketReplicationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketReplication(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketRequestPaymentResponse putBucketRequestPayment(
			software.amazon.awssdk.services.s3.model.PutBucketRequestPaymentRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketRequestPayment(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketTaggingResponse putBucketTagging(
			software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketTagging(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketVersioningResponse putBucketVersioning(
			software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketVersioning(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutBucketWebsiteResponse putBucketWebsite(
			software.amazon.awssdk.services.s3.model.PutBucketWebsiteRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putBucketWebsite(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutObjectResponse putObject(
			software.amazon.awssdk.services.s3.model.PutObjectRequest p0, java.nio.file.Path p1)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putObject(p0, p1));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutObjectResponse putObject(
			software.amazon.awssdk.services.s3.model.PutObjectRequest p0,
			software.amazon.awssdk.core.sync.RequestBody p1) throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putObject(p0, p1));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutObjectAclResponse putObjectAcl(
			software.amazon.awssdk.services.s3.model.PutObjectAclRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putObjectAcl(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutObjectLegalHoldResponse putObjectLegalHold(
			software.amazon.awssdk.services.s3.model.PutObjectLegalHoldRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putObjectLegalHold(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutObjectLockConfigurationResponse putObjectLockConfiguration(
			software.amazon.awssdk.services.s3.model.PutObjectLockConfigurationRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putObjectLockConfiguration(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutObjectRetentionResponse putObjectRetention(
			software.amazon.awssdk.services.s3.model.PutObjectRetentionRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putObjectRetention(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse putObjectTagging(
			software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putObjectTagging(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.PutPublicAccessBlockResponse putPublicAccessBlock(
			software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.putPublicAccessBlock(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.RestoreObjectResponse restoreObject(
			software.amazon.awssdk.services.s3.model.RestoreObjectRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.restoreObject(p0));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.UploadPartResponse uploadPart(
			software.amazon.awssdk.services.s3.model.UploadPartRequest p0, java.nio.file.Path p1)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.uploadPart(p0, p1));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.UploadPartResponse uploadPart(
			software.amazon.awssdk.services.s3.model.UploadPartRequest p0,
			software.amazon.awssdk.core.sync.RequestBody p1) throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.uploadPart(p0, p1));
	}

	@Override
	public software.amazon.awssdk.services.s3.model.UploadPartCopyResponse uploadPartCopy(
			software.amazon.awssdk.services.s3.model.UploadPartCopyRequest p0)
			throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(p0.bucket(), s3Client -> s3Client.uploadPartCopy(p0));
	}
}
