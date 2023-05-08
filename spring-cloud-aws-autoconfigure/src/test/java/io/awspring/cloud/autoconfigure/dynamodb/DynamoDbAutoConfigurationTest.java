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

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.autoconfigure.ConfiguredAwsClient;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.AwsClientCustomizer;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.dynamodb.DynamoDbTableNameResolver;
import io.awspring.cloud.dynamodb.DynamoDbTableSchemaResolver;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.dax.ClusterDaxAsyncClient;
import software.amazon.dax.ClusterDaxClient;

/**
 * Tests for {@link DynamoDbAutoConfiguration}.
 *
 * @author Matej Nedic
 */
class DynamoDbAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1",
					"spring.cloud.aws.credentials.access-key:noop", "spring.cloud.aws.credentials.secret-key:noop")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, DynamoDbAutoConfiguration.class));

	@Nested
	class DynamoDbTests {
		@Test
		void dynamoDBAutoConfigurationIsDisabled() {
			contextRunner.withPropertyValues("spring.cloud.aws.dynamodb.enabled:false")
					.run(context -> assertThat(context).doesNotHaveBean(DynamoDbClient.class));
		}

		@Test
		void customTableResolverResolverCanBeConfigured() {
			contextRunner.withUserConfiguration(CustomDynamoDbConfiguration.class).run(context -> {
				DynamoDbTableSchemaResolver dynamoDbTableSchemaResolver = context
						.getBean(DynamoDbTableSchemaResolver.class);
				DynamoDbTableNameResolver dynamoDBDynamoDbTableNameResolver = context
						.getBean(DynamoDbTableNameResolver.class);

				assertThat(dynamoDbTableSchemaResolver).isNotNull();
				assertThat(dynamoDBDynamoDbTableNameResolver).isNotNull();

				assertThat(dynamoDbTableSchemaResolver).isInstanceOf(CustomDynamoDBDynamoDbTableSchemaResolver.class);
				assertThat(dynamoDBDynamoDbTableNameResolver)
						.isInstanceOf(CustomDynamoDBDynamoDbTableNameResolver.class);

			});
		}

		@Test
		void doesNotCreateDaxClientWhenDaxNotInClasspath() {
			contextRunner.withClassLoader(new FilteredClassLoader(ClusterDaxClient.class)).run(context -> {
				assertThat(context).hasSingleBean(DynamoDbClient.class);
				assertThat(context.getBean(DynamoDbClient.class)).isNotInstanceOf(ClusterDaxClient.class);
			});
		}

		@Test
		void withDynamoDbClientCustomEndpoint() {
			contextRunner.withPropertyValues("spring.cloud.aws.dynamodb.endpoint:http://localhost:8090")
					.run(context -> {
						assertThat(context).hasSingleBean(DynamoDbClient.class);
						assertThat(context).hasSingleBean(DynamoDbAsyncClient.class);
						assertThat(context).hasSingleBean(DynamoDbTemplate.class);
						assertThat(context).hasSingleBean(DynamoDbEnhancedClient.class);

						ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(DynamoDbClient.class));
						assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
						assertThat(client.isEndpointOverridden()).isTrue();
					});
		}

		@Test
		void dynamoDbClientConfiguredSinceNoUrl() {
			contextRunner.run(context -> {
				assertThat(context).hasSingleBean(DynamoDbClient.class);
				assertThat(context).hasSingleBean(DynamoDbAsyncClient.class);
				assertThat(context).hasSingleBean(DynamoDbTemplate.class);
				assertThat(context).hasSingleBean(DynamoDbEnhancedClient.class);
				assertThat(context).doesNotHaveBean(ClusterDaxClient.class);
			});
		}

		@Test
		void customDynamoDbClientConfigurer() {
			contextRunner.withUserConfiguration(DynamoDbAutoConfigurationTest.CustomAwsClientConfig.class)
					.run(context -> {
						ConfiguredAwsClient dynamoDbClient = new ConfiguredAwsClient(
								context.getBean(DynamoDbClient.class));
						assertThat(dynamoDbClient.getApiCallTimeout()).isEqualTo(Duration.ofMillis(1999));
						assertThat(dynamoDbClient.getSyncHttpClient()).isNotNull();
					});
		}
	}

	@Nested
	class DaxTests {

		@Test
		void propertiesAreAppliedToDaxClient() {
			contextRunner.withPropertyValues(
					"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com",
					"spring.cloud.aws.dynamodb.dax.write-retries:4", "spring.cloud.aws.dynamodb.dax.read-retries:5",
					"spring.cloud.aws.dynamodb.dax.idle-timeout-millis:10",
					"spring.cloud.aws.dynamodb.dax.request-timeout-millis:20",
					"spring.cloud.aws.dynamodb.dax.connection-ttl-millis:30",
					"spring.cloud.aws.dynamodb.dax.cluster-update-interval-millis:40",
					"spring.cloud.aws.dynamodb.dax.endpoint-refresh-timeout-millis:50",
					"spring.cloud.aws.dynamodb.dax.max-concurrency:60",
					"spring.cloud.aws.dynamodb.dax.max-pending-connection-acquires:70",
					"spring.cloud.aws.dynamodb.dax.skip-hostname-verification:true",
					"spring.cloud.aws.dynamodb.dax.connect-timeout-millis:80").run(context -> {
						ConfiguredDaxClient daxClient = new ConfiguredDaxClient(
								context.getBean(ClusterDaxClient.class));
						assertThat(daxClient.getUrl())
								.isEqualTo("dax://something.dax-clusters.us-east-1.amazonaws.com");
						assertThat(daxClient.getWriteRetries()).isEqualTo(4);
						assertThat(daxClient.getReadRetries()).isEqualTo(5);
						assertThat(daxClient.getIdleTimeoutMillis()).isEqualTo(10);
						assertThat(daxClient.getRequestTimeoutMillis()).isEqualTo(20);
						assertThat(daxClient.getConnectionTtlMillis()).isEqualTo(30);
						assertThat(daxClient.getClusterUpdateIntervalMillis()).isEqualTo(40);
						assertThat(daxClient.getEndpointRefreshTimeoutMillis()).isEqualTo(50);
						assertThat(daxClient.getMaxConcurrency()).isEqualTo(60);
						assertThat(daxClient.getMaxPendingConnectionAcquires()).isEqualTo(70);
						assertThat(daxClient.getSkipHostNameVerification()).isTrue();
						assertThat(daxClient.getConnectTimeoutMillis()).isEqualTo(80);
					});
		}

		@Test
		void defaultsAreUsedWhenPropertiesAreNotSet() {
			contextRunner
					.withPropertyValues(
							"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com")
					.run(context -> {
						ConfiguredDaxClient daxClient = new ConfiguredDaxClient(
								context.getBean(ClusterDaxClient.class));
						assertThat(context).hasSingleBean(ClusterDaxAsyncClient.class);
						assertThat(daxClient.getUrl())
								.isEqualTo("dax://something.dax-clusters.us-east-1.amazonaws.com");
						assertThat(daxClient.getWriteRetries()).isEqualTo(2);
						assertThat(daxClient.getReadRetries()).isEqualTo(2);
						assertThat(daxClient.getIdleTimeoutMillis()).isEqualTo(30000);
						assertThat(daxClient.getRequestTimeoutMillis()).isEqualTo(1000);
						assertThat(daxClient.getConnectionTtlMillis()).isEqualTo(0);
						assertThat(daxClient.getClusterUpdateIntervalMillis()).isEqualTo(4000);
						assertThat(daxClient.getEndpointRefreshTimeoutMillis()).isEqualTo(6000);
						assertThat(daxClient.getMaxConcurrency()).isEqualTo(1000);
						assertThat(daxClient.getMaxPendingConnectionAcquires()).isEqualTo(10000);
						assertThat(daxClient.getSkipHostNameVerification()).isFalse();
						assertThat(daxClient.getConnectTimeoutMillis()).isEqualTo(1000);
					});
		}

		@Test
		void clusterDaxClientWithCustomEndpoint() {
			contextRunner
					.withPropertyValues(
							"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com")
					.run(context -> {
						assertThat(context).hasSingleBean(DynamoDbClient.class);
						assertThat(context).hasSingleBean(DynamoDbTemplate.class);
						assertThat(context).hasSingleBean(DynamoDbEnhancedClient.class);

						ConfiguredDaxClient daxClient = new ConfiguredDaxClient(
								context.getBean(ClusterDaxClient.class));
						assertThat(daxClient.getUrl())
								.isEqualTo("dax://something.dax-clusters.us-east-1.amazonaws.com");
					});
		}

		@Test
		void clusterDaxClient_CustomUrl_DefaultValues() {
			contextRunner
					.withPropertyValues(
							"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com")
					.run(context -> {
						ConfiguredDaxClient daxClient = new ConfiguredDaxClient(
								context.getBean(ClusterDaxClient.class));
						assertThat(context).hasSingleBean(ClusterDaxAsyncClient.class);
						assertThat(daxClient.getUrl())
								.isEqualTo("dax://something.dax-clusters.us-east-1.amazonaws.com");
						assertThat(daxClient.getWriteRetries()).isEqualTo(2);
						assertThat(daxClient.getConnectTimeoutMillis()).isEqualTo(1000);
					});
		}

		@Test
		void customTableResolverResolverCanBeConfigured() {
			contextRunner
					.withPropertyValues(
							"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com")
					.withUserConfiguration(CustomDynamoDbConfiguration.class).run(context -> {
						DynamoDbTableSchemaResolver dynamoDbTableSchemaResolver = context
								.getBean(DynamoDbTableSchemaResolver.class);
						DynamoDbTableNameResolver dynamoDBDynamoDbTableNameResolver = context
								.getBean(DynamoDbTableNameResolver.class);

						assertThat(dynamoDbTableSchemaResolver).isNotNull();
						assertThat(dynamoDBDynamoDbTableNameResolver).isNotNull();

						assertThat(dynamoDbTableSchemaResolver)
								.isInstanceOf(CustomDynamoDBDynamoDbTableSchemaResolver.class);
						assertThat(dynamoDBDynamoDbTableNameResolver)
								.isInstanceOf(CustomDynamoDBDynamoDbTableNameResolver.class);

						assertThat(context).hasSingleBean(ClusterDaxAsyncClient.class);
					});
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDynamoDbConfiguration {

		@Bean
		DynamoDbTableSchemaResolver tableSchemaResolver() {
			return new CustomDynamoDBDynamoDbTableSchemaResolver();
		}

		@Bean
		DynamoDbTableNameResolver tableNameResolver() {
			return new CustomDynamoDBDynamoDbTableNameResolver();
		}
	}

	static class CustomDynamoDBDynamoDbTableSchemaResolver implements DynamoDbTableSchemaResolver {

		@Override
		public <T> TableSchema resolve(Class<T> clazz, String tableName) {
			return null;
		}

	}

	static class CustomDynamoDBDynamoDbTableNameResolver implements DynamoDbTableNameResolver {

		@Override
		public String resolve(Class clazz) {
			return null;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAwsClientConfig {

		@Bean
		AwsClientCustomizer<DynamoDbClientBuilder> dynamoDbClientBuilderAwsClientConfigurer() {
			return new DynamoDbAutoConfigurationTest.CustomAwsClientConfig.DynamoDbClientCustomizer();
		}

		static class DynamoDbClientCustomizer implements AwsClientCustomizer<DynamoDbClientBuilder> {
			@Override
			@Nullable
			public ClientOverrideConfiguration overrideConfiguration() {
				return ClientOverrideConfiguration.builder().apiCallTimeout(Duration.ofMillis(1999)).build();
			}

			@Override
			@Nullable
			public SdkHttpClient httpClient() {
				return ApacheHttpClient.builder().connectionTimeout(Duration.ofMillis(1542)).build();
			}
		}

	}

}
