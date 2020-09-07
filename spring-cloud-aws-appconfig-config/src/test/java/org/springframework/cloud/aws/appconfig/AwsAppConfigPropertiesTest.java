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

package org.springframework.cloud.aws.appconfig;

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
public class AwsAppConfigPropertiesTest {

	@ParameterizedTest
	@MethodSource("invalidProperties")
	public void validationFails(AwsAppConfigProperties properties, String field,
			String errorCode) {
		Errors errors = new BeanPropertyBindingResult(properties, "properties");

		properties.validate(properties, errors);

		assertThat(errors.getFieldError(field)).isNotNull();
		assertThat(errors.getFieldError(field).getCode()).isEqualTo(errorCode);
	}

	private static Stream<Arguments> invalidProperties() {
		return Stream.of(
				Arguments.of(
						new AwsAppConfigPropertiesBuilder().withAccountId("").build(),
						"accountId", "NotEmpty"),
				Arguments.of(
						new AwsAppConfigPropertiesBuilder().withApplication("").build(),
						"application", "NotEmpty"),
				Arguments.of(new AwsAppConfigPropertiesBuilder()
						.withConfigurationProfile("").build(), "configurationProfile",
						"NotEmpty"),
				Arguments.of(
						new AwsAppConfigPropertiesBuilder().withEnvironment("").build(),
						"environment", "NotEmpty"));
	}

	@Test
	void validationSucceeds() {
		AwsAppConfigProperties properties = new AwsAppConfigPropertiesBuilder()
				.withAccountId("12345678").withApplication("my-project")
				.withConfigurationProfile("my-app").withEnvironment("dev").build();

		Errors errors = new BeanPropertyBindingResult(properties, "properties");
		properties.validate(properties, errors);

		assertThat(errors.getAllErrors()).isEmpty();
	}

	private static class AwsAppConfigPropertiesBuilder {

		private AwsAppConfigProperties properties = new AwsAppConfigProperties();

		AwsAppConfigPropertiesBuilder withAccountId(String accountId) {
			properties.setAccountId(accountId);
			return this;
		}

		AwsAppConfigPropertiesBuilder withApplication(String application) {
			properties.setApplication(application);
			return this;
		}

		AwsAppConfigPropertiesBuilder withConfigurationProfile(
				String configurationProfile) {
			properties.setConfigurationProfile(configurationProfile);
			return this;
		}

		AwsAppConfigPropertiesBuilder withEnvironment(String environment) {
			properties.setEnvironment(environment);
			return this;
		}

		AwsAppConfigPropertiesBuilder withConfigurationVersion(String version) {
			properties.setConfigurationVersion(version);
			return this;
		}

		AwsAppConfigPropertiesBuilder withFailFast(boolean failFast) {
			properties.setFailFast(failFast);
			return this;
		}

		AwsAppConfigPropertiesBuilder withRegion(String region) {
			properties.setRegion(region);
			return this;
		}

		private AwsAppConfigProperties build() {
			return properties;
		}

	}

}
