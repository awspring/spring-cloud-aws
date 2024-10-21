/*
 * Copyright 2013-2024 the original author or authors.
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
package io.awspring.cloud.autoconfigure.cognito;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AWS Cognito Integration
 *
 * @author Oleh Onufryk
 * @since 3.3.0
 */

@ConfigurationProperties(CognitoProperties.CONFIG_PREFIX)
public class CognitoProperties extends AwsClientProperties {

	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.cloud.aws.cognito";

	/**
	 * Enables Cognito integration.
	 */
	private boolean enabled = true;

	/**
	 * The user pool ID.
	 */
	private String userPoolId;

	/**
	 * The client ID.
	 */
	private String clientId;

	/**
	 * The client secret.
	 */
	private String clientSecret;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getUserPoolId() {
		return userPoolId;
	}

	public void setUserPoolId(String userPoolId) {
		this.userPoolId = userPoolId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
}
