/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.secretsmanager;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsSecretsManagerPropertySourceTest {

	private AWSSecretsManager smClient = mock(AWSSecretsManager.class);

	private AwsSecretsManagerPropertySource propertySource = new AwsSecretsManagerPropertySource(
			"/config/myservice", smClient);

	@Test
	public void shouldParseSecretValue() {
		GetSecretValueResult secretValueResult = new GetSecretValueResult();
		secretValueResult.setSecretString("{\"key1\": \"value1\", \"key2\": \"value2\"}");

		when(smClient.getSecretValue(any(GetSecretValueRequest.class)))
				.thenReturn(secretValueResult);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).containsExactly("key1", "key2");
		assertThat(propertySource.getProperty("key1")).isEqualTo("value1");
		assertThat(propertySource.getProperty("key2")).isEqualTo("value2");
	}

}
