/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.verifiedpermissions.security;

/**
 * How the principal is sent to Verified Permissions.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public enum TokenMode {

	/**
	 * The application describes the principal: {@code User::"<name>"} with its roles as parents.
	 */
	ENTITY,

	/**
	 * The raw OIDC ID token is sent and validated by the policy store's identity source. Its claims become principal
	 * attributes.
	 */
	IDENTITY_TOKEN,

	/**
	 * The raw OAuth2 access token (a JWT) is sent and validated by the policy store's identity source. Its claims are
	 * placed in {@code context.token}, not on the principal.
	 */
	ACCESS_TOKEN
}
