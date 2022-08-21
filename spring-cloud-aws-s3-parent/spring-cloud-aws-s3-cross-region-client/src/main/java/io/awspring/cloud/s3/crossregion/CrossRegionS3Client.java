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
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class CrossRegionS3Client extends AbstractCrossRegionS3Client {

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

	@Override
	<R> R executeInDefaultRegion(Function<S3Client, R> fn) {
		return fn.apply(defaultS3Client);
	}

	@Override
	<R> R executeInBucketRegion(String bucket, Function<S3Client, R> fn) {
		return this.executeInBucketRegion(bucket, fn, true);
	}

	<R> R executeInBucketRegion(String bucket, Function<S3Client, R> fn, boolean allowRecursiveRegionSearch) {
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

	// Functions which are guaranteed to have the region headers, according to
	// https://github.com/aws/aws-sdk-java/blob/fd409dee8ae23fb8953e0bb4dbde65536a7e0514/aws-java-sdk-s3/src/main/java/com/amazonaws/services/s3/Headers.java#L226

	@Override
	public HeadBucketResponse headBucket(HeadBucketRequest request) throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(request.bucket(), c -> c.headBucket(request), false);
	}

	@Override
	public ListObjectsResponse listObjects(ListObjectsRequest request) throws AwsServiceException, SdkClientException {
		return executeInBucketRegion(request.bucket(), c -> c.listObjects(request), false);
	}
}
