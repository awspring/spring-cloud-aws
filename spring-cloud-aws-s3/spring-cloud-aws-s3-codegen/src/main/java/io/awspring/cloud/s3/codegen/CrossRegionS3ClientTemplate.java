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
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.StringUtils;

public class CrossRegionS3ClientTemplate implements S3Client {

	private static final Logger LOGGER = LoggerFactory.getLogger("io.awspring.cloud.s3.CrossRegionS3Client");

	private static final int DEFAULT_BUCKET_CACHE_SIZE = 20;

	private final Map<Region, S3Client> clientCache = new ConcurrentHashMap<>(Region.regions().size());

	private final S3Client defaultS3Client;

	private final ConcurrentLruCache<String, S3Client> bucketCache;

	public CrossRegionS3ClientTemplate(S3ClientBuilder clientBuilder) {
		this(DEFAULT_BUCKET_CACHE_SIZE, clientBuilder);
	}

	public CrossRegionS3ClientTemplate(int bucketCacheSize, S3ClientBuilder clientBuilder) {
		this.defaultS3Client = clientBuilder.build();
		this.bucketCache = new ConcurrentLruCache<>(bucketCacheSize, bucket -> {
			Region region = resolveBucketRegion(bucket);
			return clientCache.computeIfAbsent(region, r -> {
				LOGGER.debug("Creating new S3 client for region: {}", r);
				return clientBuilder.region(r).build();
			});
		});
	}

	@Override
	public ListBucketsResponse listBuckets(ListBucketsRequest request) throws AwsServiceException, SdkClientException {
		return defaultS3Client.listBuckets(request);
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
				LOGGER.debug("Exception when requesting S3 for bucket: {}: [{}] {}", bucket,
						e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
			}
			// "PermanentRedirect" means that the bucket is in different region than the
			// defaultS3Client is configured for
			if ("PermanentRedirect".equals(e.awsErrorDetails().errorCode())) {
				return fn.apply(bucketCache.get(bucket));
			}
			else {
				throw e;
			}
		}
	}

	private Region resolveBucketRegion(String bucket) {
		LOGGER.debug("Resolving region for bucket {}", bucket);
		String bucketLocation = defaultS3Client.getBucketLocation(request -> request.bucket(bucket))
				.locationConstraintAsString();
		Region region = StringUtils.hasLength(bucketLocation) ? Region.of(bucketLocation) : Region.US_EAST_1;
		LOGGER.debug("Region for bucket {} is {}", bucket, region);
		return region;
	}

	@Override
	public String serviceName() {
		return S3Client.SERVICE_NAME;
	}

	@Override
	public void close() {
		this.clientCache.values().forEach(SdkAutoCloseable::close);
	}

}
