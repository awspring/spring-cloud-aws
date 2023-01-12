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
package io.awspring.cloud.secretsmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

/**
 * Tests for {@link SecretsManagerPropertySource}.
 *
 */

class SecretsManagerPropertySourceTest {

	private SecretsManagerClient client = mock(SecretsManagerClient.class);

	@Test
	void shouldParseSecretValue() {
		SecretsManagerPropertySource propertySource = new SecretsManagerPropertySource("/config/myservice", client);
		GetSecretValueResponse secretValueResult = GetSecretValueResponse.builder()
				.secretString("{\"key1\": \"value1\", \"key2\": \"value2\"}").build();

		when(client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValueResult);

		propertySource.init();

		assertThat(propertySource.getName()).isEqualTo("aws-secretsmanager:/config/myservice");
		assertThat(propertySource.getPropertyNames()).containsExactly("key1", "key2");
		assertThat(propertySource.getProperty("key1")).isEqualTo("value1");
		assertThat(propertySource.getProperty("key2")).isEqualTo("value2");
	}

	@Test
	void shouldParsePlainTextSecretValue() {
		SecretsManagerPropertySource propertySource = new SecretsManagerPropertySource("/config/myservice", client);
		GetSecretValueResponse secretValueResult = GetSecretValueResponse.builder().secretString("my secret")
				.name("secret name").build();

		when(client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValueResult);

		propertySource.init();

		assertThat(propertySource.getName()).isEqualTo("aws-secretsmanager:/config/myservice");
		assertThat(propertySource.getPropertyNames()).containsExactly("secret name");
		assertThat(propertySource.getProperty("secret name")).isEqualTo("my secret");
	}

	@Test
	void throwsExceptionWhenSecretNotFound() {
		SecretsManagerPropertySource propertySource = new SecretsManagerPropertySource("/config/myservice", client);
		when(client.getSecretValue(any(GetSecretValueRequest.class)))
				.thenThrow(ResourceNotFoundException.builder().message("secret not found").build());

		assertThatThrownBy(propertySource::init).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void resolvesPrefixAndSecretIdFromContext() {
		SecretsManagerPropertySource propertySource = new SecretsManagerPropertySource("/config/myservice?prefix=xxx",
				client);
		assertThat(propertySource.getName()).isEqualTo("aws-secretsmanager:/config/myservice?prefix=xxx");
		assertThat(propertySource.getPrefix()).isEqualTo("xxx");
		assertThat(propertySource.getContext()).isEqualTo("/config/myservice?prefix=xxx");
		assertThat(propertySource.getSecretId()).isEqualTo("/config/myservice");
	}

	@Test
	void addsPrefixToJsonSecret() {
		SecretsManagerPropertySource propertySource = new SecretsManagerPropertySource("/config/myservice?prefix=xxx-",
				client);
		GetSecretValueResponse secretValueResult = GetSecretValueResponse.builder()
				.secretString("{\"key1\": \"value1\", \"key2\": \"value2\"}").build();
		when(client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValueResult);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).contains("xxx-key1", "xxx-key2");
		assertThat(propertySource.getProperty("xxx-key1")).isEqualTo("value1");
		assertThat(propertySource.getProperty("key1")).isNull();
	}

	@Test
	void addsPrefixToPlainTextSecret() {
		SecretsManagerPropertySource propertySource = new SecretsManagerPropertySource("/config/myservice?prefix=yyy.",
				client);

		GetSecretValueResponse secretValueResult = GetSecretValueResponse.builder().secretString("my secret")
				.name("key1").build();

		when(client.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValueResult);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).containsExactly("yyy.key1");
		assertThat(propertySource.getProperty("yyy.key1")).isEqualTo("my secret");
		assertThat(propertySource.getProperty("key1")).isNull();
	}

	@Test
	void copyPreservesPrefix() {
		SecretsManagerPropertySource propertySource = new SecretsManagerPropertySource("/config/myservice?prefix=yyy",
				client);
		SecretsManagerPropertySource copy = propertySource.copy();
		assertThat(propertySource.getContext()).isEqualTo(copy.getContext());
		assertThat(propertySource.getPrefix()).isEqualTo(copy.getPrefix());
	}

}
