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
package io.awspring.cloud.s3;

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
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import org.springframework.util.ConcurrentLruCache;
import org.springframework.util.StringUtils;

public class CrossRegionS3Client implements S3Client {

    private static final Logger LOGGER = LoggerFactory.getLogger("io.awspring.cloud.s3.CrossRegionS3Client");

    private static final int DEFAULT_BUCKET_CACHE_SIZE = 20;

    private final Map<Region, S3Client> clientCache = new ConcurrentHashMap<>(Region.regions().size());

    private final S3Client defaultS3Client;

    private final ConcurrentLruCache<String, S3Client> bucketCache;

    public CrossRegionS3Client(S3ClientBuilder clientBuilder) {
        this(DEFAULT_BUCKET_CACHE_SIZE, clientBuilder);
    }

    public CrossRegionS3Client(int bucketCacheSize, S3ClientBuilder clientBuilder) {
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

    // visible for testing
    Map<Region, S3Client> getClientCache() {
        return clientCache;
    }

    private <Result> Result executeInBucketRegion(String bucket, Function<S3Client, Result> fn) {
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
            // "PermanentRedirect" means that the bucket is in different region than the
            // defaultS3Client is configured for
            if ("PermanentRedirect".equals(e.awsErrorDetails().errorCode())) {
                return fn.apply(bucketCache.get(bucket));
            } else {
                throw e;
            }
        }
    }

    private Region resolveBucketRegion(String bucket) {
        LOGGER.debug("Resolving region for bucket {}", bucket);
        String bucketLocation = defaultS3Client.getBucketLocation(GetBucketLocationRequest.builder().bucket(bucket).build()).locationConstraintAsString();
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

    @Override
    public software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse abortMultipartUpload(software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.abortMultipartUpload(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse completeMultipartUpload(software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.completeMultipartUpload(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.CopyObjectResponse copyObject(software.amazon.awssdk.services.s3.model.CopyObjectRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.copyObject(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.CreateBucketResponse createBucket(software.amazon.awssdk.services.s3.model.CreateBucketRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.createBucket(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse createMultipartUpload(software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.createMultipartUpload(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketResponse deleteBucket(software.amazon.awssdk.services.s3.model.DeleteBucketRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucket(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationResponse deleteBucketAnalyticsConfiguration(software.amazon.awssdk.services.s3.model.DeleteBucketAnalyticsConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketAnalyticsConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketCorsResponse deleteBucketCors(software.amazon.awssdk.services.s3.model.DeleteBucketCorsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketCors(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketEncryptionResponse deleteBucketEncryption(software.amazon.awssdk.services.s3.model.DeleteBucketEncryptionRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketEncryption(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketIntelligentTieringConfigurationResponse deleteBucketIntelligentTieringConfiguration(software.amazon.awssdk.services.s3.model.DeleteBucketIntelligentTieringConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketIntelligentTieringConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationResponse deleteBucketInventoryConfiguration(software.amazon.awssdk.services.s3.model.DeleteBucketInventoryConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketInventoryConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleResponse deleteBucketLifecycle(software.amazon.awssdk.services.s3.model.DeleteBucketLifecycleRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketLifecycle(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationResponse deleteBucketMetricsConfiguration(software.amazon.awssdk.services.s3.model.DeleteBucketMetricsConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketMetricsConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketOwnershipControlsResponse deleteBucketOwnershipControls(software.amazon.awssdk.services.s3.model.DeleteBucketOwnershipControlsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketOwnershipControls(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketPolicyResponse deleteBucketPolicy(software.amazon.awssdk.services.s3.model.DeleteBucketPolicyRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketPolicy(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketReplicationResponse deleteBucketReplication(software.amazon.awssdk.services.s3.model.DeleteBucketReplicationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketReplication(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketTaggingResponse deleteBucketTagging(software.amazon.awssdk.services.s3.model.DeleteBucketTaggingRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketTagging(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteResponse deleteBucketWebsite(software.amazon.awssdk.services.s3.model.DeleteBucketWebsiteRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteBucketWebsite(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteObjectResponse deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteObject(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteObjectTaggingResponse deleteObjectTagging(software.amazon.awssdk.services.s3.model.DeleteObjectTaggingRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteObjectTagging(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeleteObjectsResponse deleteObjects(software.amazon.awssdk.services.s3.model.DeleteObjectsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deleteObjects(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.DeletePublicAccessBlockResponse deletePublicAccessBlock(software.amazon.awssdk.services.s3.model.DeletePublicAccessBlockRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.deletePublicAccessBlock(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationResponse getBucketAccelerateConfiguration(software.amazon.awssdk.services.s3.model.GetBucketAccelerateConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketAccelerateConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketAclResponse getBucketAcl(software.amazon.awssdk.services.s3.model.GetBucketAclRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketAcl(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationResponse getBucketAnalyticsConfiguration(software.amazon.awssdk.services.s3.model.GetBucketAnalyticsConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketAnalyticsConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketCorsResponse getBucketCors(software.amazon.awssdk.services.s3.model.GetBucketCorsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketCors(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketEncryptionResponse getBucketEncryption(software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketEncryption(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketIntelligentTieringConfigurationResponse getBucketIntelligentTieringConfiguration(software.amazon.awssdk.services.s3.model.GetBucketIntelligentTieringConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketIntelligentTieringConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationResponse getBucketInventoryConfiguration(software.amazon.awssdk.services.s3.model.GetBucketInventoryConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketInventoryConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse getBucketLifecycleConfiguration(software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketLifecycleConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketLocationResponse getBucketLocation(software.amazon.awssdk.services.s3.model.GetBucketLocationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketLocation(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketLoggingResponse getBucketLogging(software.amazon.awssdk.services.s3.model.GetBucketLoggingRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketLogging(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationResponse getBucketMetricsConfiguration(software.amazon.awssdk.services.s3.model.GetBucketMetricsConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketMetricsConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationResponse getBucketNotificationConfiguration(software.amazon.awssdk.services.s3.model.GetBucketNotificationConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketNotificationConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketOwnershipControlsResponse getBucketOwnershipControls(software.amazon.awssdk.services.s3.model.GetBucketOwnershipControlsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketOwnershipControls(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse getBucketPolicy(software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketPolicy(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketPolicyStatusResponse getBucketPolicyStatus(software.amazon.awssdk.services.s3.model.GetBucketPolicyStatusRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketPolicyStatus(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketReplicationResponse getBucketReplication(software.amazon.awssdk.services.s3.model.GetBucketReplicationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketReplication(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentResponse getBucketRequestPayment(software.amazon.awssdk.services.s3.model.GetBucketRequestPaymentRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketRequestPayment(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse getBucketTagging(software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketTagging(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse getBucketVersioning(software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketVersioning(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetBucketWebsiteResponse getBucketWebsite(software.amazon.awssdk.services.s3.model.GetBucketWebsiteRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getBucketWebsite(request));
    }

    @Override
    public software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> getObject(software.amazon.awssdk.services.s3.model.GetObjectRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getObject(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetObjectAclResponse getObjectAcl(software.amazon.awssdk.services.s3.model.GetObjectAclRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getObjectAcl(request));
    }

    @Override
    public software.amazon.awssdk.core.ResponseBytes<software.amazon.awssdk.services.s3.model.GetObjectResponse> getObjectAsBytes(software.amazon.awssdk.services.s3.model.GetObjectRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getObjectAsBytes(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetObjectLegalHoldResponse getObjectLegalHold(software.amazon.awssdk.services.s3.model.GetObjectLegalHoldRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getObjectLegalHold(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationResponse getObjectLockConfiguration(software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getObjectLockConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetObjectRetentionResponse getObjectRetention(software.amazon.awssdk.services.s3.model.GetObjectRetentionRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getObjectRetention(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse getObjectTagging(software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getObjectTagging(request));
    }

    @Override
    public software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectTorrentResponse> getObjectTorrent(software.amazon.awssdk.services.s3.model.GetObjectTorrentRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getObjectTorrent(request));
    }

    @Override
    public software.amazon.awssdk.core.ResponseBytes<software.amazon.awssdk.services.s3.model.GetObjectTorrentResponse> getObjectTorrentAsBytes(software.amazon.awssdk.services.s3.model.GetObjectTorrentRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getObjectTorrentAsBytes(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.GetPublicAccessBlockResponse getPublicAccessBlock(software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.getPublicAccessBlock(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.HeadBucketResponse headBucket(software.amazon.awssdk.services.s3.model.HeadBucketRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.headBucket(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.HeadObjectResponse headObject(software.amazon.awssdk.services.s3.model.HeadObjectRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.headObject(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsResponse listBucketAnalyticsConfigurations(software.amazon.awssdk.services.s3.model.ListBucketAnalyticsConfigurationsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listBucketAnalyticsConfigurations(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.ListBucketIntelligentTieringConfigurationsResponse listBucketIntelligentTieringConfigurations(software.amazon.awssdk.services.s3.model.ListBucketIntelligentTieringConfigurationsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listBucketIntelligentTieringConfigurations(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsResponse listBucketInventoryConfigurations(software.amazon.awssdk.services.s3.model.ListBucketInventoryConfigurationsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listBucketInventoryConfigurations(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsResponse listBucketMetricsConfigurations(software.amazon.awssdk.services.s3.model.ListBucketMetricsConfigurationsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listBucketMetricsConfigurations(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse listMultipartUploads(software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listMultipartUploads(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.paginators.ListMultipartUploadsIterable listMultipartUploadsPaginator(software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listMultipartUploadsPaginator(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse listObjectVersions(software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listObjectVersions(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.paginators.ListObjectVersionsIterable listObjectVersionsPaginator(software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listObjectVersionsPaginator(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.ListObjectsResponse listObjects(software.amazon.awssdk.services.s3.model.ListObjectsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listObjects(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.ListObjectsV2Response listObjectsV2(software.amazon.awssdk.services.s3.model.ListObjectsV2Request request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listObjectsV2(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable listObjectsV2Paginator(software.amazon.awssdk.services.s3.model.ListObjectsV2Request request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listObjectsV2Paginator(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.ListPartsResponse listParts(software.amazon.awssdk.services.s3.model.ListPartsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listParts(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.paginators.ListPartsIterable listPartsPaginator(software.amazon.awssdk.services.s3.model.ListPartsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.listPartsPaginator(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationResponse putBucketAccelerateConfiguration(software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketAccelerateConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketAclResponse putBucketAcl(software.amazon.awssdk.services.s3.model.PutBucketAclRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketAcl(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketAnalyticsConfigurationResponse putBucketAnalyticsConfiguration(software.amazon.awssdk.services.s3.model.PutBucketAnalyticsConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketAnalyticsConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketCorsResponse putBucketCors(software.amazon.awssdk.services.s3.model.PutBucketCorsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketCors(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketEncryptionResponse putBucketEncryption(software.amazon.awssdk.services.s3.model.PutBucketEncryptionRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketEncryption(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketIntelligentTieringConfigurationResponse putBucketIntelligentTieringConfiguration(software.amazon.awssdk.services.s3.model.PutBucketIntelligentTieringConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketIntelligentTieringConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketInventoryConfigurationResponse putBucketInventoryConfiguration(software.amazon.awssdk.services.s3.model.PutBucketInventoryConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketInventoryConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationResponse putBucketLifecycleConfiguration(software.amazon.awssdk.services.s3.model.PutBucketLifecycleConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketLifecycleConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketLoggingResponse putBucketLogging(software.amazon.awssdk.services.s3.model.PutBucketLoggingRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketLogging(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketMetricsConfigurationResponse putBucketMetricsConfiguration(software.amazon.awssdk.services.s3.model.PutBucketMetricsConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketMetricsConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationResponse putBucketNotificationConfiguration(software.amazon.awssdk.services.s3.model.PutBucketNotificationConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketNotificationConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketOwnershipControlsResponse putBucketOwnershipControls(software.amazon.awssdk.services.s3.model.PutBucketOwnershipControlsRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketOwnershipControls(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketPolicyResponse putBucketPolicy(software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketPolicy(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketReplicationResponse putBucketReplication(software.amazon.awssdk.services.s3.model.PutBucketReplicationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketReplication(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketRequestPaymentResponse putBucketRequestPayment(software.amazon.awssdk.services.s3.model.PutBucketRequestPaymentRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketRequestPayment(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketTaggingResponse putBucketTagging(software.amazon.awssdk.services.s3.model.PutBucketTaggingRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketTagging(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketVersioningResponse putBucketVersioning(software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketVersioning(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutBucketWebsiteResponse putBucketWebsite(software.amazon.awssdk.services.s3.model.PutBucketWebsiteRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putBucketWebsite(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutObjectAclResponse putObjectAcl(software.amazon.awssdk.services.s3.model.PutObjectAclRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putObjectAcl(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutObjectLegalHoldResponse putObjectLegalHold(software.amazon.awssdk.services.s3.model.PutObjectLegalHoldRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putObjectLegalHold(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutObjectLockConfigurationResponse putObjectLockConfiguration(software.amazon.awssdk.services.s3.model.PutObjectLockConfigurationRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putObjectLockConfiguration(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutObjectRetentionResponse putObjectRetention(software.amazon.awssdk.services.s3.model.PutObjectRetentionRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putObjectRetention(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse putObjectTagging(software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putObjectTagging(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.PutPublicAccessBlockResponse putPublicAccessBlock(software.amazon.awssdk.services.s3.model.PutPublicAccessBlockRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.putPublicAccessBlock(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.RestoreObjectResponse restoreObject(software.amazon.awssdk.services.s3.model.RestoreObjectRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.restoreObject(request));
    }

    @Override
    public software.amazon.awssdk.services.s3.model.UploadPartCopyResponse uploadPartCopy(software.amazon.awssdk.services.s3.model.UploadPartCopyRequest request) throws AwsServiceException, SdkClientException {
        return executeInBucketRegion(request.bucket(), s3Client -> s3Client.uploadPartCopy(request));
    }
}

