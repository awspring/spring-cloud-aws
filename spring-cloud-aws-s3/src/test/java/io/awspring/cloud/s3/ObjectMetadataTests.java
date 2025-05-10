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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

/**
 * Unit tests for {@link ObjectMetadata}.
 *
 * @author Maciej Walkowiak
 */
class ObjectMetadataTests {

	@Test
	void canBeAppliedToPutObjectRequest() {
		Instant now = Instant.now();
		ObjectMetadata objectMetadata = ObjectMetadata.builder().acl("acl").cacheControl("cacheControl")
				.contentDisposition("contentDisposition").contentEncoding("contentEncoding")
				.contentLanguage("contentLanguage").contentType("contentType").contentLength(0L).expires(now)
				.grantFullControl("grantFullControl").grantRead("grantRead").grantReadACP("grantReadACP")
				.grantWriteACP("grantWriteACP").metadata("key1", "value1").metadata("key2", "value2")
				.serverSideEncryption("serverSideEncryption").storageClass("storageClass")
				.websiteRedirectLocation("websiteRedirectLocation").sseCustomerAlgorithm("sseCustomerAlgorithm")
				.sseCustomerKey("sseCustomerKey").sseCustomerKeyMD5("sseCustomerKeyMD5").ssekmsKeyId("ssekmsKeyId")
				.ssekmsEncryptionContext("ssekmsEncryptionContext").bucketKeyEnabled(true).requestPayer("requestPayer")
				.tagging("tagging").objectLockMode("objectLockMode").objectLockRetainUntilDate(now)
				.objectLockLegalHoldStatus("objectLockLegalHoldStatus").expectedBucketOwner("expectedBucketOwner")
				.checksumAlgorithm("checksumAlgorithm").build();

		PutObjectRequest.Builder builder = PutObjectRequest.builder();
		objectMetadata.apply(builder);

		assertThat(builder).usingRecursiveComparison()
				.ignoringFields("awsRequestOverrideConfig", "checksumCRC32C", "checksumSHA1", "checksumSHA256", "key",
						"contentMD5", "bucket", "checksumCRC32", "contentLength", "ifNoneMatch", "ifMatch",
						"writeOffsetBytes")
				.isEqualTo(objectMetadata);
	}

	@Test
	void mapsEnumsToString() {
		ObjectMetadata metadata = ObjectMetadata.builder().acl(ObjectCannedACL.AUTHENTICATED_READ)
				.storageClass(StorageClass.ONEZONE_IA).objectLockLegalHoldStatus(ObjectLockLegalHoldStatus.OFF)
				.requestPayer(RequestPayer.REQUESTER).serverSideEncryption(ServerSideEncryption.AES256)
				.checksumAlgorithm(ChecksumAlgorithm.CRC32).build();

		PutObjectRequest.Builder builder = PutObjectRequest.builder();
		metadata.apply(builder);
		PutObjectRequest result = builder.build();

		assertThat(result.acl()).isEqualTo(ObjectCannedACL.AUTHENTICATED_READ);
		assertThat(result.storageClass()).isEqualTo(StorageClass.ONEZONE_IA);
		assertThat(result.objectLockLegalHoldStatus()).isEqualTo(ObjectLockLegalHoldStatus.OFF);
		assertThat(result.requestPayer()).isEqualTo(RequestPayer.REQUESTER);
		assertThat(result.serverSideEncryption()).isEqualTo(ServerSideEncryption.AES256);
		assertThat(result.checksumAlgorithm()).isEqualTo(ChecksumAlgorithm.CRC32);
	}

	@Test
	void doesNotApplyContentLengthForPartUpload() {
		long objectContentLength = 16L;
		long partContentLength = 8L;
		ObjectMetadata metadata = ObjectMetadata.builder().contentLength(objectContentLength).build();

		UploadPartRequest.Builder builder = UploadPartRequest.builder().contentLength(partContentLength);
		metadata.apply(builder);
		UploadPartRequest result = builder.build();

		assertThat(result.contentLength()).isEqualTo(partContentLength);
	}

}
