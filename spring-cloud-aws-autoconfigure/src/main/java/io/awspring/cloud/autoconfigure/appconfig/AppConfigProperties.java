/*
 * Copyright 2013-2021 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * @author jarpz
 * @author Eddú Meléndez
 */
@ConfigurationProperties(prefix = "spring.cloud.aws.appconfig")
public class AppConfigProperties implements Validator {

	/**
	 * Application name or application id.
	 */
	private String application;

	/**
	 * Environment name or environment id.
	 */
	private String environment;

	/**
	 * Configuration name or configuration id.
	 */
	private String configurationProfile;

	/**
	 * Configuration version to receive.
	 */
	private String configurationVersion;

	/**
	 * Id to identify the client for the configurations.
	 */
	private String clientId;

	/**
	 * Overrides the default region.
	 */
	private String region;

	/**
	 * Throw exceptions during config lookup if true, otherwise, log warnings.
	 */
	private boolean failFast;

	@Override
	public boolean supports(Class<?> clazz) {
		return AppConfigProperties.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		AppConfigProperties properties = (AppConfigProperties) target;

		if (StringUtils.isEmpty(properties.getApplication())) {
			errors.rejectValue("application", "NotEmpty", "application should not be empty or null.");
		}

		if (StringUtils.isEmpty(properties.getEnvironment())) {
			errors.rejectValue("environment", "NotEmpty", "environment should not be empty or null.");
		}
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getConfigurationProfile() {
		return configurationProfile;
	}

	public void setConfigurationProfile(String configurationProfile) {
		this.configurationProfile = configurationProfile;
	}

	public String getConfigurationVersion() {
		return configurationVersion;
	}

	public void setConfigurationVersion(String configurationVersion) {
		this.configurationVersion = configurationVersion;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public boolean isFailFast() {
		return failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

}
