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
import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.cache.DecisionCacheKey;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request-local entity and in-flight decision state.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
final class RequestAuthorizationState {
	private final ConcurrentHashMap<RequestEntityKey, CompletableFuture<Optional<Entity>>> entities = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<DecisionCacheKey, CompletableFuture<AuthorizationOutcome>> decisions = new ConcurrentHashMap<>();

	/**
	 * Loaded root entities, keyed by policy store target, qualified reference and authentication. A pending future is a
	 * load in flight that concurrent callers in the same request wait for.
	 */
	public ConcurrentHashMap<RequestEntityKey, CompletableFuture<Optional<Entity>>> entities() {
		return entities;
	}

	public ConcurrentHashMap<DecisionCacheKey, CompletableFuture<AuthorizationOutcome>> decisions() {
		return decisions;
	}
}
