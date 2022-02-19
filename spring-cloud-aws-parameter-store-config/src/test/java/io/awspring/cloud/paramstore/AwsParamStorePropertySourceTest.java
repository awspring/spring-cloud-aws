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

package io.awspring.cloud.paramstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsParamStorePropertySourceTest {

	private AWSSimpleSystemsManagement ssmClient = mock(AWSSimpleSystemsManagement.class);

	private AwsParamStorePropertySource propertySource = new AwsParamStorePropertySource("/config/myservice/",
			ssmClient);

	@Test
	void followsNextToken() {
		GetParametersByPathResult firstResult = new GetParametersByPathResult().withNextToken("next").withParameters(
				new Parameter().withName("/config/myservice/key1").withValue("value1"),
				new Parameter().withName("/config/myservice/key2").withValue("value2"));

		GetParametersByPathResult nextResult = new GetParametersByPathResult().withParameters(
				new Parameter().withName("/config/myservice/key3").withValue("value3"),
				new Parameter().withName("/config/myservice/key4").withValue("value4"));

		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class))).thenReturn(firstResult, nextResult);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).containsExactly("key1", "key2", "key3", "key4");
		assertThat(propertySource.getProperty("key3")).isEqualTo("value3");
	}

	@Test
	void arrayParameterNames() {
		GetParametersByPathResult result = new GetParametersByPathResult().withParameters(
				new Parameter().withName("/config/myservice/key_0_.value").withValue("value1"),
				new Parameter().withName("/config/myservice/key_0_.nested_0_.nestedValue")
						.withValue("key_nestedValue1"),
				new Parameter().withName("/config/myservice/key_0_.nested_1_.nestedValue")
						.withValue("key_nestedValue2"),
				new Parameter().withName("/config/myservice/key_1_.value").withValue("value2"),
				new Parameter().withName("/config/myservice/key_1_.nested_0_.nestedValue")
						.withValue("key_nestedValue1"),
				new Parameter().withName("/config/myservice/key_1_.nested_1_.nestedValue")
						.withValue("key_nestedValue2"));

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

}
