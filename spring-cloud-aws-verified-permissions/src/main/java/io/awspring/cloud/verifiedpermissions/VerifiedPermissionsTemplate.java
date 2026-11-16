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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.util.Assert;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedRequest;
import software.amazon.awssdk.services.verifiedpermissions.model.BatchIsAuthorizedWithTokenRequest;

/**
 * {@link VerifiedPermissionsOperations} backed by the blocking {@link VerifiedPermissionsClient}.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class VerifiedPermissionsTemplate implements VerifiedPermissionsOperations {

	private final VerifiedPermissionsClient client;

	private final VerifiedPermissionsRequestFactory requestFactory;

	public VerifiedPermissionsTemplate(VerifiedPermissionsClient client,
			VerifiedPermissionsRequestFactory requestFactory) {
		Assert.notNull(client, "client must not be null");
		Assert.notNull(requestFactory, "requestFactory must not be null");
		this.client = client;
		this.requestFactory = requestFactory;
	}

	@Override
	public AuthorizationOutcome authorize(PolicyStoreTarget target, AuthorizationRequest request) {
		Assert.notNull(request, "request must not be null");
		if (request.principal() instanceof PrincipalSource.EntityPrincipal) {
			return call("IsAuthorized",
					() -> requestFactory.outcome(client.isAuthorized(requestFactory.isAuthorized(target, request))));
		}
		return call("IsAuthorizedWithToken", () -> requestFactory
				.outcome(client.isAuthorizedWithToken(requestFactory.isAuthorizedWithToken(target, request))));
	}

	@Override
	public List<AuthorizationOutcome> authorizeAll(PolicyStoreTarget target, BatchAuthorizationRequest request) {
		Assert.notNull(request, "request must not be null");
		List<AuthorizationOutcome> outcomes = new ArrayList<>(request.items().size());
		if (request.principal() instanceof PrincipalSource.EntityPrincipal) {
			for (BatchIsAuthorizedRequest batch : requestFactory.batchIsAuthorized(target, request)) {
				outcomes.addAll(call("BatchIsAuthorized",
						() -> requestFactory.outcomes(batch, client.batchIsAuthorized(batch))));
			}
		}
		else {
			for (BatchIsAuthorizedWithTokenRequest batch : requestFactory.batchIsAuthorizedWithToken(target, request)) {
				outcomes.addAll(call("BatchIsAuthorizedWithToken",
						() -> requestFactory.outcomes(batch, client.batchIsAuthorizedWithToken(batch))));
			}
		}
		return outcomes;
	}

	private static <T> T call(String operation, Supplier<T> call) {
		try {
			return call.get();
		}
		catch (SdkException e) {
			throw new VerifiedPermissionsException("Verified Permissions " + operation + " call failed", e);
		}
	}
}
