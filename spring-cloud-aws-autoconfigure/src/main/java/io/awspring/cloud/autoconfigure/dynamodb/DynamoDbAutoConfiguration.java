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

import static io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer.createSpecificMetricPublisher;

import io.awspring.cloud.autoconfigure.core.AwsClientBuilderConfigurer;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.dynamodb.*;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

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

	private final DynamoDbProperties properties;

	public DynamoDbAutoConfiguration(DynamoDbProperties properties) {
		this.properties = properties;
	}

	@ConditionalOnMissingBean
	@Bean
	public DynamoDbClientBuilder dynamoDbClientBuilder(AwsClientBuilderConfigurer awsClientBuilderConfigurer,
			ObjectProvider<AwsClientCustomizer<DynamoDbClientBuilder>> configurer,
			ObjectProvider<MetricPublisher> metricPublisherObjectProvider) {
		MetricPublisher metricPublisher = createSpecificMetricPublisher(metricPublisherObjectProvider.getIfAvailable(),
				properties);
		return awsClientBuilderConfigurer.configure(DynamoDbClient.builder(), properties, configurer.getIfAvailable(),
				metricPublisher);
	}

	@ConditionalOnMissingBean
	@Bean
	public DynamoDbClient dynamoDbClient(DynamoDbClientBuilder dynamoDbClientBuilder) {
		return dynamoDbClientBuilder.build();
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
