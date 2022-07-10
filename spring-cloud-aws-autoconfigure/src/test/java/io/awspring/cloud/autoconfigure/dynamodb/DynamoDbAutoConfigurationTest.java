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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.dax.ClusterDaxClient;

/**
 *
 * @author Matej Nedic
 */
class DynamoDbAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1",
					"spring.cloud.aws.credentials.access-key:noop", "spring.cloud.aws.credentials.secret-key:noop")
			.withConfiguration(AutoConfigurations.of(AwsAutoConfiguration.class, RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, DynamoDbAutoConfiguration.class));

	@Test
	void dynamoDBAutoConfigurationIsDisabled() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.dynamodb.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(DynamoDbClient.class));
	}

	@Test
	void dynamoDBAutoConfigurationIsEnabled() {
		this.contextRunner
				.withPropertyValues("spring.cloud.aws.dynamodb.enabled:true",
						"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com")
				.run(context -> {
					assertThat(context).hasSingleBean(DynamoDbClient.class);
					assertThat(context).hasSingleBean(DynamoDbTemplate.class);
					assertThat(context).hasSingleBean(DynamoDbEnhancedClient.class);

					ClusterDaxClient client = context.getBean(ClusterDaxClient.class);
					software.amazon.dax.Configuration configuration = getConfiguration(client);
					assertThat(configuration.url()).isEqualTo("dax://something.dax-clusters.us-east-1.amazonaws.com");
				});
	}

	@Test
	void withCustomEndpoint() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com")
				.run(context -> {
					assertThat(context).hasSingleBean(DynamoDbClient.class);
					assertThat(context).hasSingleBean(DynamoDbTemplate.class);
					assertThat(context).hasSingleBean(DynamoDbEnhancedClient.class);

					ClusterDaxClient client = context.getBean(ClusterDaxClient.class);
					software.amazon.dax.Configuration configuration = getConfiguration(client);
					assertThat(configuration.url()).isEqualTo("dax://something.dax-clusters.us-east-1.amazonaws.com");
				});
	}

	@Test
	void customTableResolverResolverCanBeConfigured() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com")
				.withUserConfiguration(DynamoDbAutoConfigurationTest.CustomDynamoDbConfiguration.class).run(context -> {
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

				});
	}

	@Test
	void testWhenClusterDaxClientIsNotOnClassPath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ClusterDaxClient.class)).run(context -> {
			assertThat(context).hasSingleBean(DynamoDbClient.class);
			assertThat(context.getBean(DynamoDbClient.class)).isNotInstanceOf(ClusterDaxClient.class);
		});
	}

	@Test
	void withDynamoDbClientCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.dynamodb.endpoint:http://localhost:8090")
				.run(context -> {
					assertThat(context).hasSingleBean(DynamoDbClient.class);
					assertThat(context).hasSingleBean(DynamoDbTemplate.class);
					assertThat(context).hasSingleBean(DynamoDbEnhancedClient.class);

					ConfiguredAwsClient client = new ConfiguredAwsClient(context.getBean(DynamoDbClient.class));
					assertThat(client.getEndpoint()).isEqualTo(URI.create("http://localhost:8090"));
					assertThat(client.isEndpointOverridden()).isTrue();
				});
	}

	@Test
	void dynamoDbClientConfiguredSinceNoUrl() {
		this.contextRunner.run(context -> {
			assertThat(context).hasSingleBean(DynamoDbClient.class);
			assertThat(context).hasSingleBean(DynamoDbTemplate.class);
			assertThat(context).hasSingleBean(DynamoDbEnhancedClient.class);
			assertThat(context).doesNotHaveBean(ClusterDaxClient.class);
		});
	}

	@Test
	void customDynamoDbClientConfigurer() {
		this.contextRunner.withUserConfiguration(DynamoDbAutoConfigurationTest.CustomAwsClientConfig.class)
				.run(context -> {
					ConfiguredAwsClient sesClient = new ConfiguredAwsClient(context.getBean(DynamoDbClient.class));
					assertThat(sesClient.getApiCallTimeout()).isEqualTo(Duration.ofMillis(1999));
					assertThat(sesClient.getSyncHttpClient()).isNotNull();
				});
	}

	@Test
	void customDynamoDbClientDaxSettings() {
		this.contextRunner.withPropertyValues(
				"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com",
				"spring.cloud.aws.dynamodb.dax.writeRetries:4",
				"spring.cloud.aws.dynamodb.dax.connectTimeoutMillis:4000").run(context -> {
					ClusterDaxClient client = context.getBean(ClusterDaxClient.class);
					software.amazon.dax.Configuration configuration = getConfiguration(client);
					assertThat(configuration.url()).isEqualTo("dax://something.dax-clusters.us-east-1.amazonaws.com");
					assertThat(configuration.writeRetries()).isEqualTo(4);
					assertThat(configuration.connectTimeoutMillis()).isEqualTo(4000);
				});
	}

	private software.amazon.dax.Configuration getConfiguration(ClusterDaxClient client) {
		return ((software.amazon.dax.Configuration) ReflectionTestUtils.getField(
				ReflectionTestUtils.getField(ReflectionTestUtils.getField(client, "client"), "cluster"),
				"configuration"));
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
		AwsClientCustomizer<DynamoDbClientBuilder> snsClientBuilderAwsClientConfigurer() {
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
