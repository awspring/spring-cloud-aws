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

import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.CredentialsProviderAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.RegionProviderAutoConfiguration;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import io.awspring.cloud.dynamodb.TableSchemaResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.utils.AttributeMap;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Matej Nedic
 */
public class DynamoDbAutoConfigurationTest {

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
		this.contextRunner.withPropertyValues("spring.cloud.aws.dynamodb.enabled:true").run(context -> {
			assertThat(context).hasSingleBean(DynamoDbClient.class);
			assertThat(context).hasSingleBean(DynamoDbTemplate.class);
			assertThat(context).hasSingleBean(DynamoDbEnhancedClient.class);

			DynamoDbClient client = context.getBean(DynamoDbClient.class);
			SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(client,
				"clientConfiguration");
			AttributeMap attributes = (AttributeMap) ReflectionTestUtils.getField(clientConfiguration, "attributes");
			assertThat(attributes.get(SdkClientOption.ENDPOINT))
				.isEqualTo(URI.create("https://dynamodb.eu-west-1.amazonaws.com"));

		});
	}

	@Test
	void withCustomEndpoint() {
		this.contextRunner.withPropertyValues("spring.cloud.aws.dynamodb.endpoint:http://localhost:8090").run(context -> {
			assertThat(context).hasSingleBean(DynamoDbClient.class);
			assertThat(context).hasSingleBean(DynamoDbTemplate.class);
			assertThat(context).hasSingleBean(DynamoDbEnhancedClient.class);

			DynamoDbClient client = context.getBean(DynamoDbClient.class);

			SdkClientConfiguration clientConfiguration = (SdkClientConfiguration) ReflectionTestUtils.getField(client,
				"clientConfiguration");
			AttributeMap attributes = (AttributeMap) ReflectionTestUtils.getField(clientConfiguration, "attributes");
			assertThat(attributes.get(SdkClientOption.ENDPOINT)).isEqualTo(URI.create("http://localhost:8090"));
			assertThat(attributes.get(SdkClientOption.ENDPOINT_OVERRIDDEN)).isTrue();
		});
	}

	@Test
	void customTableResolverResolverCanBeConfigured() {
		this.contextRunner.withUserConfiguration(DynamoDbAutoConfigurationTest.CustomDynamoDbConfiguration.class)
			.run(context -> assertThat(context).hasSingleBean(DynamoDbAutoConfigurationTest.CustomDynamoDBTableResolver.class));
	}


	@Configuration(proxyBeanMethods = false)
	static class CustomDynamoDbConfiguration {

		@Bean
		TableSchemaResolver tableSchemaResolver() {
			return new CustomDynamoDBTableResolver();
		}
	}

	static class CustomDynamoDBTableResolver implements TableSchemaResolver {

		@Override
		public <T> TableSchema resolve(Class<T> clazz, String tableName) {
			return null;
		}

	}

}
