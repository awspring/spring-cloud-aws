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
 * The answer Verified Permissions gave to one authorization question.
 * <p>
 * {@link #allowed()} is authoritative. A {@code DENY} can come with {@link #errors()}, for example when a policy failed
 * to evaluate; those are diagnostics, not a different outcome.
 *
 * @param allowed whether the decision was {@code ALLOW}
 * @param determiningPolicies ids of the policies that produced the decision
 * @param errors evaluation errors reported by Verified Permissions
 * @author Matej Nedic
 * @since 4.3.0
 */
public record AuthorizationOutcome(boolean allowed, List<String> determiningPolicies, List<String> errors) {

	public AuthorizationOutcome {
		determiningPolicies = List.copyOf(determiningPolicies);
		errors = List.copyOf(errors);
	}

	public static AuthorizationOutcome allow(String... determiningPolicies) {
		return new AuthorizationOutcome(true, List.of(determiningPolicies), List.of());
	}

	public static AuthorizationOutcome deny(String... determiningPolicies) {
		return new AuthorizationOutcome(false, List.of(determiningPolicies), List.of());
	}
}
