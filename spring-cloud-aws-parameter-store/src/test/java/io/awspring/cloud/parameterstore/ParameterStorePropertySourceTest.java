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
package io.awspring.cloud.parameterstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

/**
 * Unit tests for {@link ParameterStorePropertySource}.
 *
 * @author Joris Kuipers
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Matej Nedic
 */
class ParameterStorePropertySourceTest {

	private SsmClient ssmClient = mock(SsmClient.class);

	private ParameterStorePropertySource propertySource = new ParameterStorePropertySource("/config/myservice/",
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

		assertThat(propertySource.getName()).isEqualTo("aws-parameterstore:/config/myservice/");
		assertThat(propertySource.getPropertyNames()).containsExactly("key1", "key2", "key3", "key4");
		assertThat(propertySource.getProperty("key3")).isEqualTo("value3");
	}

	@Test
	void arrayParameterNames() {
		GetParametersByPathResponse result = GetParametersByPathResponse.builder()
				.parameters(Parameter.builder().name("/config/myservice/key_0_.value").value("value1").build(),
						Parameter.builder().name("/config/myservice/key_0_.nested_0_.nestedValue")
								.value("key_nestedValue1").build(),
						Parameter.builder().name("/config/myservice/key_0_.nested_1_.nestedValue")
								.value("key_nestedValue2").build(),
						Parameter.builder().name("/config/myservice/key_1_.value").value("value2").build(),
						Parameter.builder().name("/config/myservice/key_1_.nested_0_.nestedValue")
								.value("key_nestedValue1").build(),
						Parameter.builder().name("/config/myservice/key_1_.nested_1_.nestedValue")
								.value("key_nestedValue2").build())
				.build();

		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(result);

		propertySource.init();

		assertSoftly(it -> {
			it.assertThat(propertySource.getPropertyNames()).containsExactly("key[0].value",
					"key[0].nested[0].nestedValue", "key[0].nested[1].nestedValue", "key[1].value",
					"key[1].nested[0].nestedValue", "key[1].nested[1].nestedValue");
			it.assertThat(propertySource.getProperty("key[0].value")).isEqualTo("value1");
			it.assertThat(propertySource.getProperty("key[1].nested[1].nestedValue")).isEqualTo("key_nestedValue2");
		});
	}

	@Test
	void resolvesPrefixAndParameterPathFromContext() {
		ParameterStorePropertySource propertySource = new ParameterStorePropertySource("/config/myservice/?prefix=xxx",
				ssmClient);
		assertThat(propertySource.getName()).isEqualTo("aws-parameterstore:/config/myservice/?prefix=xxx");
		assertThat(propertySource.getPrefix()).isEqualTo("xxx");
		assertThat(propertySource.getContext()).isEqualTo("/config/myservice/?prefix=xxx");
		assertThat(propertySource.getParameterPath()).isEqualTo("/config/myservice/");
	}

	@Test
	void addsPrefixToParameter() {
		ParameterStorePropertySource propertySource = new ParameterStorePropertySource("/config/myservice/?prefix=yyy.",
				ssmClient);

		Parameter parameter = Parameter.builder().name("key1").value("my parameter").build();
		GetParametersByPathResponse parametersByPathResponse = GetParametersByPathResponse.builder()
				.parameters(parameter).build();

		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(parametersByPathResponse);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).containsExactly("yyy.key1");
		assertThat(propertySource.getProperty("yyy.key1")).isEqualTo("my parameter");
		assertThat(propertySource.getProperty("key1")).isNull();
	}

	@Test
	void copyPreservesPrefix() {
		ParameterStorePropertySource propertySource = new ParameterStorePropertySource("/config/myservice/?prefix=yyy",
				ssmClient);
		ParameterStorePropertySource copy = propertySource.copy();
		assertThat(propertySource.getContext()).isEqualTo(copy.getContext());
		assertThat(propertySource.getPrefix()).isEqualTo(copy.getPrefix());
	}
}
