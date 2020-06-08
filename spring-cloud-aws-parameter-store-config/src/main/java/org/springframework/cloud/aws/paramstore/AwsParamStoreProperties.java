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

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * Configuration properties for the AWS Parameter Store integration. Mostly based on the
 * Spring Cloud Consul Configuration equivalent.
 *
 * @author Joris Kuipers
 * @author Matej Nedic
 * @since 2.0.0
 */
@ConfigurationProperties(AwsParamStoreProperties.CONFIG_PREFIX)
public class AwsParamStoreProperties implements Validator {

	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "aws.paramstore";

	/**
	 * Pattern used for prefix validation.
	 */
	private static final Pattern PREFIX_PATTERN = Pattern
			.compile("(/[a-zA-Z0-9.\\-_]+)*");

	/**
	 * Pattern used for profileSeparator validation.
	 */
	private static final Pattern PROFILE_SEPARATOR_PATTERN = Pattern
			.compile("[a-zA-Z0-9.\\-_/]+");

	/**
	 * Prefix indicating first level for every property. Value must start with a forward
	 * slash followed by a valid path segment or be empty. Defaults to "/config".
	 */
	private String prefix = "/config";

	private String defaultContext = "application";

	private String profileSeparator = "_";

	/** Throw exceptions during config lookup if true, otherwise, log warnings. */
	private boolean failFast = true;

	/**
	 * Alternative to spring.application.name to use in looking up values in AWS Parameter
	 * Store.
	 */
	private String name;

	/** Is AWS Parameter Store support enabled. */
	private boolean enabled = true;

	@Override
	public boolean supports(Class clazz) {
		return AwsParamStoreProperties.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "prefix", "NotEmpty",
				"prefix should not be empty or null.");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "defaultContext", "NotEmpty",
				"defaultContext should not be empty or null.");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "profileSeparator", "NotEmpty",
				"profileSeparator should not be empty or null.");

		AwsParamStoreProperties awsParamStoreProperties = (AwsParamStoreProperties) target;

		if (!PREFIX_PATTERN.matcher(awsParamStoreProperties.getPrefix()).matches()) {
			errors.rejectValue("prefix", "Pattern",
					"The prefix must have pattern of:  " + PREFIX_PATTERN.toString());
		}
		if (!PROFILE_SEPARATOR_PATTERN
				.matcher(awsParamStoreProperties.getProfileSeparator()).matches()) {
			errors.rejectValue("profileSeparator", "Pattern",
					"The profileSeparator must have pattern of:  "
							+ PROFILE_SEPARATOR_PATTERN.toString());
		}
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getDefaultContext() {
		return defaultContext;
	}

	public void setDefaultContext(String defaultContext) {
		this.defaultContext = defaultContext;
	}

	public String getProfileSeparator() {
		return profileSeparator;
	}

	public void setProfileSeparator(String profileSeparator) {
		this.profileSeparator = profileSeparator;
	}

	public boolean isFailFast() {
		return failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
