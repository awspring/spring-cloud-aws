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

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.dynamodb.DynamoDbTableNameResolver;
import io.awspring.cloud.dynamodb.DynamoDbTableSchemaResolver;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.dax.ClusterDaxClient;

/**
 *
 * @author Matej Nedic
 */
class DynamoDbAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.aws.region.static:eu-west-1")
			.withConfiguration(AutoConfigurations.of(RegionProviderAutoConfiguration.class,
					CredentialsProviderAutoConfiguration.class, DynamoDbAutoConfiguration.class,
					AwsAutoConfiguration.class));

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
					software.amazon.dax.Configuration configuration = getConfiugration(client);
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
					software.amazon.dax.Configuration configuration = getConfiugration(client);
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
	void customDynamoDbClientDaxSettings() {
		this.contextRunner.withPropertyValues(
				"spring.cloud.aws.dynamodb.dax.url:dax://something.dax-clusters.us-east-1.amazonaws.com",
				"spring.cloud.aws.dynamodb.dax.writeRetries:4",
				"spring.cloud.aws.dynamodb.dax.connectTimeoutMillis:4000").run(context -> {
					ClusterDaxClient client = context.getBean(ClusterDaxClient.class);
					software.amazon.dax.Configuration configuration = getConfiugration(client);
					assertThat(configuration.url()).isEqualTo("dax://something.dax-clusters.us-east-1.amazonaws.com");
					assertThat(configuration.writeRetries()).isEqualTo(4);
					assertThat(configuration.connectTimeoutMillis()).isEqualTo(4000);
				});
	}

	private software.amazon.dax.Configuration getConfiugration(ClusterDaxClient client) {
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

}
