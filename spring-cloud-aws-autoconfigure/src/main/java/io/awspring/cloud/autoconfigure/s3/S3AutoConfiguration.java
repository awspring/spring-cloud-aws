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

import java.util.Optional;

import io.awspring.cloud.core.SpringCloudClientConfiguration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@ConditionalOnClass(S3Client.class)
@EnableConfigurationProperties(S3Properties.class)
@Configuration(proxyBeanMethods = false)
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
			.overrideConfiguration(SpringCloudClientConfiguration.clientOverrideConfiguration());
		Optional.ofNullable(this.properties.getEndpoint()).ifPresent(builder::endpointOverride);
		return builder;
	}

	@Bean
	@ConditionalOnMissingBean
	S3Client s3Client(S3ClientBuilder s3ClientBuilder) {
		return s3ClientBuilder.build();
	}

	@Bean
	@Primary
	S3Client crossRegionS3Client(S3ClientBuilder s3ClientBuilder) {
		return new CrossRegionS3Client(s3ClientBuilder);
	}

}
