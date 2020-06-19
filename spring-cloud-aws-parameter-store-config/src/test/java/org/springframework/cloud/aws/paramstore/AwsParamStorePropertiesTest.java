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

package org.springframework.cloud.aws.paramstore;

import javax.validation.Validation;

import org.junit.Test;

import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import static org.assertj.core.api.Assertions.assertThat;

public class AwsParamStorePropertiesTest {

	private SmartValidator validator = new SpringValidatorAdapter(
			Validation.buildDefaultValidatorFactory().getValidator());

	@Test
	public void acceptsForwardSlashAsProfileSeparator() {
		AwsParamStoreProperties properties = new AwsParamStoreProperties();
		properties.setProfileSeparator("/");

		Errors errors = new BeanPropertyBindingResult(properties, "properties");

		validator.validate(properties, errors);

		assertThat(errors.getFieldError("profileSeparator")).isNull();
	}

	@Test
	public void acceptsBackslashAsProfileSeparator() {
		AwsParamStoreProperties properties = new AwsParamStoreProperties();
		properties.setProfileSeparator("\\");

		Errors errors = new BeanPropertyBindingResult(properties, "properties");

		validator.validate(properties, errors);

		assertThat(errors.getFieldError("profileSeparator")).isNull();
	}

}
