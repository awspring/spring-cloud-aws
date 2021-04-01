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

package io.awspring.cloud.v3.paramstore;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsParameterStorePropertySourceTest {

	private SsmClient ssmClient = mock(SsmClient.class);

	private AwsParameterStorePropertySource propertySource = new AwsParameterStorePropertySource("/config/myservice/",
			ssmClient);

	@Test
	void followsNextToken() {
		GetParametersByPathResponse firstResult = GetParametersByPathResponse.builder().nextToken("next")
				.parameters(Parameter.builder().name("/config/myservice/key1").value("value1").build(),
						Parameter.builder().name("/config/myservice/key2").value("value2").build())
				.build();

		GetParametersByPathResponse nextResult = GetParametersByPathResponse.builder()
				.parameters(Parameter.builder().name("/config/myservice/key3").value("value3").build(),
						Parameter.builder().name("/config/myservice/key4").value("value4").build())
				.build();

		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(firstResult, nextResult);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).containsExactly("key1", "key2", "key3", "key4");
		assertThat(propertySource.getProperty("key3")).isEqualTo("value3");
	}

}
