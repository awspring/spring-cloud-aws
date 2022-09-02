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
package io.awspring.cloud.autoconfigure.s3.properties;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * Properties related to AWS S3.
 *
 * @author Maciej Walkowiak
 */
@ConfigurationProperties(prefix = S3Properties.PREFIX)
public class S3Properties extends AwsClientProperties {

	/**
	 * The prefix used for S3 related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.s3";

	/**
	 * Option to enable using the accelerate endpoint when accessing S3. Accelerate endpoints allow faster transfer of
	 * objects by using Amazon CloudFront's globally distributed edge locations.
	 */
	@Nullable
	private Boolean accelerateModeEnabled;

	/**
	 * Option to disable doing a validation of the checksum of an object stored in S3.
	 */
	@Nullable
	private Boolean checksumValidationEnabled;

	/**
	 * Option to enable using chunked encoding when signing the request payload for
	 * {@link software.amazon.awssdk.services.s3.model.PutObjectRequest} and
	 * {@link software.amazon.awssdk.services.s3.model.UploadPartRequest}.
	 */
	@Nullable
	private Boolean chunkedEncodingEnabled;

	/**
	 * Option to enable using path style access for accessing S3 objects instead of DNS style access. DNS style access
	 * is preferred as it will result in better load balancing when accessing S3.
	 */
	@Nullable
	private Boolean pathStyleAccessEnabled;

	/**
	 * If an S3 resource ARN is passed in as the target of an S3 operation that has a different region to the one the
	 * client was configured with, this flag must be set to 'true' to permit the client to make a cross-region call to
	 * the region specified in the ARN otherwise an exception will be thrown.
	 */
	@Nullable
	private Boolean useArnRegionEnabled;

	/**
	 * Configuration properties for {@link S3TransferManager} integration.
	 */
	@Nullable
	@NestedConfigurationProperty
	private S3TransferManagerProperties transferManager;

	@Nullable
	public Boolean getAccelerateModeEnabled() {
		return this.accelerateModeEnabled;
	}

	public void setAccelerateModeEnabled(@Nullable Boolean accelerateModeEnabled) {
		this.accelerateModeEnabled = accelerateModeEnabled;
	}

	@Nullable
	public Boolean getChecksumValidationEnabled() {
		return this.checksumValidationEnabled;
	}

	public void setChecksumValidationEnabled(@Nullable Boolean checksumValidationEnabled) {
		this.checksumValidationEnabled = checksumValidationEnabled;
	}

	@Nullable
	public Boolean getChunkedEncodingEnabled() {
		return this.chunkedEncodingEnabled;
	}

	public void setChunkedEncodingEnabled(@Nullable Boolean chunkedEncodingEnabled) {
		this.chunkedEncodingEnabled = chunkedEncodingEnabled;
	}

	@Nullable
	public Boolean getPathStyleAccessEnabled() {
		return this.pathStyleAccessEnabled;
	}

	public void setPathStyleAccessEnabled(@Nullable Boolean pathStyleAccessEnabled) {
		this.pathStyleAccessEnabled = pathStyleAccessEnabled;
	}

	@Nullable
	public Boolean getUseArnRegionEnabled() {
		return this.useArnRegionEnabled;
	}

	public void setUseArnRegionEnabled(@Nullable Boolean useArnRegionEnabled) {
		this.useArnRegionEnabled = useArnRegionEnabled;
	}

	@Nullable
	public S3TransferManagerProperties getTransferManager() {
		return this.transferManager;
	}

	public void setTransferManager(@Nullable S3TransferManagerProperties transferManager) {
		this.transferManager = transferManager;
	}
}
