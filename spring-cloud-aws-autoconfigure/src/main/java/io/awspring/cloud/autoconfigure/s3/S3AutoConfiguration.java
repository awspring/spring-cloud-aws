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

import io.awspring.cloud.core.SpringCloudClientConfiguration;
import io.awspring.cloud.s3.DiskBufferingS3OutputStreamProvider;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.S3ProtocolResolver;
import io.awspring.cloud.s3.crossregion.CrossRegionS3Client;
import java.util.Optional;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * {@link EnableAutoConfiguration} for {@link S3Client} and {@link S3ProtocolResolver}.
 *
 * @author Maciej Walkowiak
 */
@ConditionalOnClass({ S3Client.class, S3OutputStreamProvider.class })
@EnableConfigurationProperties(S3Properties.class)
@Configuration(proxyBeanMethods = false)
@Import(S3ProtocolResolver.class)
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
public class S3AutoConfiguration {

	private final S3Properties properties;

	public S3AutoConfiguration(S3Properties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	S3ClientBuilder s3ClientBuilder(AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
		Region region = StringUtils.hasLength(this.properties.getRegion()) ? Region.of(this.properties.getRegion())
				: regionProvider.getRegion();
		S3ClientBuilder builder = S3Client.builder().credentialsProvider(credentialsProvider).region(region)
				.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration())
				.serviceConfiguration(s3ServiceConfiguration());
		Optional.ofNullable(this.properties.getEndpoint()).ifPresent(builder::endpointOverride);
		return builder;
	}

	@Bean
	@ConditionalOnMissingBean
	S3OutputStreamProvider s3OutputStreamProvider(S3Client s3Client) {
		return new DiskBufferingS3OutputStreamProvider(s3Client);
	}

	private S3Configuration s3ServiceConfiguration() {
		S3Configuration.Builder config = S3Configuration.builder();
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(properties::getAccelerateModeEnabled).whenNonNull().to(config::accelerateModeEnabled);
		propertyMapper.from(properties::getChecksumValidationEnabled).whenNonNull()
				.to(config::checksumValidationEnabled);
		propertyMapper.from(properties::getChunkedEncodingEnabled).whenNonNull().to(config::chunkedEncodingEnabled);
		propertyMapper.from(properties::getDualstackEnabled).whenNonNull().to(config::dualstackEnabled);
		propertyMapper.from(properties::getPathStyleAccessEnabled).whenNonNull().to(config::pathStyleAccessEnabled);
		propertyMapper.from(properties::getUseArnRegionEnabled).whenNonNull().to(config::useArnRegionEnabled);
		return config.build();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(CrossRegionS3Client.class)
	static class CrossRegionS3ClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		S3Client s3Client(S3ClientBuilder s3ClientBuilder) {
			return new CrossRegionS3Client(s3ClientBuilder);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("io.awspring.cloud.s3.crossregion.CrossRegionS3Client")
	static class StandardS3ClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		S3Client s3Client(S3ClientBuilder s3ClientBuilder) {
			return s3ClientBuilder.build();
		}

	}

}
