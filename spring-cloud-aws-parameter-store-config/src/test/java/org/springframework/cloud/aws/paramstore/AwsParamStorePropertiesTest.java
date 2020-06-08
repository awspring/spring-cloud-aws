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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AwsParamStoreProperties}.
 *
 * @author Matej Nedic
 * @author Maciej Walkowiak
 */
public class AwsParamStorePropertiesTest {

	@ParameterizedTest
	@MethodSource("invalidProperties")
	public void validationFails(AwsParamStoreProperties properties, String field,
			String errorCode) {
		Errors errors = new BeanPropertyBindingResult(properties, "properties");

		properties.validate(properties, errors);

		assertThat(errors.getFieldError(field)).isNotNull();
		assertThat(errors.getFieldError(field).getCode()).isEqualTo(errorCode);
	}

	@Test
	void validationSucceeds() {
		AwsParamStoreProperties properties = new AwsParamStorePropertiesBuilder()
				.withPrefix("/con").withDefaultContext("app").withProfileSeparator("_")
				.build();

		Errors errors = new BeanPropertyBindingResult(properties, "properties");
		properties.validate(properties, errors);

		assertThat(errors.getAllErrors()).isEmpty();
	}

	private static Stream<Arguments> invalidProperties() {
		return Stream.of(
				Arguments.of(new AwsParamStorePropertiesBuilder().withPrefix("").build(),
						"prefix", "NotEmpty"),
				Arguments.of(
						new AwsParamStorePropertiesBuilder().withPrefix("!.").build(),
						"prefix", "Pattern"),
				Arguments.of(new AwsParamStorePropertiesBuilder().withDefaultContext("")
						.build(), "defaultContext", "NotEmpty"),
				Arguments.of(new AwsParamStorePropertiesBuilder().withProfileSeparator("")
						.build(), "profileSeparator", "NotEmpty"),
				Arguments.of(new AwsParamStorePropertiesBuilder()
						.withProfileSeparator("!_").build(), "profileSeparator",
						"Pattern"));
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
