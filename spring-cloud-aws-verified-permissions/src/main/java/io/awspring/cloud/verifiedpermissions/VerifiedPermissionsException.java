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

/**
 * Thrown when a call to Verified Permissions fails, for example because of throttling, networking, IAM or a validation
 * error. Access is never granted in that case.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class VerifiedPermissionsException extends RuntimeException {

	public VerifiedPermissionsException(String message, Throwable cause) {
		super(message, cause);
	}
}
