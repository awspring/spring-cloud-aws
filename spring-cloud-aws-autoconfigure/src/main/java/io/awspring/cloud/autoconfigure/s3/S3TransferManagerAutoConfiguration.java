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
package io.awspring.cloud.autoconfigure.s3;

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import io.awspring.cloud.autoconfigure.s3.properties.S3TransferManagerProperties;
import io.awspring.cloud.s3.PropertiesS3ObjectContentTypeResolver;
import io.awspring.cloud.s3.S3ObjectContentTypeResolver;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.TransferManagerS3OutputStreamProvider;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.transfer.s3.S3ClientConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.S3TransferManagerOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.UploadDirectoryOverrideConfiguration;

/**
 * {@link EnableAutoConfiguration} for {@link S3TransferManager}
 *
 * @author Anton Perez
 * @since 3.0
 */
@ConditionalOnClass({ S3TransferManager.class, S3OutputStreamProvider.class })
@EnableConfigurationProperties({ S3Properties.class })
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(S3AutoConfiguration.class)
public class S3TransferManagerAutoConfiguration {

	private final S3Properties properties;
	private final AwsProperties awsProperties;
	private final AwsCredentialsProvider credentialsProvider;
	private final AwsClientBuilderConfigurer awsClientBuilderConfigurer;

	public S3TransferManagerAutoConfiguration(S3Properties properties, AwsProperties awsProperties,
			AwsCredentialsProvider credentialsProvider, AwsClientBuilderConfigurer awsClientBuilderConfigurer) {
		this.properties = properties;
		this.awsProperties = awsProperties;
		this.credentialsProvider = credentialsProvider;
		this.awsClientBuilderConfigurer = awsClientBuilderConfigurer;
	}

	@Bean
	@ConditionalOnMissingBean
	S3TransferManager s3TransferManager() {
		return S3TransferManager.builder().s3ClientConfiguration(s3ClientConfiguration())
				.transferConfiguration(extractUploadDirectoryOverrideConfiguration()).build();
	}

	@Bean
	@ConditionalOnMissingBean
	S3OutputStreamProvider transferManagerS3StreamProvider(S3TransferManager s3TransferManager,
			Optional<S3ObjectContentTypeResolver> contentTypeResolver) {
		return new TransferManagerS3OutputStreamProvider(s3TransferManager,
				contentTypeResolver.orElseGet(PropertiesS3ObjectContentTypeResolver::new));
	}

	private S3ClientConfiguration s3ClientConfiguration() {
		S3ClientConfiguration.Builder builder = configure(S3ClientConfiguration.builder());
		if (this.properties.getTransferManager() != null) {
			S3TransferManagerProperties transferManagerProperties = this.properties.getTransferManager();
			PropertyMapper propertyMapper = PropertyMapper.get();
			propertyMapper.from(transferManagerProperties::getMaxConcurrency).whenNonNull().to(builder::maxConcurrency);
			propertyMapper.from(transferManagerProperties::getTargetThroughputInGbps).whenNonNull()
					.to(builder::targetThroughputInGbps);
			propertyMapper.from(transferManagerProperties::getMinimumPartSizeInBytes).whenNonNull()
					.to(builder::minimumPartSizeInBytes);
		}
		return builder.build();
	}

	private S3ClientConfiguration.Builder configure(S3ClientConfiguration.Builder builder) {
		// this must follow the same logic as in AwsClientBuilderConfigurer
		builder.credentialsProvider(this.credentialsProvider)
				.region(this.awsClientBuilderConfigurer.resolveRegion(this.properties));
		// TODO: how to set client override configuration?
		Optional.ofNullable(this.awsProperties.getEndpoint()).ifPresent(builder::endpointOverride);
		Optional.ofNullable(this.properties.getEndpoint()).ifPresent(builder::endpointOverride);
		return builder;
	}

	private S3TransferManagerOverrideConfiguration extractUploadDirectoryOverrideConfiguration() {
		UploadDirectoryOverrideConfiguration.Builder config = UploadDirectoryOverrideConfiguration.builder();
		if (this.properties.getTransferManager() != null
				&& this.properties.getTransferManager().getUploadDirectory() != null) {
			S3TransferManagerProperties.S3UploadDirectoryProperties s3UploadDirectoryProperties = this.properties
					.getTransferManager().getUploadDirectory();
			PropertyMapper propertyMapper = PropertyMapper.get();
			propertyMapper.from(s3UploadDirectoryProperties::getMaxDepth).whenNonNull().to(config::maxDepth);
			propertyMapper.from(s3UploadDirectoryProperties::getRecursive).whenNonNull().to(config::recursive);
			propertyMapper.from(s3UploadDirectoryProperties::getFollowSymbolicLinks).whenNonNull()
					.to(config::followSymbolicLinks);
		}
		return S3TransferManagerOverrideConfiguration.builder().uploadDirectoryConfiguration(config.build()).build();
	}

}
