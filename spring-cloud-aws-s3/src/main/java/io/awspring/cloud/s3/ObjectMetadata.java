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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.s3.model.*;

/**
 * Container for S3 Object Metadata. For information about each field look at {@link PutObjectRequest} Javadocs.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
public class ObjectMetadata {

	@Nullable
	private final String acl;

	@Nullable
	private final String cacheControl;

	@Nullable
	private final String contentDisposition;

	@Nullable
	private final String contentEncoding;

	@Nullable
	private final String contentLanguage;

	@Nullable
	private final Long contentLength;

	@Nullable
	private final String contentType;

	@Nullable
	private final Instant expires;

	@Nullable
	private final String grantFullControl;

	@Nullable
	private final String grantRead;

	@Nullable
	private final String grantReadACP;

	@Nullable
	private final String grantWriteACP;

	@Nullable
	private final Map<String, String> metadata;

	@Nullable
	private final String serverSideEncryption;

	@Nullable
	private final String storageClass;

	@Nullable
	private final String websiteRedirectLocation;

	@Nullable
	private final String sseCustomerAlgorithm;

	@Nullable
	private final String sseCustomerKey;

	@Nullable
	private final String sseCustomerKeyMD5;

	@Nullable
	private final String ssekmsKeyId;

	@Nullable
	private final String ssekmsEncryptionContext;

	@Nullable
	private final Boolean bucketKeyEnabled;

	@Nullable
	private final String requestPayer;

	@Nullable
	private final String tagging;

	@Nullable
	private final String objectLockMode;

	@Nullable
	private final Instant objectLockRetainUntilDate;

	@Nullable
	private final String objectLockLegalHoldStatus;

	@Nullable
	private final String expectedBucketOwner;

	@Nullable
	private final String checksumAlgorithm;

	public static Builder builder() {
		return new Builder();
	}

	ObjectMetadata(@Nullable String acl, @Nullable String cacheControl, @Nullable String contentDisposition,
			@Nullable String contentEncoding, @Nullable String contentLanguage, @Nullable String contentType,
			@Nullable Long contentLength, @Nullable Instant expires, @Nullable String grantFullControl,
			@Nullable String grantRead, @Nullable String grantReadACP, @Nullable String grantWriteACP,
			@Nullable Map<String, String> metadata, @Nullable String serverSideEncryption,
			@Nullable String storageClass, @Nullable String websiteRedirectLocation,
			@Nullable String sseCustomerAlgorithm, @Nullable String sseCustomerKey, @Nullable String sseCustomerKeyMD5,
			@Nullable String ssekmsKeyId, @Nullable String ssekmsEncryptionContext, @Nullable Boolean bucketKeyEnabled,
			@Nullable String requestPayer, @Nullable String tagging, @Nullable String objectLockMode,
			@Nullable Instant objectLockRetainUntilDate, @Nullable String objectLockLegalHoldStatus,
			@Nullable String expectedBucketOwner, @Nullable String checksumAlgorithm) {
		this.acl = acl;
		this.cacheControl = cacheControl;
		this.contentDisposition = contentDisposition;
		this.contentEncoding = contentEncoding;
		this.contentLanguage = contentLanguage;
		this.contentType = contentType;
		this.contentLength = contentLength;
		this.expires = expires;
		this.grantFullControl = grantFullControl;
		this.grantRead = grantRead;
		this.grantReadACP = grantReadACP;
		this.grantWriteACP = grantWriteACP;
		this.metadata = metadata;
		this.serverSideEncryption = serverSideEncryption;
		this.storageClass = storageClass;
		this.websiteRedirectLocation = websiteRedirectLocation;
		this.sseCustomerAlgorithm = sseCustomerAlgorithm;
		this.sseCustomerKey = sseCustomerKey;
		this.sseCustomerKeyMD5 = sseCustomerKeyMD5;
		this.ssekmsKeyId = ssekmsKeyId;
		this.ssekmsEncryptionContext = ssekmsEncryptionContext;
		this.bucketKeyEnabled = bucketKeyEnabled;
		this.requestPayer = requestPayer;
		this.tagging = tagging;
		this.objectLockMode = objectLockMode;
		this.objectLockRetainUntilDate = objectLockRetainUntilDate;
		this.objectLockLegalHoldStatus = objectLockLegalHoldStatus;
		this.expectedBucketOwner = expectedBucketOwner;
		this.checksumAlgorithm = checksumAlgorithm;
	}

	void apply(PutObjectRequest.Builder builder) {
		if (acl != null) {
			builder.acl(acl);
		}
		if (cacheControl != null) {
			builder.cacheControl(cacheControl);
		}
		if (contentDisposition != null) {
			builder.contentDisposition(contentDisposition);
		}
		if (contentEncoding != null) {
			builder.contentEncoding(contentEncoding);
		}
		if (contentLanguage != null) {
			builder.contentLanguage(contentLanguage);
		}
		if (contentType != null) {
			builder.contentType(contentType);
		}
		if (contentLength != null) {
			builder.contentLength(contentLength);
		}
		if (expires != null) {
			builder.expires(expires);
		}
		if (grantFullControl != null) {
			builder.grantFullControl(grantFullControl);
		}
		if (grantRead != null) {
			builder.grantRead(grantRead);
		}
		if (grantReadACP != null) {
			builder.grantReadACP(grantReadACP);
		}
		if (grantWriteACP != null) {
			builder.grantWriteACP(grantWriteACP);
		}
		if (metadata != null) {
			builder.metadata(metadata);
		}
		if (serverSideEncryption != null) {
			builder.serverSideEncryption(serverSideEncryption);
		}
		if (storageClass != null) {
			builder.storageClass(storageClass);
		}
		if (websiteRedirectLocation != null) {
			builder.websiteRedirectLocation(websiteRedirectLocation);
		}
		if (sseCustomerAlgorithm != null) {
			builder.sseCustomerAlgorithm(sseCustomerAlgorithm);
		}
		if (sseCustomerKey != null) {
			builder.sseCustomerKey(sseCustomerKey);
		}
		if (sseCustomerKeyMD5 != null) {
			builder.sseCustomerKeyMD5(sseCustomerKeyMD5);
		}
		if (ssekmsKeyId != null) {
			builder.ssekmsKeyId(ssekmsKeyId);
		}
		if (ssekmsEncryptionContext != null) {
			builder.ssekmsEncryptionContext(ssekmsEncryptionContext);
		}
		if (bucketKeyEnabled != null) {
			builder.bucketKeyEnabled(bucketKeyEnabled);
		}
		if (requestPayer != null) {
			builder.requestPayer(requestPayer);
		}
		if (tagging != null) {
			builder.tagging(tagging);
		}
		if (objectLockMode != null) {
			builder.objectLockMode(objectLockMode);
		}
		if (objectLockRetainUntilDate != null) {
			builder.objectLockRetainUntilDate(objectLockRetainUntilDate);
		}
		if (objectLockLegalHoldStatus != null) {
			builder.objectLockLegalHoldStatus(objectLockLegalHoldStatus);
		}
		if (expectedBucketOwner != null) {
			builder.expectedBucketOwner(expectedBucketOwner);
		}
		if (checksumAlgorithm != null) {
			builder.checksumAlgorithm(checksumAlgorithm);
		}
	}

	void apply(CreateMultipartUploadRequest.Builder builder) {
		if (acl != null) {
			builder.acl(acl);
		}
		if (cacheControl != null) {
			builder.cacheControl(cacheControl);
		}
		if (contentDisposition != null) {
			builder.contentDisposition(contentDisposition);
		}
		if (contentEncoding != null) {
			builder.contentEncoding(contentEncoding);
		}
		if (contentLanguage != null) {
			builder.contentLanguage(contentLanguage);
		}
		if (contentType != null) {
			builder.contentType(contentType);
		}
		if (expires != null) {
			builder.expires(expires);
		}
		if (grantFullControl != null) {
			builder.grantFullControl(grantFullControl);
		}
		if (grantRead != null) {
			builder.grantRead(grantRead);
		}
		if (grantReadACP != null) {
			builder.grantReadACP(grantReadACP);
		}
		if (grantWriteACP != null) {
			builder.grantWriteACP(grantWriteACP);
		}
		if (metadata != null) {
			builder.metadata(metadata);
		}
		if (serverSideEncryption != null) {
			builder.serverSideEncryption(serverSideEncryption);
		}
		if (storageClass != null) {
			builder.storageClass(storageClass);
		}
		if (websiteRedirectLocation != null) {
			builder.websiteRedirectLocation(websiteRedirectLocation);
		}
		if (sseCustomerAlgorithm != null) {
			builder.sseCustomerAlgorithm(sseCustomerAlgorithm);
		}
		if (sseCustomerKey != null) {
			builder.sseCustomerKey(sseCustomerKey);
		}
		if (sseCustomerKeyMD5 != null) {
			builder.sseCustomerKeyMD5(sseCustomerKeyMD5);
		}
		if (ssekmsKeyId != null) {
			builder.ssekmsKeyId(ssekmsKeyId);
		}
		if (ssekmsEncryptionContext != null) {
			builder.ssekmsEncryptionContext(ssekmsEncryptionContext);
		}
		if (bucketKeyEnabled != null) {
			builder.bucketKeyEnabled(bucketKeyEnabled);
		}
		if (requestPayer != null) {
			builder.requestPayer(requestPayer);
		}
		if (tagging != null) {
			builder.tagging(tagging);
		}
		if (objectLockMode != null) {
			builder.objectLockMode(objectLockMode);
		}
		if (objectLockRetainUntilDate != null) {
			builder.objectLockRetainUntilDate(objectLockRetainUntilDate);
		}
		if (objectLockLegalHoldStatus != null) {
			builder.objectLockLegalHoldStatus(objectLockLegalHoldStatus);
		}
		if (expectedBucketOwner != null) {
			builder.expectedBucketOwner(expectedBucketOwner);
		}
		if (checksumAlgorithm != null) {
			builder.checksumAlgorithm(checksumAlgorithm);
		}
	}

	void apply(UploadPartRequest.Builder builder) {
		if (sseCustomerAlgorithm != null) {
			builder.sseCustomerAlgorithm(sseCustomerAlgorithm);
		}
		if (sseCustomerKey != null) {
			builder.sseCustomerKey(sseCustomerKey);
		}
		if (sseCustomerKeyMD5 != null) {
			builder.sseCustomerKeyMD5(sseCustomerKeyMD5);
		}
		if (requestPayer != null) {
			builder.requestPayer(requestPayer);
		}
		if (expectedBucketOwner != null) {
			builder.expectedBucketOwner(expectedBucketOwner);
		}
		if (checksumAlgorithm != null) {
			builder.checksumAlgorithm(checksumAlgorithm);
		}
	}

	void apply(CompleteMultipartUploadRequest.Builder builder) {
		if (sseCustomerAlgorithm != null) {
			builder.sseCustomerAlgorithm(sseCustomerAlgorithm);
		}
		if (sseCustomerKey != null) {
			builder.sseCustomerKey(sseCustomerKey);
		}
		if (sseCustomerKeyMD5 != null) {
			builder.sseCustomerKeyMD5(sseCustomerKeyMD5);
		}
		if (requestPayer != null) {
			builder.requestPayer(requestPayer);
		}
		if (expectedBucketOwner != null) {
			builder.expectedBucketOwner(expectedBucketOwner);
		}
	}

	@Nullable
	public String getAcl() {
		return acl;
	}

	@Nullable
	public String getCacheControl() {
		return cacheControl;
	}

	@Nullable
	public String getContentDisposition() {
		return contentDisposition;
	}

	@Nullable
	public String getContentEncoding() {
		return contentEncoding;
	}

	@Nullable
	public String getContentLanguage() {
		return contentLanguage;
	}

	@Nullable
	public String getContentType() {
		return contentType;
	}

	@Nullable
	public Long getContentLength() {
		return contentLength;
	}

	@Nullable
	public Instant getExpires() {
		return expires;
	}

	@Nullable
	public String getGrantFullControl() {
		return grantFullControl;
	}

	@Nullable
	public String getGrantRead() {
		return grantRead;
	}

	@Nullable
	public String getGrantReadACP() {
		return grantReadACP;
	}

	@Nullable
	public String getGrantWriteACP() {
		return grantWriteACP;
	}

	@Nullable
	public Map<String, String> getMetadata() {
		return metadata;
	}

	@Nullable
	public String getServerSideEncryption() {
		return serverSideEncryption;
	}

	@Nullable
	public String getStorageClass() {
		return storageClass;
	}

	@Nullable
	public String getWebsiteRedirectLocation() {
		return websiteRedirectLocation;
	}

	@Nullable
	public String getSseCustomerAlgorithm() {
		return sseCustomerAlgorithm;
	}

	@Nullable
	public String getSseCustomerKey() {
		return sseCustomerKey;
	}

	@Nullable
	public String getSseCustomerKeyMD5() {
		return sseCustomerKeyMD5;
	}

	@Nullable
	public String getSsekmsKeyId() {
		return ssekmsKeyId;
	}

	@Nullable
	public String getSsekmsEncryptionContext() {
		return ssekmsEncryptionContext;
	}

	@Nullable
	public Boolean getBucketKeyEnabled() {
		return bucketKeyEnabled;
	}

	@Nullable
	public String getRequestPayer() {
		return requestPayer;
	}

	@Nullable
	public String getTagging() {
		return tagging;
	}

	@Nullable
	public String getObjectLockMode() {
		return objectLockMode;
	}

	@Nullable
	public Instant getObjectLockRetainUntilDate() {
		return objectLockRetainUntilDate;
	}

	@Nullable
	public String getObjectLockLegalHoldStatus() {
		return objectLockLegalHoldStatus;
	}

	@Nullable
	public String getExpectedBucketOwner() {
		return expectedBucketOwner;
	}

	@Nullable
	public String getChecksumAlgorithm() {
		return checksumAlgorithm;
	}

	public static class Builder {

		private final Map<String, String> metadata = new HashMap<>();

		@Nullable
		private String acl;

		@Nullable
		private String cacheControl;

		@Nullable
		private String contentDisposition;

		@Nullable
		private String contentEncoding;

		@Nullable
		private String contentLanguage;

		@Nullable
		private String contentType;

		@Nullable
		private Long contentLength;

		@Nullable
		private Instant expires;

		@Nullable
		private String grantFullControl;

		@Nullable
		private String grantRead;

		@Nullable
		private String grantReadACP;

		@Nullable
		private String grantWriteACP;

		@Nullable
		private String serverSideEncryption;

		@Nullable
		private String storageClass;

		@Nullable
		private String websiteRedirectLocation;

		@Nullable
		private String sseCustomerAlgorithm;

		@Nullable
		private String sseCustomerKey;

		@Nullable
		private String sseCustomerKeyMD5;

		@Nullable
		private String ssekmsKeyId;

		@Nullable
		private String ssekmsEncryptionContext;

		@Nullable
		private Boolean bucketKeyEnabled;

		@Nullable
		private String requestPayer;

		@Nullable
		private String tagging;

		@Nullable
		private String objectLockMode;

		@Nullable
		private Instant objectLockRetainUntilDate;

		@Nullable
		private String objectLockLegalHoldStatus;

		@Nullable
		private String expectedBucketOwner;

		@Nullable
		private String checksumAlgorithm;

		public Builder acl(@Nullable String acl) {
			this.acl = acl;
			return this;
		}

		public Builder acl(@Nullable ObjectCannedACL acl) {
			return this.acl(acl != null ? acl.toString() : null);
		}

		public Builder cacheControl(@Nullable String cacheControl) {
			this.cacheControl = cacheControl;
			return this;
		}

		public Builder contentDisposition(@Nullable String contentDisposition) {
			this.contentDisposition = contentDisposition;
			return this;
		}

		public Builder contentEncoding(@Nullable String contentEncoding) {
			this.contentEncoding = contentEncoding;
			return this;
		}

		public Builder contentLanguage(@Nullable String contentLanguage) {
			this.contentLanguage = contentLanguage;
			return this;
		}

		public Builder contentType(@Nullable String contentType) {
			this.contentType = contentType;
			return this;
		}

		public Builder contentLength(@Nullable Long contentLength) {
			this.contentLength = contentLength;
			return this;
		}

		public Builder expires(Instant expires) {
			this.expires = expires;
			return this;
		}

		public Builder grantFullControl(@Nullable String grantFullControl) {
			this.grantFullControl = grantFullControl;
			return this;
		}

		public Builder grantRead(@Nullable String grantRead) {
			this.grantRead = grantRead;
			return this;
		}

		public Builder grantReadACP(@Nullable String grantReadACP) {
			this.grantReadACP = grantReadACP;
			return this;
		}

		public Builder grantWriteACP(@Nullable String grantWriteACP) {
			this.grantWriteACP = grantWriteACP;
			return this;
		}

		public Builder metadata(@Nullable String key, String value) {
			this.metadata.put(key, value);
			return this;
		}

		public Builder serverSideEncryption(@Nullable String serverSideEncryption) {
			this.serverSideEncryption = serverSideEncryption;
			return this;
		}

		public Builder serverSideEncryption(@Nullable ServerSideEncryption serverSideEncryption) {
			return this.serverSideEncryption(serverSideEncryption != null ? serverSideEncryption.toString() : null);
		}

		public Builder storageClass(@Nullable String storageClass) {
			this.storageClass = storageClass;
			return this;
		}

		public Builder storageClass(@Nullable StorageClass storageClass) {
			return this.storageClass(storageClass != null ? storageClass.toString() : null);
		}

		public Builder websiteRedirectLocation(@Nullable String websiteRedirectLocation) {
			this.websiteRedirectLocation = websiteRedirectLocation;
			return this;
		}

		public Builder sseCustomerAlgorithm(@Nullable String sseCustomerAlgorithm) {
			this.sseCustomerAlgorithm = sseCustomerAlgorithm;
			return this;
		}

		public Builder sseCustomerKey(@Nullable String sseCustomerKey) {
			this.sseCustomerKey = sseCustomerKey;
			return this;
		}

		public Builder sseCustomerKeyMD5(@Nullable String sseCustomerKeyMD5) {
			this.sseCustomerKeyMD5 = sseCustomerKeyMD5;
			return this;
		}

		public Builder ssekmsKeyId(@Nullable String ssekmsKeyId) {
			this.ssekmsKeyId = ssekmsKeyId;
			return this;
		}

		public Builder ssekmsEncryptionContext(@Nullable String ssekmsEncryptionContext) {
			this.ssekmsEncryptionContext = ssekmsEncryptionContext;
			return this;
		}

		public Builder bucketKeyEnabled(Boolean bucketKeyEnabled) {
			this.bucketKeyEnabled = bucketKeyEnabled;
			return this;
		}

		public Builder requestPayer(@Nullable String requestPayer) {
			this.requestPayer = requestPayer;
			return this;
		}

		public Builder requestPayer(@Nullable RequestPayer requestPayer) {
			return this.requestPayer(requestPayer != null ? requestPayer.toString() : null);
		}

		public Builder tagging(@Nullable String tagging) {
			this.tagging = tagging;
			return this;
		}

		public Builder objectLockMode(@Nullable String objectLockMode) {
			this.objectLockMode = objectLockMode;
			return this;
		}

		public Builder objectLockMode(@Nullable ObjectLockMode objectLockMode) {
			return this.objectLockMode(objectLockMode != null ? objectLockMode.toString() : null);
		}

		public Builder objectLockRetainUntilDate(@Nullable Instant objectLockRetainUntilDate) {
			this.objectLockRetainUntilDate = objectLockRetainUntilDate;
			return this;
		}

		public Builder objectLockLegalHoldStatus(@Nullable String objectLockLegalHoldStatus) {
			this.objectLockLegalHoldStatus = objectLockLegalHoldStatus;
			return this;
		}

		public Builder objectLockLegalHoldStatus(@Nullable ObjectLockLegalHoldStatus objectLockLegalHoldStatus) {
			return this.objectLockLegalHoldStatus(
					objectLockLegalHoldStatus != null ? objectLockLegalHoldStatus.toString() : null);
		}

		public Builder expectedBucketOwner(@Nullable String expectedBucketOwner) {
			this.expectedBucketOwner = expectedBucketOwner;
			return this;
		}

		public Builder checksumAlgorithm(@Nullable String checksumAlgorithm) {
			this.checksumAlgorithm = checksumAlgorithm;
			return this;
		}

		public Builder checksumAlgorithm(@Nullable ChecksumAlgorithm checksumAlgorithm) {
			return checksumAlgorithm(checksumAlgorithm != null ? checksumAlgorithm.toString() : null);
		}

		public ObjectMetadata build() {
			return new ObjectMetadata(acl, cacheControl, contentDisposition, contentEncoding, contentLanguage,
					contentType, contentLength, expires, grantFullControl, grantRead, grantReadACP, grantWriteACP,
					metadata, serverSideEncryption, storageClass, websiteRedirectLocation, sseCustomerAlgorithm,
					sseCustomerKey, sseCustomerKeyMD5, ssekmsKeyId, ssekmsEncryptionContext, bucketKeyEnabled,
					requestPayer, tagging, objectLockMode, objectLockRetainUntilDate, objectLockLegalHoldStatus,
					expectedBucketOwner, checksumAlgorithm);
		}

	}

}
