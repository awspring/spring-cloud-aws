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
package io.awspring.cloud.autoconfigure.dynamodb;

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.dynamodb.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.dax.ClusterDaxClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for DynamoDB integration.
 *
 * @author Matej Nedic
 * @since 3.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DynamoDbProperties.class)
@ConditionalOnClass({ DynamoDbClient.class, DynamoDbEnhancedClient.class, DynamoDbTemplate.class })
@AutoConfigureAfter({ CredentialsProviderAutoConfiguration.class, RegionProviderAutoConfiguration.class })
@ConditionalOnProperty(name = "spring.cloud.aws.dynamodb.enabled", havingValue = "true", matchIfMissing = true)
public class DynamoDbAutoConfiguration {
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "software.amazon.dax.ClusterDaxClient")
	static class DaxDynamoDbClient {

		@ConditionalOnMissingBean
		@Bean
		public DynamoDbClient dynamoDbClient(DynamoDbProperties properties, AwsCredentialsProvider credentialsProvider,
				AwsRegionProvider regionProvider, ObjectProvider<List<MetricPublisher>> metricsPublishers)
				throws IOException {
			DaxProperties daxProperties = properties.getDax();
			software.amazon.dax.Configuration.Builder configuration = software.amazon.dax.Configuration.builder()
					.idleTimeoutMillis(daxProperties.getIdleTimeoutMillis())
					.connectionTtlMillis(daxProperties.getConnectionTtlMillis())
					.connectTimeoutMillis(daxProperties.getConnectTimeoutMillis())
					.requestTimeoutMillis(daxProperties.getRequestTimeoutMillis())
					.writeRetries(daxProperties.getWriteRetries()).readRetries(daxProperties.getReadRetries())
					.clusterUpdateIntervalMillis(daxProperties.getClusterUpdateIntervalMillis())
					.endpointRefreshTimeoutMillis(daxProperties.getEndpointRefreshTimeoutMillis())
					.maxConcurrency(daxProperties.getMaxConcurrency())
					.maxPendingConnectionAcquires(daxProperties.getMaxPendingConnectionAcquires())
					.skipHostNameVerification(daxProperties.isSkipHostNameVerification())
					.region(AwsClientBuilderConfigurer.resolveRegion(properties, regionProvider))
					.credentialsProvider(credentialsProvider).url(properties.getDax().getUrl());
			if (metricsPublishers.getIfAvailable() != null) {
				metricsPublishers.getIfAvailable().forEach(configuration::addMetricPublisher);
			}
			return ClusterDaxClient.builder().overrideConfiguration(configuration.build()).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("software.amazon.dax.ClusterDaxClient")
	static class StandardDynamoDbClient {

		@ConditionalOnMissingBean
		@Bean
		public DynamoDbClient dynamoDbClient(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
				ObjectProvider<AwsClientCustomizer<DynamoDbClientBuilder>> configurer, DynamoDbProperties properties) {
			return awsClientBuilderConfigurer
					.configure(DynamoDbClient.builder(), properties, configurer.getIfAvailable()).build();
		}

	}

	@ConditionalOnMissingBean
	@Bean
	public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
		return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
	}

	@ConditionalOnMissingBean(DynamoDbOperations.class)
	@Bean
	public DynamoDbTemplate dynamoDBTemplate(DynamoDbEnhancedClient dynamoDbEnhancedClient,
			Optional<DynamoDbTableSchemaResolver> tableSchemaResolver,
			Optional<DynamoDbTableNameResolver> tableNameResolver) {
		DynamoDbTableSchemaResolver tableSchemaRes = tableSchemaResolver
				.orElseGet(DefaultDynamoDbTableSchemaResolver::new);
		DynamoDbTableNameResolver tableNameRes = tableNameResolver.orElseGet(DefaultDynamoDbTableNameResolver::new);
		return new DynamoDbTemplate(dynamoDbEnhancedClient, tableSchemaRes, tableNameRes);
	}

}
