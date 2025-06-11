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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.AwsSyncClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsConnectionDetails;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.s3.properties.S3Properties;
import io.awspring.cloud.s3.InMemoryBufferingS3OutputStreamProvider;
import io.awspring.cloud.s3.Jackson2JsonS3ObjectConverter;
import io.awspring.cloud.s3.PropertiesS3ObjectContentTypeResolver;
import io.awspring.cloud.s3.S3ObjectContentTypeResolver;
import io.awspring.cloud.s3.S3ObjectConverter;
import io.awspring.cloud.s3.S3Operations;
import io.awspring.cloud.s3.S3OutputStreamProvider;
import io.awspring.cloud.s3.S3ProtocolResolver;
import io.awspring.cloud.s3.S3Template;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.s3accessgrants.plugin.S3AccessGrantsPlugin;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.encryption.s3.S3EncryptionClient;

/**
 * {@link AutoConfiguration} for {@link S3Client} and {@link S3ProtocolResolver}.
 *
 * @author Maciej Walkowiak
 * @author Matej Nedic
 */
@AutoConfiguration
@ConditionalOnClass({ S3Client.class, S3OutputStreamProvider.class })
@EnableConfigurationProperties({ S3Properties.class, AwsProperties.class })
@Import(S3ProtocolResolver.class)
@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
public class S3AutoConfiguration {

	private final S3Properties properties;

	public S3AutoConfiguration(S3Properties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	S3ClientBuilder s3ClientBuilder(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsConnectionDetails> connectionDetails,
			ObjectProvider<S3ClientCustomizer> s3ClientCustomizers,
			ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers) {
		S3ClientBuilder builder = awsClientBuilderConfigurer.configureSyncClient(S3Client.builder(), this.properties,
				connectionDetails.getIfAvailable(), s3ClientCustomizers.orderedStream(),
				awsSyncClientCustomizers.orderedStream());

		if (ClassUtils.isPresent("software.amazon.awssdk.s3accessgrants.plugin.S3AccessGrantsPlugin", null)) {
			S3AccessGrantsPlugin s3AccessGrantsPlugin = S3AccessGrantsPlugin.builder()
					.enableFallback(properties.getPlugin().getEnableFallback()).build();
			builder.addPlugin(s3AccessGrantsPlugin);
		}

		Optional.ofNullable(this.properties.getCrossRegionEnabled()).ifPresent(builder::crossRegionAccessEnabled);

		builder.serviceConfiguration(this.properties.toS3Configuration());
		return builder;
	}

	@Bean
	@ConditionalOnMissingBean(S3Operations.class)
	@ConditionalOnBean(S3ObjectConverter.class)
	S3Template s3Template(S3Client s3Client, S3OutputStreamProvider s3OutputStreamProvider,
			S3ObjectConverter s3ObjectConverter, S3Presigner s3Presigner) {
		return new S3Template(s3Client, s3OutputStreamProvider, s3ObjectConverter, s3Presigner);
	}

	@Bean
	@ConditionalOnMissingBean
	S3Presigner s3Presigner(S3Properties properties, AwsProperties awsProperties,
			AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider,
			ObjectProvider<AwsConnectionDetails> connectionDetails) {
		S3Presigner.Builder builder = S3Presigner.builder().serviceConfiguration(properties.toS3Configuration())
				.credentialsProvider(credentialsProvider).region(AwsClientBuilderConfigurer.resolveRegion(properties,
						connectionDetails.getIfAvailable(), regionProvider));

		if (properties.getEndpoint() != null) {
			builder.endpointOverride(properties.getEndpoint());
		}
		else if (awsProperties.getEndpoint() != null) {
			builder.endpointOverride(awsProperties.getEndpoint());
		}
		connectionDetails.ifAvailable(it -> {
			if (it.getEndpoint() != null) {
				builder.endpointOverride(it.getEndpoint());
			}
		});
		Optional.ofNullable(awsProperties.getFipsEnabled()).ifPresent(builder::fipsEnabled);
		Optional.ofNullable(awsProperties.getDualstackEnabled()).ifPresent(builder::dualstackEnabled);
		return builder.build();
	}

	@Conditional(S3EncryptionConditional.class)
	@ConditionalOnClass(name = "software.amazon.encryption.s3.S3EncryptionClient")
	@Configuration
	public static class S3EncryptionConfiguration {

		@Bean
		@ConditionalOnMissingBean
		S3Client s3EncryptionClient(S3EncryptionClient.Builder s3EncryptionBuilder, S3ClientBuilder s3ClientBuilder) {
			s3EncryptionBuilder.wrappedClient(s3ClientBuilder.build());
			return s3EncryptionBuilder.build();
		}

		@Bean
		@ConditionalOnMissingBean
		S3EncryptionClient.Builder s3EncrpytionClientBuilder(S3Properties properties,
				AwsClientBuilderConfigurer awsClientBuilderConfigurer,
				ObjectProvider<AwsConnectionDetails> connectionDetails,
				ObjectProvider<S3EncryptionClientCustomizer> s3ClientCustomizers,
				ObjectProvider<AwsSyncClientCustomizer> awsSyncClientCustomizers,
				ObjectProvider<S3RsaProvider> rsaProvider, ObjectProvider<S3AesProvider> aesProvider) {
			S3EncryptionClient.Builder builder = awsClientBuilderConfigurer.configureSyncClient(
					S3EncryptionClient.builder(), properties, connectionDetails.getIfAvailable(),
					s3ClientCustomizers.orderedStream(), awsSyncClientCustomizers.orderedStream());

			Optional.ofNullable(properties.getCrossRegionEnabled()).ifPresent(builder::crossRegionAccessEnabled);
			builder.serviceConfiguration(properties.toS3Configuration());

			configureEncryptionProperties(properties, rsaProvider, aesProvider, builder);
			return builder;
		}

		private static void configureEncryptionProperties(S3Properties properties,
				ObjectProvider<S3RsaProvider> rsaProvider, ObjectProvider<S3AesProvider> aesProvider,
				S3EncryptionClient.Builder builder) {
			PropertyMapper propertyMapper = PropertyMapper.get();
			var encryptionProperties = properties.getEncryption();

			propertyMapper.from(encryptionProperties::isEnableDelayedAuthenticationMode)
					.to(builder::enableDelayedAuthenticationMode);
			propertyMapper.from(encryptionProperties::isEnableLegacyUnauthenticatedModes)
					.to(builder::enableLegacyUnauthenticatedModes);
			propertyMapper.from(encryptionProperties::isEnableMultipartPutObject).to(builder::enableMultipartPutObject);

			if (!StringUtils.hasText(properties.getEncryption().getKeyId())) {
				if (aesProvider.getIfAvailable() != null) {
					builder.aesKey(aesProvider.getObject().generateSecretKey());
				}
				else {
					builder.rsaKeyPair(rsaProvider.getObject().generateKeyPair());
				}
			}
			else {
				propertyMapper.from(encryptionProperties::getKeyId).to(builder::kmsKeyId);
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean
	S3Client s3Client(S3ClientBuilder s3ClientBuilder) {
		return s3ClientBuilder.build();
	}

	@Configuration
	@ConditionalOnClass(ObjectMapper.class)
	static class Jackson2JsonS3ObjectConverterConfiguration {

		@ConditionalOnMissingBean
		@Bean
		S3ObjectConverter s3ObjectConverter(Optional<ObjectMapper> objectMapper) {
			return new Jackson2JsonS3ObjectConverter(objectMapper.orElseGet(ObjectMapper::new));
		}
	}

	@Bean
	@ConditionalOnMissingBean
	S3OutputStreamProvider inMemoryBufferingS3StreamProvider(S3Client s3Client,
			Optional<S3ObjectContentTypeResolver> contentTypeResolver) {
		return new InMemoryBufferingS3OutputStreamProvider(s3Client,
				contentTypeResolver.orElseGet(PropertiesS3ObjectContentTypeResolver::new));
	}
}
