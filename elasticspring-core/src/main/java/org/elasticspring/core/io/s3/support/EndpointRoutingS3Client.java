/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.io.s3.support;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.S3ResponseMetadata;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketCrossOriginConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLoggingConfiguration;
import com.amazonaws.services.s3.model.BucketNotificationConfiguration;
import com.amazonaws.services.s3.model.BucketPolicy;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketPolicyRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketWebsiteConfigurationRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.DeleteVersionRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetBucketAclRequest;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import com.amazonaws.services.s3.model.GetBucketPolicyRequest;
import com.amazonaws.services.s3.model.GetBucketWebsiteConfigurationRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.Region;
import com.amazonaws.services.s3.model.RestoreObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.SetBucketAclRequest;
import com.amazonaws.services.s3.model.SetBucketLoggingConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketWebsiteConfigurationRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.model.VersionListing;
import org.elasticspring.core.io.s3.S3Region;
import org.elasticspring.core.region.RegionProvider;
import org.springframework.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;

public class EndpointRoutingS3Client implements AmazonS3 {

	private final AmazonS3 defaultClient;
	private final AmazonS3ClientFactory clientFactory;

	public EndpointRoutingS3Client(AmazonS3ClientFactory clientFactory, RegionProvider regionProvider) {
		this.clientFactory = clientFactory;
		this.defaultClient = clientFactory.getClientForRegion(regionProvider.getRegion());
	}

	@Override
	public void abortMultipartUpload(AbortMultipartUploadRequest request) throws AmazonClientException {
		this.defaultClient.abortMultipartUpload(request);
	}

	@Override
	public void changeObjectStorageClass(String bucketName, String key, StorageClass newStorageClass) throws AmazonClientException {
		this.defaultClient.changeObjectStorageClass(bucketName, key, newStorageClass);
	}

	@Override
	public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest request) throws AmazonClientException {
		return getClientForBucketName(request.getBucketName()).completeMultipartUpload(request);
	}

	@Override
	public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest) throws AmazonClientException {
		return this.defaultClient.copyObject(copyObjectRequest);
	}

	@Override
	public CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws AmazonClientException {
		return this.defaultClient.copyObject(sourceBucketName, sourceKey, destinationBucketName, destinationKey);
	}

	@Override
	public CopyPartResult copyPart(CopyPartRequest copyPartRequest) throws AmazonClientException {
		return this.defaultClient.copyPart(copyPartRequest);
	}

	@Override
	public Bucket createBucket(String bucketName) throws AmazonClientException {
		return this.defaultClient.createBucket(bucketName);
	}

	@Override
	public Bucket createBucket(String bucketName, Region region) throws AmazonClientException {
		return this.defaultClient.createBucket(bucketName, region);
	}

	@Override
	public Bucket createBucket(String bucketName, String region) throws AmazonClientException {
		return this.defaultClient.createBucket(bucketName, region);
	}

	@Override
	public Bucket createBucket(CreateBucketRequest createBucketRequest) throws AmazonClientException {
		return this.defaultClient.createBucket(createBucketRequest);
	}

	@Override
	public void deleteBucket(String bucketName) throws AmazonClientException {
		this.defaultClient.deleteBucket(bucketName);
	}

	@Override
	public void deleteBucket(DeleteBucketRequest deleteBucketRequest) throws AmazonClientException {
		this.defaultClient.deleteBucket(deleteBucketRequest);
	}

	@Override
	public void deleteBucketCrossOriginConfiguration(String bucketName) {
		this.defaultClient.deleteBucketCrossOriginConfiguration(bucketName);
	}

	@Override
	public void deleteBucketLifecycleConfiguration(String bucketName) {
		this.defaultClient.deleteBucketLifecycleConfiguration(bucketName);
	}

	@Override
	public void deleteBucketPolicy(String bucketName) throws AmazonClientException {
		this.defaultClient.deleteBucketPolicy(bucketName);
	}

	@Override
	public void deleteBucketPolicy(DeleteBucketPolicyRequest deleteBucketPolicyRequest) throws AmazonClientException {
		this.defaultClient.deleteBucketPolicy(deleteBucketPolicyRequest);
	}

	@Override
	public void deleteBucketTaggingConfiguration(String bucketName) {
		this.defaultClient.deleteBucketTaggingConfiguration(bucketName);
	}

	@Override
	public void deleteBucketWebsiteConfiguration(String bucketName) throws AmazonClientException {
		this.defaultClient.deleteBucketWebsiteConfiguration(bucketName);
	}

	@Override
	public void deleteBucketWebsiteConfiguration(DeleteBucketWebsiteConfigurationRequest deleteBucketWebsiteConfigurationRequest) throws AmazonClientException {
		this.defaultClient.deleteBucketWebsiteConfiguration(deleteBucketWebsiteConfigurationRequest);
	}

	@Override
	public void deleteObject(String bucketName, String key) throws AmazonClientException {
		this.defaultClient.deleteObject(bucketName, key);
	}

	@Override
	public void deleteObject(DeleteObjectRequest deleteObjectRequest) throws AmazonClientException {
		this.defaultClient.deleteObject(deleteObjectRequest);
	}

	@Override
	public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest) throws AmazonClientException {
		return this.defaultClient.deleteObjects(deleteObjectsRequest);
	}

	@Override
	public void deleteVersion(String bucketName, String key, String versionId) throws AmazonClientException {
		this.defaultClient.deleteVersion(bucketName, key, versionId);
	}

	@Override
	public void deleteVersion(DeleteVersionRequest deleteVersionRequest) throws AmazonClientException {
		this.defaultClient.deleteVersion(deleteVersionRequest);
	}

	@Override
	public boolean doesBucketExist(String bucketName) throws AmazonClientException {
		return this.defaultClient.doesBucketExist(bucketName);
	}

	@Override
	public URL generatePresignedUrl(String bucketName, String key, Date expiration) throws AmazonClientException {
		return this.defaultClient.generatePresignedUrl(bucketName, key, expiration);
	}

	@Override
	public URL generatePresignedUrl(String bucketName, String key, Date expiration, HttpMethod method) throws AmazonClientException {
		return this.defaultClient.generatePresignedUrl(bucketName, key, expiration, method);
	}

	@Override
	public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest) throws AmazonClientException {
		return this.defaultClient.generatePresignedUrl(generatePresignedUrlRequest);
	}

	@Override
	public AccessControlList getBucketAcl(String bucketName) throws AmazonClientException {
		return this.defaultClient.getBucketAcl(bucketName);
	}

	@Override
	public AccessControlList getBucketAcl(GetBucketAclRequest getBucketAclRequest) throws AmazonClientException {
		return this.defaultClient.getBucketAcl(getBucketAclRequest);
	}

	@Override
	public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(String bucketName) {
		return this.defaultClient.getBucketCrossOriginConfiguration(bucketName);
	}

	@Override
	public BucketLifecycleConfiguration getBucketLifecycleConfiguration(String bucketName) {
		return this.defaultClient.getBucketLifecycleConfiguration(bucketName);
	}

	@Override
	public String getBucketLocation(String bucketName) throws AmazonClientException {
		return this.defaultClient.getBucketLocation(bucketName);
	}

	@Override
	public String getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) throws AmazonClientException {
		return this.defaultClient.getBucketLocation(getBucketLocationRequest);
	}

	@Override
	public BucketLoggingConfiguration getBucketLoggingConfiguration(String bucketName) throws AmazonClientException {
		return this.defaultClient.getBucketLoggingConfiguration(bucketName);
	}

	@Override
	public BucketNotificationConfiguration getBucketNotificationConfiguration(String bucketName) throws AmazonClientException {
		return this.defaultClient.getBucketNotificationConfiguration(bucketName);
	}

	@Override
	public BucketPolicy getBucketPolicy(String bucketName) throws AmazonClientException {
		return this.defaultClient.getBucketPolicy(bucketName);
	}

	@Override
	public BucketPolicy getBucketPolicy(GetBucketPolicyRequest getBucketPolicyRequest) throws AmazonClientException {
		return this.defaultClient.getBucketPolicy(getBucketPolicyRequest);
	}

	@Override
	public BucketTaggingConfiguration getBucketTaggingConfiguration(String bucketName) {
		return this.defaultClient.getBucketTaggingConfiguration(bucketName);
	}

	@Override
	public BucketVersioningConfiguration getBucketVersioningConfiguration(String bucketName) throws AmazonClientException {
		return this.defaultClient.getBucketVersioningConfiguration(bucketName);
	}

	@Override
	public BucketWebsiteConfiguration getBucketWebsiteConfiguration(String bucketName) throws AmazonClientException {
		return this.defaultClient.getBucketWebsiteConfiguration(bucketName);
	}

	@Override
	public BucketWebsiteConfiguration getBucketWebsiteConfiguration(GetBucketWebsiteConfigurationRequest getBucketWebsiteConfigurationRequest) throws AmazonClientException {
		return this.defaultClient.getBucketWebsiteConfiguration(getBucketWebsiteConfigurationRequest);
	}

	@Override
	public S3ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
		return this.defaultClient.getCachedResponseMetadata(request);
	}

	@Override
	public S3Object getObject(String bucketName, String key) throws AmazonClientException {
		return getClientForBucketName(bucketName).getObject(bucketName, key);
	}

	@Override
	public S3Object getObject(GetObjectRequest getObjectRequest) throws AmazonClientException {
		Assert.notNull(getObjectRequest);
		return getClientForBucketName(getObjectRequest.getBucketName()).getObject(getObjectRequest);
	}

	@Override
	public ObjectMetadata getObject(GetObjectRequest getObjectRequest, File destinationFile) throws AmazonClientException {
		Assert.notNull(getObjectRequest);
		return getClientForBucketName(getObjectRequest.getBucketName()).getObject(getObjectRequest, destinationFile);
	}

	private AmazonS3 getClientForBucketName(String bucketName) {
		Assert.notNull(bucketName);
		if (bucketName.contains(".")) {
			String bucketRegion = this.defaultClient.getBucketLocation(bucketName);
			return this.clientFactory.getClientForRegion(S3Region.fromLocation(bucketRegion));
		} else {
			return this.defaultClient;
		}
	}

	@Override
	public AccessControlList getObjectAcl(String bucketName, String key) throws AmazonClientException {
		return getClientForBucketName(bucketName).getObjectAcl(bucketName, key);
	}

	@Override
	public AccessControlList getObjectAcl(String bucketName, String key, String versionId) throws AmazonClientException {
		return getClientForBucketName(bucketName).getObjectAcl(bucketName, key, versionId);
	}

	@Override
	public ObjectMetadata getObjectMetadata(String bucketName, String key) throws AmazonClientException {
		return getClientForBucketName(bucketName).getObjectMetadata(bucketName, key);
	}

	@Override
	public ObjectMetadata getObjectMetadata(GetObjectMetadataRequest getObjectMetadataRequest) throws AmazonClientException {
		return getClientForBucketName(getObjectMetadataRequest.getBucketName()).getObjectMetadata(getObjectMetadataRequest);
	}

	@Override
	public Owner getS3AccountOwner() throws AmazonClientException {
		return this.defaultClient.getS3AccountOwner();
	}

	@Override
	public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws AmazonClientException {
		return getClientForBucketName(request.getBucketName()).initiateMultipartUpload(request);
	}

	@Override
	public List<Bucket> listBuckets() throws AmazonClientException {
		return this.defaultClient.listBuckets();
	}

	@Override
	public List<Bucket> listBuckets(ListBucketsRequest listBucketsRequest) throws AmazonClientException {
		return this.defaultClient.listBuckets(listBucketsRequest);
	}

	@Override
	public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest request) throws AmazonClientException {
		return this.defaultClient.listMultipartUploads(request);
	}

	@Override
	public ObjectListing listNextBatchOfObjects(ObjectListing previousObjectListing) throws AmazonClientException {
		return this.defaultClient.listNextBatchOfObjects(previousObjectListing);
	}

	@Override
	public VersionListing listNextBatchOfVersions(VersionListing previousVersionListing) throws AmazonClientException {
		return this.defaultClient.listNextBatchOfVersions(previousVersionListing);
	}

	@Override
	public ObjectListing listObjects(String bucketName) throws AmazonClientException {
		return this.defaultClient.listObjects(bucketName);
	}

	@Override
	public ObjectListing listObjects(String bucketName, String prefix) throws AmazonClientException {
		return this.defaultClient.listObjects(bucketName, prefix);
	}

	@Override
	public ObjectListing listObjects(ListObjectsRequest listObjectsRequest) throws AmazonClientException {
		return this.defaultClient.listObjects(listObjectsRequest);
	}

	@Override
	public PartListing listParts(ListPartsRequest request) throws AmazonClientException {
		return this.defaultClient.listParts(request);
	}

	@Override
	public VersionListing listVersions(String bucketName, String prefix) throws AmazonClientException {
		return this.defaultClient.listVersions(bucketName, prefix);
	}

	@Override
	public VersionListing listVersions(String bucketName, String prefix, String keyMarker, String versionIdMarker, String delimiter, Integer maxResults) throws AmazonClientException {
		return this.defaultClient.listVersions(bucketName, prefix, keyMarker, versionIdMarker, delimiter, maxResults);
	}

	@Override
	public VersionListing listVersions(ListVersionsRequest listVersionsRequest) throws AmazonClientException {
		return this.defaultClient.listVersions(listVersionsRequest);
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, File file) throws AmazonClientException {
		return getClientForBucketName(bucketName).putObject(bucketName, key, file);
	}

	@Override
	public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata) throws AmazonClientException {
		return getClientForBucketName(bucketName).putObject(bucketName, key, input, metadata);
	}

	@Override
	public PutObjectResult putObject(PutObjectRequest putObjectRequest) throws AmazonClientException {
		return getClientForBucketName(putObjectRequest.getBucketName()).putObject(putObjectRequest);
	}

	@Override
	public void restoreObject(String bucketName, String key, int expirationInDays) throws AmazonServiceException {
		getClientForBucketName(bucketName).restoreObject(bucketName, key, expirationInDays);
	}

	@Override
	public void restoreObject(RestoreObjectRequest copyGlacierObjectRequest) throws AmazonServiceException {
		this.defaultClient.restoreObject(copyGlacierObjectRequest);
	}

	@Override
	public void setBucketAcl(String bucketName, AccessControlList acl) throws AmazonClientException {
		this.defaultClient.setBucketAcl(bucketName, acl);
	}

	@Override
	public void setBucketAcl(String bucketName, CannedAccessControlList acl) throws AmazonClientException {
		this.defaultClient.setBucketAcl(bucketName, acl);
	}

	@Override
	public void setBucketAcl(SetBucketAclRequest setBucketAclRequest) throws AmazonClientException {
		this.defaultClient.setBucketAcl(setBucketAclRequest);
	}

	@Override
	public void setBucketCrossOriginConfiguration(String bucketName, BucketCrossOriginConfiguration bucketCrossOriginConfiguration) {
		this.defaultClient.setBucketCrossOriginConfiguration(bucketName, bucketCrossOriginConfiguration);
	}

	@Override
	public void setBucketLifecycleConfiguration(String bucketName, BucketLifecycleConfiguration bucketLifecycleConfiguration) {
		this.defaultClient.setBucketLifecycleConfiguration(bucketName, bucketLifecycleConfiguration);
	}

	@Override
	public void setBucketLoggingConfiguration(SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest) throws AmazonClientException {
		this.defaultClient.setBucketLoggingConfiguration(setBucketLoggingConfigurationRequest);
	}

	@Override
	public void setBucketNotificationConfiguration(String bucketName, BucketNotificationConfiguration bucketNotificationConfiguration) throws AmazonClientException {
		this.defaultClient.setBucketNotificationConfiguration(bucketName, bucketNotificationConfiguration);
	}

	@Override
	public void setBucketPolicy(String bucketName, String policyText) throws AmazonClientException {
		this.defaultClient.setBucketPolicy(bucketName, policyText);
	}

	@Override
	public void setBucketPolicy(SetBucketPolicyRequest setBucketPolicyRequest) throws AmazonClientException {
		this.defaultClient.setBucketPolicy(setBucketPolicyRequest);
	}

	@Override
	public void setBucketTaggingConfiguration(String bucketName, BucketTaggingConfiguration bucketTaggingConfiguration) {
		this.defaultClient.setBucketTaggingConfiguration(bucketName, bucketTaggingConfiguration);
	}

	@Override
	public void setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest) throws AmazonClientException {
		this.defaultClient.setBucketVersioningConfiguration(setBucketVersioningConfigurationRequest);
	}

	@Override
	public void setBucketWebsiteConfiguration(String bucketName, BucketWebsiteConfiguration configuration) throws AmazonClientException {
		this.defaultClient.setBucketWebsiteConfiguration(bucketName, configuration);
	}

	@Override
	public void setBucketWebsiteConfiguration(SetBucketWebsiteConfigurationRequest setBucketWebsiteConfigurationRequest) throws AmazonClientException {
		this.defaultClient.setBucketWebsiteConfiguration(setBucketWebsiteConfigurationRequest);
	}

	@Override
	public void setEndpoint(String endpoint) {
		this.defaultClient.setEndpoint(endpoint);
	}

	@Override
	public void setObjectAcl(String bucketName, String key, AccessControlList acl) throws AmazonClientException {
		this.defaultClient.setObjectAcl(bucketName, key, acl);
	}

	@Override
	public void setObjectAcl(String bucketName, String key, CannedAccessControlList acl) throws AmazonClientException {
		this.defaultClient.setObjectAcl(bucketName, key, acl);
	}

	@Override
	public void setObjectAcl(String bucketName, String key, String versionId, AccessControlList acl) throws AmazonClientException {
		this.defaultClient.setObjectAcl(bucketName, key, versionId, acl);
	}

	@Override
	public void setObjectAcl(String bucketName, String key, String versionId, CannedAccessControlList acl) throws AmazonClientException {
		this.defaultClient.setObjectAcl(bucketName, key, versionId, acl);
	}

	@Override
	public void setObjectRedirectLocation(String bucketName, String key, String newRedirectLocation) throws AmazonClientException {
		this.defaultClient.setObjectRedirectLocation(bucketName, key, newRedirectLocation);
	}

	@Override
	public UploadPartResult uploadPart(UploadPartRequest request) throws AmazonClientException {
		return getClientForBucketName(request.getBucketName()).uploadPart(request);
	}
}
