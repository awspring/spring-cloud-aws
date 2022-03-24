package io.awspring.cloud.autoconfigure.s3;

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
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.StringUtils;

public class CrossRegionS3Client implements S3Client {
	private static final Logger LOGGER = LoggerFactory.getLogger(CrossRegionS3Client.class);

	private static final int DEFAULT_BUCKET_CACHE_SIZE = 20;

	private final S3Client defaultS3Client;
	private final Map<Region, S3Client> clientCache = new ConcurrentHashMap<>(Region.regions().size());
	private final ConcurrentLruCache<String, S3Client> bucketCache;

	CrossRegionS3Client(S3Client defaultS3Client, S3ClientBuilder clientBuilder) {
		this(DEFAULT_BUCKET_CACHE_SIZE, defaultS3Client, clientBuilder);
	}

	public CrossRegionS3Client(int bucketCacheSize, S3Client defaultS3Client, S3ClientBuilder clientBuilder) {
		this.defaultS3Client = defaultS3Client;
		this.bucketCache = new ConcurrentLruCache<>(bucketCacheSize, bucket -> {
			Region region = resolveBucketRegion(bucket);
			return clientCache.computeIfAbsent(region, r -> {
				LOGGER.debug("Creating new S3 client for region: {}", r);
				return clientBuilder.region(r).build();
			});
		});
	}

	@Override public AbortMultipartUploadResponse abortMultipartUpload(
		AbortMultipartUploadRequest abortMultipartUploadRequest)
		throws NoSuchUploadException, AwsServiceException, SdkClientException, S3Exception {
		return executeInBucketRegion(abortMultipartUploadRequest.bucket(), s3Client -> s3Client.abortMultipartUpload(abortMultipartUploadRequest));
	}

	@Override public ListObjectsResponse listObjects(ListObjectsRequest listObjectsRequest)
		throws NoSuchBucketException, AwsServiceException, SdkClientException, S3Exception {
		return executeInBucketRegion(listObjectsRequest.bucket(), s3Client -> s3Client.listObjects(listObjectsRequest));
	}

	@Override public ListBucketsResponse listBuckets() throws AwsServiceException, SdkClientException, S3Exception {
		return defaultS3Client.listBuckets();
	}

	<Result> Result executeInBucketRegion(String bucket, Function<S3Client, Result> fn) {
		try {
			if (bucketCache.contains(bucket)) {
				return fn.apply(bucketCache.get(bucket));
			} else {
				return fn.apply(defaultS3Client);
			}
		} catch (S3Exception e) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Exception when requesting S3: {}", e.awsErrorDetails().errorCode(), e);
			} else {
				LOGGER.debug("Exception when requesting S3 for bucket: {}: [{}] {}", bucket, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
			}
			// "PermanentRedirect" means that the bucket is in different region than the defaultS3Client is configured for
			if ("PermanentRedirect".equals(e.awsErrorDetails().errorCode())) {
				return fn.apply(bucketCache.get(bucket));
			} else {
				throw e;
			}
		}
	}

	private Region resolveBucketRegion(String bucket) {
		LOGGER.debug("Resolving region for bucket {}", bucket);
		String bucketLocation = defaultS3Client.getBucketLocation(request -> request.bucket(bucket)).locationConstraintAsString();
		// if bucket location is null, region is US_EAST_1
		Region region = StringUtils.hasLength(bucketLocation) ? Region.of(bucketLocation) : Region.US_EAST_1;
		LOGGER.debug("Region for bucket {} is {}", bucket, region);
		return region;
	}

	@Override public String serviceName() {
		return SERVICE_NAME;
	}

	@Override public void close() {
		this.clientCache.values().forEach(SdkAutoCloseable::close);
	}
}
