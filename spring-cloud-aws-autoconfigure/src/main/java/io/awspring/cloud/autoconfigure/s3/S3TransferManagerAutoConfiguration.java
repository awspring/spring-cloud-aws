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
import io.awspring.cloud.autoconfigure.s3.properties.S3CrtClientProperties;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import io.awspring.cloud.autoconfigure.s3.properties.S3TransferManagerProperties;
import io.awspring.cloud.s3.PropertiesS3ObjectContentTypeResolver;
import io.awspring.cloud.s3.S3ObjectContentTypeResolver;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.TransferManagerS3OutputStreamProvider;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.crt.s3.S3Client;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/**
 * {@link EnableAutoConfiguration} for {@link S3TransferManager}
 *
 * @author Anton Perez
 * @since 3.0
 */
@AutoConfiguration
@ConditionalOnClass({ S3TransferManager.class, S3OutputStreamProvider.class, S3Client.class })
@EnableConfigurationProperties({ S3Properties.class })
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(S3AutoConfiguration.class)
public class S3TransferManagerAutoConfiguration {

	private final S3Properties properties;
	private final AwsProperties awsProperties;
	private final AwsClientBuilderConfigurer awsClientBuilderConfigurer;

	public S3TransferManagerAutoConfiguration(S3Properties properties, AwsProperties awsProperties,
			AwsClientBuilderConfigurer awsClientBuilderConfigurer) {
		this.properties = properties;
		this.awsProperties = awsProperties;
		this.awsClientBuilderConfigurer = awsClientBuilderConfigurer;
	}

	@Bean
	@ConditionalOnMissingBean
	S3AsyncClient s3AsyncClient(AwsCredentialsProvider credentialsProvider) {
		S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder().credentialsProvider(credentialsProvider)
				.region(this.awsClientBuilderConfigurer.resolveRegion(this.properties));
		Optional.ofNullable(this.awsProperties.getEndpoint()).ifPresent(builder::endpointOverride);
		Optional.ofNullable(this.properties.getEndpoint()).ifPresent(builder::endpointOverride);

		if (this.properties.getCrt() != null) {
			S3CrtClientProperties crt = this.properties.getCrt();
			PropertyMapper propertyMapper = PropertyMapper.get();
			propertyMapper.from(crt::getMaxConcurrency).whenNonNull().to(builder::maxConcurrency);
			propertyMapper.from(crt::getTargetThroughputInGbps).whenNonNull().to(builder::targetThroughputInGbps);
			propertyMapper.from(crt::getMinimumPartSizeInBytes).whenNonNull().to(builder::minimumPartSizeInBytes);
			propertyMapper.from(crt::getInitialReadBufferSizeInBytes).whenNonNull()
					.to(builder::initialReadBufferSizeInBytes);
		}

		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	S3TransferManager s3TransferManager(S3AsyncClient s3AsyncClient) {
		S3TransferManager.Builder builder = S3TransferManager.builder();
		if (this.properties.getTransferManager() != null) {
			S3TransferManagerProperties transferManagerProperties = this.properties.getTransferManager();
			PropertyMapper propertyMapper = PropertyMapper.get();
			propertyMapper.from(transferManagerProperties::getMaxDepth).whenNonNull()
					.to(builder::uploadDirectoryMaxDepth);
			propertyMapper.from(transferManagerProperties::getFollowSymbolicLinks).whenNonNull()
					.to(builder::uploadDirectoryFollowSymbolicLinks);
		}
		return builder.s3Client(s3AsyncClient).build();
	}

	@Bean
	@ConditionalOnMissingBean
	S3OutputStreamProvider transferManagerS3StreamProvider(S3TransferManager s3TransferManager,
			Optional<S3ObjectContentTypeResolver> contentTypeResolver) {
		return new TransferManagerS3OutputStreamProvider(s3TransferManager,
				contentTypeResolver.orElseGet(PropertiesS3ObjectContentTypeResolver::new));
	}
}
