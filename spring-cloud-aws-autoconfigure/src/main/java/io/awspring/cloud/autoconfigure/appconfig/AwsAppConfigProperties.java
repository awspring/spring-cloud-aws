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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * @author jarpz
 */
@ConfigurationProperties(prefix = "spring.cloud.aws.appconfig")
public class AwsAppConfigProperties implements Validator {

	private String clientId;

	private String application;

	private String environment;

	private String configurationProfile;

	private String configurationVersion;

	private String region;

	private boolean failFast;

	@Override
	public boolean supports(Class<?> clazz) {
		return AwsAppConfigProperties.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		AwsAppConfigProperties properties = (AwsAppConfigProperties) target;

		if (StringUtils.isEmpty(properties.getApplication())) {
			errors.rejectValue("application", "NotEmpty", "application should not be empty or null.");
		}

		if (StringUtils.isEmpty(properties.getEnvironment())) {
			errors.rejectValue("environment", "NotEmpty", "environment should not be empty or null.");
		}
	}

	public String getClientId() {
		return clientId;
	}

	public String getApplication() {
		return application;
	}

	public String getEnvironment() {
		return environment;
	}

	public String getConfigurationProfile() {
		return configurationProfile;
	}

	public String getConfigurationVersion() {
		return configurationVersion;
	}

	public String getRegion() {
		return region;
	}

	public boolean isFailFast() {
		return failFast;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public void setApplication(String application) {
		this.application = application;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public void setConfigurationProfile(String configurationProfile) {
		this.configurationProfile = configurationProfile;
	}

	public void setConfigurationVersion(String configurationVersion) {
		this.configurationVersion = configurationVersion;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

}
