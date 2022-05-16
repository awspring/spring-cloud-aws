/*
 * Copyright 2013-2022 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

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
	 * Url of the issuer.
	 */
	@Nullable
	private String issuerUrl;

	/**
	 * Url of the registry.
	 */
	@Nullable
	private String registryUrl;

	/**
	 * Id of the user pool.
	 */
	@Nullable
	private String userPoolId;

	/**
	 * Region of the user pool.
	 */
	@Nullable
	private String region;

	/**
	 * Encryption algorithm used to sign the JWK token.
	 */
	private String algorithm = "RS256";

	/**
	 * Non-dynamic audience string to validate.
	 */
	@Nullable
	private String appClientId;

	@Nullable
	public String getIssuerUrl() {
		return this.issuerUrl;
	}

	public void setIssuerUrl(String issuerUrl) {
		this.issuerUrl = issuerUrl;
	}

	@Nullable
	public String getRegistryUrl() {
		return this.registryUrl;
	}

	public void setRegistryUrl(String registryUrl) {
		this.registryUrl = registryUrl;
	}

	@Nullable
	public String getUserPoolId() {
		return this.userPoolId;
	}

	public void setUserPoolId(String userPoolId) {
		this.userPoolId = userPoolId;
	}

	@Nullable
	public String getRegion() {
		return this.region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	@Nullable
	public String getRegistry() {
		if (this.registryUrl == null) {
			return String.format(COGNITO_REGISTRY, this.region, this.userPoolId);
		}
		return this.registryUrl;
	}

	public String getAlgorithm() {
		return this.algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	@Nullable
	public String getIssuer() {
		if (this.issuerUrl == null) {
			return String.format(COGNITO_ISSUER, this.region, this.userPoolId);
		}
		return this.issuerUrl;
	}

	@Nullable
	public String getAppClientId() {
		return this.appClientId;
	}

	public void setAppClientId(String appClientId) {
		this.appClientId = appClientId;
	}

}
