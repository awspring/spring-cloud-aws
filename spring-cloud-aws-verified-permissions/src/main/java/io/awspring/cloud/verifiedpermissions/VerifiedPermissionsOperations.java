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
package io.awspring.cloud.verifiedpermissions;

import java.util.List;

/**
 * Blocking operations against an Amazon Verified Permissions policy store.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public interface VerifiedPermissionsOperations {

	/**
	 * Answers one authorization question with {@code IsAuthorized}, or {@code IsAuthorizedWithToken} when the principal
	 * is a token.
	 * @throws VerifiedPermissionsException when the call fails
	 */
	AuthorizationOutcome authorize(PolicyStoreTarget target, AuthorizationRequest request);

	/**
	 * Answers several questions for one principal with {@code BatchIsAuthorized} or {@code BatchIsAuthorizedWithToken},
	 * 30 items per call.
	 * @return one outcome per item, in item order
	 * @throws VerifiedPermissionsException when a call fails
	 */
	List<AuthorizationOutcome> authorizeAll(PolicyStoreTarget target, BatchAuthorizationRequest request);
}
