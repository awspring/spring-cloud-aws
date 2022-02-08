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

package io.awspring.cloud.secretsmanager;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AwsSecretsManagerProperties}.
 *
 * @author Matej Nedic
 * @author Maciej Walkowiak
 */
class AwsSecretsManagerPropertiesTest {

	@ParameterizedTest
	@MethodSource("invalidProperties")
	public void validationFails(AwsSecretsManagerProperties properties, String field, String errorCode) {
		assertThatThrownBy(properties::afterPropertiesSet).isInstanceOf(ValidationException.class);
	}

	@ParameterizedTest
	@MethodSource("validProperties")
	void validationSucceeds(AwsSecretsManagerProperties properties) {
		assertThatNoException().isThrownBy(properties::afterPropertiesSet);
	}

	private static Stream<Arguments> validProperties() {
		return Stream.of(
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withPrefix("").build(), "prefix", "NotEmpty"),
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withPrefix("/sec").withDefaultContext("app")
						.withProfileSeparator("_").build()),
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withPrefix("/sec/test/var")
						.withDefaultContext("app").withProfileSeparator("_").build()),
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withPrefix("secret").withDefaultContext("app")
						.withProfileSeparator("_").build()),
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withPrefix("secret").withDefaultContext("app")
						.withProfileSeparator("\\").build()),
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withPrefix("secret").withDefaultContext("app")
						.withProfileSeparator("/").build()));
	}

	private static Stream<Arguments> invalidProperties() {
		return Stream.of(
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withPrefix("!.").build(), "prefix", "Pattern"),
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withDefaultContext("").build(), "defaultContext",
						"NotEmpty"),
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withProfileSeparator("").build(),
						"profileSeparator", "NotEmpty"),
				Arguments.of(new AwsSecretsManagerPropertiesBuilder().withProfileSeparator("!_").build(),
						"profileSeparator", "Pattern"));
	}

	private static class AwsSecretsManagerPropertiesBuilder {

		private final AwsSecretsManagerProperties properties = new AwsSecretsManagerProperties();

		AwsSecretsManagerPropertiesBuilder withPrefix(String prefix) {
			this.properties.setPrefix(prefix);
			return this;
		}

		AwsSecretsManagerPropertiesBuilder withDefaultContext(String defaultContext) {
			this.properties.setDefaultContext(defaultContext);
			return this;
		}

		AwsSecretsManagerPropertiesBuilder withProfileSeparator(String profileSeparator) {
			this.properties.setProfileSeparator(profileSeparator);
			return this;
		}

		AwsSecretsManagerProperties build() {
			return this.properties;
		}

	}

}
