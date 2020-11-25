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

package org.springframework.cloud.aws.autoconfigure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cognito Authentication properties.
 *
 * @author Eddú Meléndez
 * @since 2.3
 */
@ConfigurationProperties(prefix = "spring.cloud.aws.security.cognito")
public class CognitoAuthenticationProperties {

	private static final String COGNITO_ISSUER = "https://cognito-idp.%s.amazonaws.com/%s";

	private static final String COGNITO_REGISTRY = "https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json";

	/**
	 *
	 */
	private String userPoolId;

	/**
	 *
	 */
	private String region;

	/**
	 * Encryption algorithm used to sign the JWK token.
	 */
	private String algorithm = "RS256";

	/**
	 * Non-dynamic audience string to validate.
	 */
	private String appClientId;

	public String getUserPoolId() {
		return this.userPoolId;
	}

	public void setUserPoolId(String userPoolId) {
		this.userPoolId = userPoolId;
	}

	public String getRegion() {
		return this.region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getRegistry() {
		return String.format(COGNITO_REGISTRY, this.region, this.userPoolId);
	}

	public String getAlgorithm() {
		return this.algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public String getIssuer() {
		return String.format(COGNITO_ISSUER, this.region, this.userPoolId);
	}

	public String getAppClientId() {
		return appClientId;
	}

	public void setAppClientId(String appClientId) {
		this.appClientId = appClientId;
	}

}
