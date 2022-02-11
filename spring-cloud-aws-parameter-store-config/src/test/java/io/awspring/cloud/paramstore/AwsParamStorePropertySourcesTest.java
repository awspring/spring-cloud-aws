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

package io.awspring.cloud.paramstore;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link AwsParamStorePropertySourceLocator}.
 *
 * @author Manuel Wessner
 */
class AwsParamStorePropertySourcesTest {

	private AwsParamStoreProperties properties;

	@BeforeEach
	void setUp() {
		properties = new AwsParamStoreProperties();
		properties.setDefaultContext("application");
		properties.setName("messaging-service");
	}

	@Test
	void getAutomaticContextsWithSingleProfile() {
		AwsParamStorePropertySources propertySource = new AwsParamStorePropertySources(properties);

		List<String> contexts = propertySource.getAutomaticContexts(Collections.singletonList("production"));

		assertThat(contexts.size()).isEqualTo(4);
		assertThat(contexts).containsExactly("/config/application/", "/config/application_production/",
				"/config/messaging-service/", "/config/messaging-service_production/");
	}

	@Test
	void getAutomaticContextsWithoutProfile() {
		AwsParamStorePropertySources propertySource = new AwsParamStorePropertySources(properties);

		List<String> contexts = propertySource.getAutomaticContexts(Collections.emptyList());

		assertThat(contexts.size()).isEqualTo(2);
		assertThat(contexts).containsExactly("/config/application/", "/config/messaging-service/");
	}

	@Test
	void getAutomaticContextsWithSingleProfileWithPrefixEmpty() {
		AwsParamStoreProperties properties = new AwsParamStoreProperties();
		properties.setName("messaging-service");
		properties.setPrefix("");
		AwsParamStorePropertySources propertySource = new AwsParamStorePropertySources(properties);

		List<String> contexts = propertySource.getAutomaticContexts(Collections.singletonList("production"));

		assertThat(contexts.size()).isEqualTo(4);
		assertThat(contexts).containsExactly("/application/", "/application_production/", "/messaging-service/",
				"/messaging-service_production/");
	}

}
