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

import io.awspring.cloud.verifiedpermissions.AuthorizationOutcome;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;

/**
 * A Spring Security {@link org.springframework.security.authorization.AuthorizationResult} carrying the Verified
 * Permissions diagnostics. It reaches {@code AuthorizationDeniedException#getAuthorizationResult()}, authorization
 * events and observations.
 * <p>
 * {@link #toString()} prints the action, resource and policy ids only, never entity attributes or tokens.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class AvpAuthorizationResult extends AuthorizationDecision {

	private static final long serialVersionUID = 1L;

	private final String action;

	private final EntityRef resource;

	private final List<String> determiningPolicies;

	private final List<String> errors;

	@Nullable
	private final String reason;

	AvpAuthorizationResult(boolean granted, String action, EntityRef resource, List<String> determiningPolicies,
			List<String> errors, @Nullable String reason) {
		super(granted);
		this.action = action;
		this.resource = resource;
		this.determiningPolicies = List.copyOf(determiningPolicies);
		this.errors = List.copyOf(errors);
		this.reason = reason;
	}

	public static AvpAuthorizationResult of(String action, EntityRef resource, AuthorizationOutcome outcome) {
		return new AvpAuthorizationResult(outcome.allowed(), action, resource, outcome.determiningPolicies(),
				outcome.errors(), null);
	}

	public static AvpAuthorizationResult unauthenticated(String action, EntityRef resource) {
		return denied(action, resource, "no authenticated principal");
	}

	public static AvpAuthorizationResult denied(String action, EntityRef resource, String reason) {
		return new AvpAuthorizationResult(false, action, resource, List.of(), List.of(), reason);
	}

	public String getAction() {
		return action;
	}

	public EntityRef getResource() {
		return resource;
	}

	/**
	 * @return ids of the policies that produced the decision
	 */
	public List<String> getDeterminingPolicies() {
		return determiningPolicies;
	}

	/**
	 * @return evaluation errors reported by Verified Permissions
	 */
	public List<String> getErrors() {
		return errors;
	}

	/**
	 * @return why the decision was made without calling Verified Permissions, or {@code null}
	 */
	@Nullable
	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		return "AvpAuthorizationResult[granted=" + isGranted() + ", action=" + action + ", resource=" + resource
				+ ", determiningPolicies=" + determiningPolicies + (errors.isEmpty() ? "" : ", errors=" + errors.size())
				+ (reason == null ? "" : ", reason=" + reason) + "]";
	}
}
