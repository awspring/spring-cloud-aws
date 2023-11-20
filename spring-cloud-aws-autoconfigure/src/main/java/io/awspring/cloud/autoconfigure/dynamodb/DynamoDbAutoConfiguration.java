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
package io.awspring.cloud.autoconfigure.dynamodb;

import java.io.IOException;
import java.util.Optional;

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.dynamodb.DefaultDynamoDbTableNameResolver;
import io.awspring.cloud.dynamodb.DefaultDynamoDbTableSchemaResolver;
import io.awspring.cloud.dynamodb.DynamoDbOperations;
import io.awspring.cloud.dynamodb.DynamoDbTableNameResolver;
import io.awspring.cloud.dynamodb.DynamoDbTableSchemaResolver;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.dax.ClusterDaxAsyncClient;
import software.amazon.dax.ClusterDaxClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for DynamoDB integration.
 *
 * @author Matej Nedic
 * @author Arun Patra
 * @since 3.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(DynamoDbProperties.class)
@ConditionalOnClass({ DynamoDbClient.class, DynamoDbAsyncClient.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.dynamodb.enabled", havingValue = "true", matchIfMissing = true)
public class DynamoDbAutoConfiguration {
	@ConditionalOnProperty(name = "spring.cloud.aws.dynamodb.dax.url")
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "software.amazon.dax.ClusterDaxClient")
	static class DaxDynamoDbClient {

		private software.amazon.dax.Configuration.Builder toAwsDaxConfiguration(DaxProperties daxProperties) {
			PropertyMapper propertyMapper = PropertyMapper.get();
			software.amazon.dax.Configuration.Builder configuration = software.amazon.dax.Configuration.builder();
			propertyMapper.from(daxProperties.getIdleTimeoutMillis()).whenNonNull()
				.to(configuration::idleTimeoutMillis);
			propertyMapper.from(daxProperties.getConnectionTtlMillis()).whenNonNull()
				.to(configuration::connectionTtlMillis);
			propertyMapper.from(daxProperties.getConnectTimeoutMillis()).whenNonNull()
				.to(configuration::connectTimeoutMillis);
			propertyMapper.from(daxProperties.getRequestTimeoutMillis()).whenNonNull()
				.to(configuration::requestTimeoutMillis);
			propertyMapper.from(daxProperties.getWriteRetries()).whenNonNull().to(configuration::writeRetries);
			propertyMapper.from(daxProperties.getReadRetries()).whenNonNull().to(configuration::readRetries);
			propertyMapper.from(daxProperties.getClusterUpdateIntervalMillis()).whenNonNull()
				.to(configuration::clusterUpdateIntervalMillis);
			propertyMapper.from(daxProperties.getEndpointRefreshTimeoutMillis()).whenNonNull()
				.to(configuration::endpointRefreshTimeoutMillis);
			propertyMapper.from(daxProperties.getMaxConcurrency()).whenNonNull().to(configuration::maxConcurrency);
			propertyMapper.from(daxProperties.getMaxPendingConnectionAcquires()).whenNonNull()
				.to(configuration::maxPendingConnectionAcquires);
			propertyMapper.from(daxProperties.getSkipHostNameVerification()).whenNonNull()
				.to(configuration::skipHostNameVerification);

			return configuration;
		}

		@ConditionalOnMissingBean
		@Bean
		public DynamoDbClient daxDynamoDbClient(DynamoDbProperties properties,
												AwsCredentialsProvider credentialsProvider,
												AwsRegionProvider regionProvider) throws IOException {
			DaxProperties daxProperties = properties.getDax();

			software.amazon.dax.Configuration.Builder configuration = toAwsDaxConfiguration(daxProperties);

			configuration.region(AwsClientBuilderConfigurer.resolveRegion(properties, regionProvider))
					.credentialsProvider(credentialsProvider).url(properties.getDax().getUrl());
			return ClusterDaxClient.builder().overrideConfiguration(configuration.build()).build();
		}

		@ConditionalOnMissingBean
		@Bean
		public DynamoDbAsyncClient daxDynamoDbAsyncClient(DynamoDbProperties properties,
														  AwsCredentialsProvider credentialsProvider,
								  						  AwsRegionProvider regionProvider) throws IOException {
			DaxProperties daxProperties = properties.getDax();

			software.amazon.dax.Configuration.Builder configuration = toAwsDaxConfiguration(daxProperties);

			configuration.region(AwsClientBuilderConfigurer.resolveRegion(properties, regionProvider))
					.credentialsProvider(credentialsProvider).url(properties.getDax().getUrl());
			return ClusterDaxAsyncClient.builder().overrideConfiguration(configuration.build()).build();
		}
	}

	@Conditional(MissingDaxUrlCondition.class)
	@Configuration(proxyBeanMethods = false)
	static class StandardDynamoDbClient {

		@ConditionalOnMissingBean
		@Bean
		public DynamoDbClient standardDynamoDbClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
				ObjectProvider<AwsClientCustomizer<DynamoDbClientBuilder>> configurer, DynamoDbProperties properties) {
			return awsClientBuilderConfigurer
					.configure(DynamoDbClient.builder(), properties, configurer.getIfAvailable()).build();
		}
	}

	@Conditional(MissingDaxUrlCondition.class)
	@Configuration(proxyBeanMethods = false)
	static class StandardDynamoDbAsyncClient {

		@ConditionalOnMissingBean
		@Bean
		public DynamoDbAsyncClient standardDynamoDbAsyncClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
				ObjectProvider<AwsClientCustomizer<DynamoDbAsyncClientBuilder>> configurer,
				DynamoDbProperties properties) {
			return awsClientBuilderConfigurer
					.configure(DynamoDbAsyncClient.builder(), properties, configurer.getIfAvailable()).build();
		}

	}

	@ConditionalOnClass(name = "software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient")
	@Configuration(proxyBeanMethods = false)
	static class DynamoDbEnhancedClientConfiguration {
		@ConditionalOnMissingBean
		@ConditionalOnClass(DynamoDbEnhancedClient.class)
		@Bean
		public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
			return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
		}

		@ConditionalOnMissingBean
		@ConditionalOnClass(DynamoDbEnhancedAsyncClient.class)
		@Bean
		public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient dynamoDbClient) {
			return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbClient).build();
		}
	}

	@ConditionalOnClass(name = "io.awspring.cloud.dynamodb.DynamoDbOperations")
	@Configuration(proxyBeanMethods = false)
	static class DynamoDbTemplateConfiguration {
		@ConditionalOnMissingBean(DynamoDbOperations.class)
		@Bean
		public DynamoDbTemplate dynamoDBTemplate(DynamoDbProperties properties,
												 DynamoDbEnhancedClient dynamoDbEnhancedClient, Optional<DynamoDbTableSchemaResolver> tableSchemaResolver,
												 Optional<DynamoDbTableNameResolver> tableNameResolver) {
			DynamoDbTableSchemaResolver tableSchemaRes = tableSchemaResolver
				.orElseGet(DefaultDynamoDbTableSchemaResolver::new);

			DynamoDbTableNameResolver tableNameRes = tableNameResolver
				.orElseGet(() -> new DefaultDynamoDbTableNameResolver(properties.getTablePrefix()));
			return new DynamoDbTemplate(dynamoDbEnhancedClient, tableSchemaRes, tableNameRes);
		}
	}

	static class MissingDaxUrlCondition extends NoneNestedConditions {
		MissingDaxUrlCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty("spring.cloud.aws.dynamodb.dax.url")
		static class DynamoDbDaxUrlPropertyCondition {
		}
	}

}
