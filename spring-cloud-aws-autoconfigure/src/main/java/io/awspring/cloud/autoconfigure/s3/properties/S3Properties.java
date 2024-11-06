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
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.crt.S3CrtAsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.encryption.s3.S3EncryptionClient;

/**
 * Properties related to AWS S3.
 *
 * @author Maciej Walkowiak
 * @author Matej Nedic
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
	 * Enables cross-region bucket access.
	 */
	@Nullable
	private Boolean crossRegionEnabled;

	/**
	 * Configuration properties for {@link S3TransferManager} integration.
	 */
	@Nullable
	@NestedConfigurationProperty
	private S3TransferManagerProperties transferManager;

	/**
	 * Configuration properties for {@link S3CrtAsyncClient} integration.
	 */
	@Nullable
	@NestedConfigurationProperty
	private S3CrtClientProperties crt;

	@NestedConfigurationProperty
	private S3PluginProperties plugin = new S3PluginProperties();

	/**
	 * Configuration properties for {@link S3EncryptionClient} integration
	 */
	@NestedConfigurationProperty
	private S3EncryptionProperties encryption = new S3EncryptionProperties();

	public S3EncryptionProperties getEncryption() {
		return encryption;
	}

	public void setEncryption(S3EncryptionProperties encryption) {
		this.encryption = encryption;
	}

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
	public Boolean getCrossRegionEnabled() {
		return crossRegionEnabled;
	}

	public void setCrossRegionEnabled(@Nullable Boolean crossRegionEnabled) {
		this.crossRegionEnabled = crossRegionEnabled;
	}

	@Nullable
	public S3TransferManagerProperties getTransferManager() {
		return this.transferManager;
	}

	public void setTransferManager(@Nullable S3TransferManagerProperties transferManager) {
		this.transferManager = transferManager;
	}

	@Nullable
	public S3CrtClientProperties getCrt() {
		return crt;
	}

	public void setCrt(@Nullable S3CrtClientProperties crt) {
		this.crt = crt;
	}

	public S3Configuration toS3Configuration() {
		S3Configuration.Builder config = S3Configuration.builder();
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(this::getAccelerateModeEnabled).whenNonNull().to(config::accelerateModeEnabled);
		propertyMapper.from(this::getChecksumValidationEnabled).whenNonNull().to(config::checksumValidationEnabled);
		propertyMapper.from(this::getChunkedEncodingEnabled).whenNonNull().to(config::chunkedEncodingEnabled);
		propertyMapper.from(this::getPathStyleAccessEnabled).whenNonNull().to(config::pathStyleAccessEnabled);
		propertyMapper.from(this::getUseArnRegionEnabled).whenNonNull().to(config::useArnRegionEnabled);
		return config.build();
	}

	public S3PluginProperties getPlugin() {
		return plugin;
	}

	public void setPlugin(S3PluginProperties plugin) {
		this.plugin = plugin;
	}
}
