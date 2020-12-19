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

package org.springframework.cloud.aws.secretsmanager;

import java.net.URI;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Configuration properties for the AWS Secrets Manager integration. Mostly based on the
 * Spring Cloud Consul Configuration equivalent.
 *
 * @author Fabio Maia
 * @author Matej Nedic
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = AwsSecretsManagerProperties.CONFIG_PREFIX)
public class AwsSecretsManagerProperties implements Validator {

	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "aws.secretsmanager";

	/**
	 * Pattern used for prefix validation.
	 */
	private static final Pattern PREFIX_PATTERN = Pattern.compile("(/[a-zA-Z0-9.\\-_]+)*");

	/**
	 * Pattern used for profileSeparator validation.
	 */
	private static final Pattern PROFILE_SEPARATOR_PATTERN = Pattern.compile("[a-zA-Z0-9.\\-_/\\\\]+");

	/**
	 * Prefix indicating first level for every property. Value must start with a forward
	 * slash followed by a valid path segment or be empty. Defaults to "/config".
	 */
	private String prefix = "/secret";

	private String defaultContext = "application";

	private String profileSeparator = "_";

	/** Throw exceptions during config lookup if true, otherwise, log warnings. */
	private boolean failFast = true;

	/**
	 * If region value is not null or empty it will be used in creation of
	 * AWSSecretsManager.
	 */
	private String region;

	/**
	 * Overrides the default endpoint.
	 */
	private URI endpoint;

	/**
	 * Alternative to spring.application.name to use in looking up values in AWS Secrets
	 * Manager.
	 */
	private String name;

	/** Is AWS Secrets Manager support enabled. */
	private boolean enabled = true;

	@Override
	public boolean supports(Class<?> clazz) {
		return AwsSecretsManagerProperties.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		AwsSecretsManagerProperties properties = (AwsSecretsManagerProperties) target;

		if (!StringUtils.hasLength(properties.getPrefix())) {
			errors.rejectValue("prefix", "NotEmpty", "prefix should not be empty or null.");
		}

		if (!StringUtils.hasLength(properties.getDefaultContext())) {
			errors.rejectValue("defaultContext", "NotEmpty", "defaultContext should not be empty or null.");
		}

		if (!StringUtils.hasLength(properties.getProfileSeparator())) {
			errors.rejectValue("profileSeparator", "NotEmpty", "profileSeparator should not be empty or null.");
		}

		if (!PREFIX_PATTERN.matcher(properties.getPrefix()).matches()) {
			errors.rejectValue("prefix", "Pattern", "The prefix must have pattern of:  " + PREFIX_PATTERN.toString());
		}
		if (!PROFILE_SEPARATOR_PATTERN.matcher(properties.getProfileSeparator()).matches()) {
			errors.rejectValue("profileSeparator", "Pattern",
					"The profileSeparator must have pattern of:  " + PROFILE_SEPARATOR_PATTERN.toString());
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

	public String getRegion() {
		return region;
	}

	public void setRegion(final String region) {
		this.region = region;
	}

	public URI getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(URI endpoint) {
		this.endpoint = endpoint;
	}

}
