/*
 * Copyright 2013-2021 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link AwsSecretsManagerPropertySources}.
 *
 * @author Manuel Wessner
 */
class AwsSecretsManagerPropertySourcesTests {

	private final Log logMock = mock(Log.class);

	private AwsSecretsManagerProperties properties;

	@BeforeEach
	void setUp() {
		properties = new AwsSecretsManagerProperties();
		properties.setDefaultContext("application");
		properties.setName("messaging-service");
	}

	@Test
	void getAutomaticContextsWithSingleProfile() {
		AwsSecretsManagerPropertySources propertySource = new AwsSecretsManagerPropertySources(properties, logMock);

		List<String> contexts = propertySource.getAutomaticContexts(Collections.singletonList("production"));

		assertThat(contexts.size()).isEqualTo(4);
		assertThat(contexts).containsExactly("/secret/application", "/secret/application_production",
				"/secret/messaging-service", "/secret/messaging-service_production");
	}

	@Test
	void getAutomaticContextsWithoutProfile() {
		AwsSecretsManagerPropertySources propertySource = new AwsSecretsManagerPropertySources(properties, logMock);

		List<String> contexts = propertySource.getAutomaticContexts(Collections.emptyList());

		assertThat(contexts.size()).isEqualTo(2);
		assertThat(contexts).containsExactly("/secret/application", "/secret/messaging-service");
	}

}
