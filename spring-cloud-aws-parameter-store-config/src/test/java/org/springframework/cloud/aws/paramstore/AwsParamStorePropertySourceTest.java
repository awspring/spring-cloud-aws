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

package org.springframework.cloud.aws.paramstore;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AwsParamStorePropertySourceTest {

	private AWSSimpleSystemsManagement ssmClient = mock(AWSSimpleSystemsManagement.class);

	private AwsParamStorePropertySource propertySource = new AwsParamStorePropertySource(
			"/config/myservice/", ssmClient);

	@Test
	public void followsNextToken() {
		GetParametersByPathResult firstResult = new GetParametersByPathResult()
				.withNextToken("next").withParameters(
						new Parameter().withName("/config/myservice/key1")
								.withValue("value1"),
						new Parameter().withName("/config/myservice/key2")
								.withValue("value2"));

		GetParametersByPathResult nextResult = new GetParametersByPathResult()
				.withParameters(
						new Parameter().withName("/config/myservice/key3")
								.withValue("value3"),
						new Parameter().withName("/config/myservice/key4")
								.withValue("value4"));

		when(ssmClient.getParametersByPath(any(GetParametersByPathRequest.class)))
				.thenReturn(firstResult, nextResult);

		propertySource.init();

		assertThat(propertySource.getPropertyNames()).containsExactly("key1", "key2",
				"key3", "key4");
		assertThat(propertySource.getProperty("key3")).isEqualTo("value3");
	}

}
