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
import io.awspring.cloud.autoconfigure.s3.properties.S3UploadDirectoryProperties;
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
@EnableConfigurationProperties({ S3Properties.class, AwsProperties.class })
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(S3AutoConfiguration.class)
public class S3TransferManagerAutoConfiguration {

	private final S3Properties properties;

	public S3TransferManagerAutoConfiguration(S3Properties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	S3TransferManager s3TransferManager(AwsClientBuilderConfigurer awsClientBuilderConfigurer) {
		return S3TransferManager.builder().s3ClientConfiguration(s3ClientConfiguration(awsClientBuilderConfigurer))
				.transferConfiguration(extractUploadDirectoryOverrideConfiguration()).build();
	}

	@Bean
	@ConditionalOnMissingBean
	S3OutputStreamProvider transferManagerS3StreamProvider(S3TransferManager s3TransferManager,
			Optional<S3ObjectContentTypeResolver> contentTypeResolver) {
		return new TransferManagerS3OutputStreamProvider(s3TransferManager,
				contentTypeResolver.orElseGet(PropertiesS3ObjectContentTypeResolver::new));
	}

	private S3ClientConfiguration s3ClientConfiguration(AwsClientBuilderConfigurer awsClientBuilderConfigurer) {
		S3ClientConfiguration.Builder builder = awsClientBuilderConfigurer.configure(S3ClientConfiguration.builder(),
				properties);
		if (properties.getTransferManager() != null) {
			S3TransferManagerProperties transferManagerProperties = properties.getTransferManager();
			PropertyMapper propertyMapper = PropertyMapper.get();
			propertyMapper.from(transferManagerProperties::getMaxConcurrency).whenNonNull().to(builder::maxConcurrency);
			propertyMapper.from(transferManagerProperties::getTargetThroughputInGbps).whenNonNull()
					.to(builder::targetThroughputInGbps);
			propertyMapper.from(transferManagerProperties::getMinimumPartSizeInBytes).whenNonNull()
					.to(builder::minimumPartSizeInBytes);
		}
		return builder.build();
	}

	private S3TransferManagerOverrideConfiguration extractUploadDirectoryOverrideConfiguration() {
		UploadDirectoryOverrideConfiguration.Builder config = UploadDirectoryOverrideConfiguration.builder();
		if (properties.getTransferManager() != null && properties.getTransferManager().getUploadDirectory() != null) {
			S3UploadDirectoryProperties s3UploadDirectoryProperties = properties.getTransferManager()
					.getUploadDirectory();
			PropertyMapper propertyMapper = PropertyMapper.get();
			propertyMapper.from(s3UploadDirectoryProperties::getMaxDepth).whenNonNull().to(config::maxDepth);
			propertyMapper.from(s3UploadDirectoryProperties::getRecursive).whenNonNull().to(config::recursive);
			propertyMapper.from(s3UploadDirectoryProperties::getFollowSymbolicLinks).whenNonNull()
					.to(config::followSymbolicLinks);
		}
		return S3TransferManagerOverrideConfiguration.builder().uploadDirectoryConfiguration(config.build()).build();
	}

}
