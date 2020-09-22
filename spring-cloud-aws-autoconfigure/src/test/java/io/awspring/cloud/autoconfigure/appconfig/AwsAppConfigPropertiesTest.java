/*
 * Copyright 2013-2020 the original author or authors.
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

package io.awspring.cloud.autoconfigure.appconfig;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author jarpz
 */
class AwsAppConfigPropertiesTest {

	@ParameterizedTest
	@MethodSource("invalidProperties")
	void validationFails(AwsAppConfigProperties properties, String field, String errorCode) {
		Errors errors = new BeanPropertyBindingResult(properties, "properties");

		properties.validate(properties, errors);

		assertThat(errors.getFieldError(field)).isNotNull();
		assertThat(errors.getFieldError(field).getCode()).isEqualTo(errorCode);
	}

	private static Stream<Arguments> invalidProperties() {
		return Stream.of(Arguments.of(buildProperties("", null, null, null), "accountId", "NotEmpty"),
				Arguments.of(buildProperties(null, "", null, null), "application", "NotEmpty"),
				Arguments.of(buildProperties(null, null, null, ""), "environment", "NotEmpty"));
	}

	@Test
	void validationSucceeds() {
		AwsAppConfigProperties properties = buildProperties("12345678", "my-project", "my-app", "dev");

		Errors errors = new BeanPropertyBindingResult(properties, "properties");
		properties.validate(properties, errors);

		assertThat(errors.getAllErrors()).isEmpty();
	}

	private static AwsAppConfigProperties buildProperties(String accountId, String application,
			String configurationProfile, String environment) {
		AwsAppConfigProperties properties = new AwsAppConfigProperties();
		properties.setAccountId(accountId);
		properties.setApplication(application);
		properties.setConfigurationProfile(configurationProfile);
		properties.setEnvironment(environment);
		return properties;
	}

}
