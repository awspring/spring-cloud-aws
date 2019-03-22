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

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the AWS Secrets Manager integration. Mostly based on the
 * Spring Cloud Consul Configuration equivalent.
 *
 * @author Fabio Maia
 * @since 2.0.0
 */
@ConfigurationProperties(AwsSecretsManagerProperties.CONFIG_PREFIX)
@Validated
public class AwsSecretsManagerProperties {

	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "aws.secretsmanager";

	/**
	 * Prefix indicating first level for every property. Value must start with a forward
	 * slash followed by a valid path segment or be empty. Defaults to "/config".
	 */
	@NotNull
	@Pattern(regexp = "(/[a-zA-Z0-9.\\-_]+)*")
	private String prefix = "/secret";

	@NotEmpty
	private String defaultContext = "application";

	@NotNull
	@Pattern(regexp = "[a-zA-Z0-9.\\-_]+")
	private String profileSeparator = "_";

	/** Throw exceptions during config lookup if true, otherwise, log warnings. */
	private boolean failFast = true;

	/**
	 * Alternative to spring.application.name to use in looking up values in AWS Secrets
	 * Manager.
	 */
	private String name;

	/** Is AWS Secrets Manager support enabled. */
	private boolean enabled = true;

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
