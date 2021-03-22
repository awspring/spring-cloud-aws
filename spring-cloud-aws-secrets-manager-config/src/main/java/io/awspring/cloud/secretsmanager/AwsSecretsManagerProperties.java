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

package io.awspring.cloud.secretsmanager;

import java.net.URI;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for the AWS Secrets Manager integration. Mostly based on the
 * Spring Cloud Consul Configuration equivalent.
 *
 * @author Fabio Maia
 * @author Matej Nedic
 * @author Hari Ohm Prasath
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = AwsSecretsManagerProperties.CONFIG_PREFIX)
public class AwsSecretsManagerProperties {

	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "aws.secretsmanager";

	/**
	 * Pattern used for prefix validation.
	 */
	private static final Pattern PREFIX_PATTERN = Pattern.compile("(/)?([a-zA-Z0-9.\\-_]+)*");

	/**
	 * Pattern used for profileSeparator validation.
	 */
	private static final Pattern PROFILE_SEPARATOR_PATTERN = Pattern.compile("[a-zA-Z0-9.\\-_/\\\\]+");

	/**
	 * Prefix indicating first level for every property. Value must start with a forward
	 * slash followed by a valid path segment or be empty. Defaults to "/secret".
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

	@PostConstruct
	public void validate() {

		if (!StringUtils.hasLength(prefix)) {
			throw new ValidationException("prefix", "prefix should not be empty or null.");
		}

		if (!StringUtils.hasLength(defaultContext)) {
			throw new ValidationException("defaultContext", "defaultContext should not be empty or null.");
		}

		if (!StringUtils.hasLength(profileSeparator)) {
			throw new ValidationException("profileSeparator", "profileSeparator should not be empty or null.");
		}

		if (!PREFIX_PATTERN.matcher(prefix).matches()) {
			throw new ValidationException("prefix", "The prefix must have pattern of:  " + PREFIX_PATTERN.toString());
		}
		if (!PROFILE_SEPARATOR_PATTERN.matcher(profileSeparator).matches()) {
			throw new ValidationException("profileSeparator",
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
