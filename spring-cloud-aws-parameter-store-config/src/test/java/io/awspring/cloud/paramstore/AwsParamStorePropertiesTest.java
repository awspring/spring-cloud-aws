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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link AwsParamStoreProperties}.
 *
 * @author Matej Nedic
 * @author Maciej Walkowiak
 */
class AwsParamStorePropertiesTest {

	@ParameterizedTest
	@MethodSource("invalidProperties")
	public void validationFails(AwsParamStoreProperties properties, String field) throws Exception {
		try {
			properties.afterPropertiesSet();
			fail("validation should fail");
		}
		catch (ValidationException e) {
			assertThat(e.getField()).endsWith(field);
		}
	}

	@ParameterizedTest
	@MethodSource("validProperties")
	void validationSucceeds(AwsParamStoreProperties properties) {
		assertThatNoException().isThrownBy(properties::afterPropertiesSet);
	}

	@Test
	void checkExceptionLoggingForPrefix() {
		AwsParamStoreProperties properties = new AwsParamStorePropertiesBuilder().withPrefix("!.").build();
		assertThatThrownBy(properties::afterPropertiesSet)
				.hasMessage("The prefix value: !. must have pattern of: [a-zA-Z0-9.\\-/]+");
	}

	private static Stream<Arguments> validProperties() {
		return Stream.of(
				Arguments.of(new AwsParamStorePropertiesBuilder().withPrefix("/con").withDefaultContext("app")
						.withProfileSeparator("_").build()),
				Arguments.of(new AwsParamStorePropertiesBuilder().withPrefix("/config/someRandomValue-dev01")
						.withDefaultContext("app").withProfileSeparator("_").build()),
				Arguments.of(new AwsParamStorePropertiesBuilder().withPrefix("/con/test/bla").withDefaultContext("app")
						.withProfileSeparator("_").build()),
				Arguments.of(new AwsParamStorePropertiesBuilder().withPrefix("").withDefaultContext("app")
						.withProfileSeparator("_").build()),
				Arguments.of(new AwsParamStorePropertiesBuilder().withPrefix("/config").withDefaultContext("app")
						.withProfileSeparator("/").build()),
				Arguments.of(new AwsParamStorePropertiesBuilder().withPrefix("/config").withDefaultContext("app")
						.withProfileSeparator("\\").build()));
	}

	private static Stream<Arguments> invalidProperties() {
		return Stream.of(Arguments.of(new AwsParamStorePropertiesBuilder().withPrefix("!.").build(), "prefix"),
				Arguments.of(new AwsParamStorePropertiesBuilder().withDefaultContext("").build(), "defaultContext"),
				Arguments.of(new AwsParamStorePropertiesBuilder().withProfileSeparator("").build(), "profileSeparator"),
				Arguments.of(new AwsParamStorePropertiesBuilder().withProfileSeparator("!_").build(),
						"profileSeparator"));
	}

	private static class AwsParamStorePropertiesBuilder {

		private final AwsParamStoreProperties properties = new AwsParamStoreProperties();

		AwsParamStorePropertiesBuilder withPrefix(String prefix) {
			this.properties.setPrefix(prefix);
			return this;
		}

		AwsParamStorePropertiesBuilder withDefaultContext(String defaultContext) {
			this.properties.setDefaultContext(defaultContext);
			return this;
		}

		AwsParamStorePropertiesBuilder withProfileSeparator(String profileSeparator) {
			this.properties.setProfileSeparator(profileSeparator);
			return this;
		}

		AwsParamStoreProperties build() {
			return this.properties;
		}

	}

}
