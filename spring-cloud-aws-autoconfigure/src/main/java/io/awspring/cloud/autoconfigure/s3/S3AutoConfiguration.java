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
import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.AwsProperties;
import io.awspring.cloud.autoconfigure.s3.properties.S3EncryptionProperties;
import io.awspring.cloud.autoconfigure.s3.properties.S3EncryptionType;
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
import io.awspring.cloud.s3.crossregion.CrossRegionS3Client;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import javax.crypto.KeyGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.encryption.s3.S3EncryptionClient;

/**
 * {@link EnableAutoConfiguration} for {@link S3Client} and {@link S3ProtocolResolver}.
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
			ObjectProvider<AwsClientCustomizer<S3ClientBuilder>> configurer) {
		S3ClientBuilder builder = awsClientBuilderConfigurer.configure(S3Client.builder(), this.properties,
				configurer.getIfAvailable());

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
			AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
		S3Presigner.Builder builder = S3Presigner.builder().serviceConfiguration(properties.toS3Configuration())
				.credentialsProvider(credentialsProvider)
				.region(AwsClientBuilderConfigurer.resolveRegion(properties, regionProvider));

		if (properties.getEndpoint() != null) {
			builder.endpointOverride(properties.getEndpoint());
		}
		else if (awsProperties.getEndpoint() != null) {
			builder.endpointOverride(awsProperties.getEndpoint());
		}
		Optional.ofNullable(awsProperties.getFipsEnabled()).ifPresent(builder::fipsEnabled);
		Optional.ofNullable(awsProperties.getDualstackEnabled()).ifPresent(builder::dualstackEnabled);
		return builder.build();
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
		S3Client s3Client(S3Properties properties, S3ClientBuilder s3ClientBuilder) throws NoSuchAlgorithmException {
			if (ClassUtils.isPresent("software.amazon.encryption.s3.S3EncryptionClient", null)) {
				return s3EncClient(properties, s3ClientBuilder);
			}
			return s3ClientBuilder.build();
		}

		S3Client s3EncClient(S3Properties properties, S3ClientBuilder s3ClientBuilder) throws NoSuchAlgorithmException {
			PropertyMapper propertyMapper = PropertyMapper.get();
			S3EncryptionProperties encryptionProperties = properties.getEncryption();
			S3EncryptionClient.Builder s3EncryptionBuilder = S3EncryptionClient.builder();

			s3EncryptionBuilder.wrappedClient(s3ClientBuilder.build());
			propertyMapper.from(encryptionProperties::isEnableDelayedAuthenticationMode)
					.to(s3EncryptionBuilder::enableDelayedAuthenticationMode);
			propertyMapper.from(encryptionProperties::isEnableLegacyUnauthenticatedModes)
					.to(s3EncryptionBuilder::enableLegacyUnauthenticatedModes);
			propertyMapper.from(encryptionProperties::isEnableMultipartPutObject)
					.to(s3EncryptionBuilder::enableMultipartPutObject);

			if (!encryptionProperties.getType().getType().equals(S3EncryptionType.KMS_ID.getType())
					&& encryptionProperties.isAutoGenerateKey()) {
				if (encryptionProperties.getType().equals(S3EncryptionType.AES)) {
					KeyGenerator keyGenerator = KeyGenerator.getInstance(encryptionProperties.getType().getType());
					keyGenerator
							.init(encryptionProperties.getKeyLength() > 0 ? encryptionProperties.getKeyLength() : 256);
					s3EncryptionBuilder.aesKey(keyGenerator.generateKey());
				}
				else {
					KeyPairGenerator keyPairGenerator = KeyPairGenerator
							.getInstance(encryptionProperties.getType().getType());
					keyPairGenerator.initialize(
							encryptionProperties.getKeyLength() > 0 ? encryptionProperties.getKeyLength() : 2048);
					s3EncryptionBuilder.rsaKeyPair(keyPairGenerator.generateKeyPair());

				}
				return s3EncryptionBuilder.build();
			}
			propertyMapper.from(encryptionProperties::getKmsId).to(s3EncryptionBuilder::kmsKeyId);
			return s3EncryptionBuilder.build();
		}

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
