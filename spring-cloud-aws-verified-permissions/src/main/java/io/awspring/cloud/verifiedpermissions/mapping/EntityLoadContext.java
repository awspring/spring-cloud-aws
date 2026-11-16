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
package io.awspring.cloud.verifiedpermissions.mapping;

import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.security.AuthorizationCall;
import java.util.List;
import org.springframework.util.Assert;

/**
 * Context supplied to blocking entity loaders.
 * <p>
 * One {@link EntityLoader#loadAll(java.util.Set, EntityLoadContext)} call can serve several authorization calls, for
 * example every element of {@code filterAllowed}. {@link #calls()} lists them all in request order and {@link #call()}
 * is the first. They always share the policy store target, the authentication, the method invocation and the token
 * override; only action, resource and return object differ. A loaded root is reused for every later call in the same
 * request with the same target and authentication, so a loader must return the same entity whatever the action or
 * resource of the call. Put action-specific data in context instead.
 *
 * @param target the policy store target the entities are loaded for
 * @param call the first authorization call this load serves
 * @param calls every authorization call this load serves, starting with {@code call}
 * @author Matej Nedic
 * @since 4.3.0
 */
public record EntityLoadContext(PolicyStoreTarget target, AuthorizationCall call, List<AuthorizationCall> calls) {

	public EntityLoadContext {
		Assert.notNull(target, "target must not be null");
		Assert.notNull(call, "call must not be null");
		calls = List.copyOf(calls);
		Assert.isTrue(!calls.isEmpty() && calls.get(0).equals(call), "calls must start with call");
	}

	public EntityLoadContext(PolicyStoreTarget target, AuthorizationCall call) {
		this(target, call, List.of(call));
	}
}
