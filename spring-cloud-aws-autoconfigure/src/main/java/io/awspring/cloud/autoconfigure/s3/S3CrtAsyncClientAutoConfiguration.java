/*
 * Copyright 2013-2023 the original author or authors.
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
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.s3.properties.S3CrtClientProperties;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
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
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

/**
 * {@link EnableAutoConfiguration} for {@link software.amazon.awssdk.crt.s3.S3Client} based
 * {@link software.amazon.awssdk.services.s3.S3AsyncClient}.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
@AutoConfiguration
@ConditionalOnClass({ S3Client.class, S3AsyncClient.class, AwsCrtHttpClient.class})
@EnableConfigurationProperties({ S3Properties.class })
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(S3TransferManagerAutoConfiguration.class)
public class S3CrtAsyncClientAutoConfiguration {

	private final S3Properties properties;
	private final AwsProperties awsProperties;
	private final AwsClientBuilderConfigurer awsClientBuilderConfigurer;

	public S3CrtAsyncClientAutoConfiguration(S3Properties properties, AwsProperties awsProperties,
			AwsClientBuilderConfigurer awsClientBuilderConfigurer) {
		this.properties = properties;
		this.awsProperties = awsProperties;
		this.awsClientBuilderConfigurer = awsClientBuilderConfigurer;
	}

	@Bean
	@ConditionalOnMissingBean
	S3AsyncClient s3AsyncClient(AwsCredentialsProvider credentialsProvider,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder().credentialsProvider(credentialsProvider).region(
				this.awsClientBuilderConfigurer.resolveRegion(this.properties, connectionDetails.getIfAvailable()));
		Optional.ofNullable(connectionDetails.getIfAvailable()).map(AwsConnectionDetails::getEndpoint)
			.ifPresent(builder::endpointOverride);
		Optional.ofNullable(this.awsProperties.getEndpoint()).ifPresent(builder::endpointOverride);
		Optional.ofNullable(this.properties.getEndpoint()).ifPresent(builder::endpointOverride);
		Optional.ofNullable(this.properties.getCrossRegionEnabled()).ifPresent(builder::crossRegionAccessEnabled);
		Optional.ofNullable(this.properties.getPathStyleAccessEnabled()).ifPresent(builder::forcePathStyle);

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
}
