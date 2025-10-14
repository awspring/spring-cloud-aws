/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.autoconfigure.s3vectors;

import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3vectors.S3VectorsClient;
import software.amazon.awssdk.services.s3vectors.S3VectorsClientBuilder;

/**
 * @author Matej Nedic
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ S3VectorsClient.class })
@EnableConfigurationProperties({ S3VectorProperties.class, AwsProperties.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.s3.vector.enabled", havingValue = "true", matchIfMissing = true)
public class S3VectorClientAutoConfiguration {

	private final S3VectorProperties properties;

	public S3VectorClientAutoConfiguration(S3VectorProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	S3VectorsClientBuilder s3VectorsClientBuilder(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsConnectionDetails> connectionDetails,
			ObjectProvider<S3VectorClientCustomizer> s3ClientCustomizers,
			ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers) {

		return awsClientBuilderConfigurer.configureSyncClient(S3VectorsClient.builder(), this.properties,
				connectionDetails.getIfAvailable(), s3ClientCustomizers.orderedStream(),
				awsSyncClientCustomizers.orderedStream());
	}

	@Bean
	S3VectorsClient s3VectorsClient(S3VectorsClientBuilder builder) {
		return builder.build();
	}
}
