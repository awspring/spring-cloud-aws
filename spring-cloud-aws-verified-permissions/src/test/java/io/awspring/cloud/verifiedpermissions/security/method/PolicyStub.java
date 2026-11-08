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
package io.awspring.cloud.verifiedpermissions.security.method;

import io.awspring.cloud.verifiedpermissions.AuthorizationOutcome;
import io.awspring.cloud.verifiedpermissions.AuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.VerifiedPermissionsOperations;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory policy store for tests: allows exactly the {@code "<action> <resource>"} pairs it was given, for example
 * {@code "ViewPhoto Photo::\"1\""}.
 */
public class PolicyStub {

	private final Set<String> allowed;

	final List<AuthorizationRequest> requests = new CopyOnWriteArrayList<>();

	final AtomicInteger batchCalls = new AtomicInteger();

	public PolicyStub(String... allowed) {
		this.allowed = Set.of(allowed);
	}

	AuthorizationOutcome decide(String action, Object resource) {
		return allowed.contains(action + " " + resource) ? AuthorizationOutcome.allow("policy-1")
				: AuthorizationOutcome.deny();
	}

	VerifiedPermissionsOperations blocking() {
		return new VerifiedPermissionsOperations() {
			@Override
			public AuthorizationOutcome authorize(PolicyStoreTarget target, AuthorizationRequest request) {
				requests.add(request);
				return decide(request.action(), request.resource());
			}

			@Override
			public List<AuthorizationOutcome> authorizeAll(PolicyStoreTarget target,
					BatchAuthorizationRequest request) {
				batchCalls.incrementAndGet();
				return request.items().stream().map(item -> decide(item.action(), item.resource())).toList();
			}
		};
	}
}
