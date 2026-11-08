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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.verifiedpermissions.AuthorizationOutcome;
import io.awspring.cloud.verifiedpermissions.AuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.PrincipalSource;
import io.awspring.cloud.verifiedpermissions.VerifiedPermissionsOperations;
import io.awspring.cloud.verifiedpermissions.cache.ConcurrentDecisionCache;
import io.awspring.cloud.verifiedpermissions.mapping.DefaultEntityGraphResolver;
import io.awspring.cloud.verifiedpermissions.mapping.EntityLoadContext;
import io.awspring.cloud.verifiedpermissions.mapping.EntityLoader;
import io.awspring.cloud.verifiedpermissions.mapping.EntityResolver;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class VerifiedPermissionsAuthorizerTests {

	private final TestingAuthenticationToken alice = new TestingAuthenticationToken("alice", "pw", "ROLE_USER");

	@Test
	void cachesCompleteDecisionAcrossCallsWhenEnabled() {
		AtomicInteger calls = new AtomicInteger();
		VerifiedPermissionsOperations operations = operations(calls);
		VerifiedPermissionsAuthorizer authorizer = authorizer(operations, List.of(), List.of(),
				new ConcurrentDecisionCache(100));

		assertThat(authorizer.authorize(alice, "ViewPhoto", EntityRef.of("Photo", "1"), Map.of()).isGranted()).isTrue();
		assertThat(authorizer.authorize(alice, "ViewPhoto", EntityRef.of("Photo", "1"), Map.of()).isGranted()).isTrue();

		assertThat(calls).hasValue(1);
	}

	@Test
	void deniesMissingLoadedRootWithoutCallingService() {
		AtomicInteger calls = new AtomicInteger();
		EntityLoader loader = new EntityLoader() {
			@Override
			public boolean supports(EntityRef ref) {
				return ref.type().equals("Photo");
			}

			@Override
			public Map<EntityRef, Entity> loadAll(Set<EntityRef> refs, EntityLoadContext context) {
				return Map.of();
			}
		};
		VerifiedPermissionsAuthorizer authorizer = authorizer(operations(calls), List.of(loader), List.of(),
				new ConcurrentDecisionCache(100));

		AvpAuthorizationResult result = authorizer.authorize(alice, "ViewPhoto", EntityRef.of("Photo", "missing"),
				Map.of());

		assertThat(result.isGranted()).isFalse();
		assertThat(result.getReason()).isEqualTo("resource not found");
		assertThat(calls).hasValue(0);
	}

	@Test
	void protectsTrustedContextFromAnnotationOverride() {
		ContextProvider provider = source -> new ContextContribution(Map.of("tenant", "trusted"), Set.of("tenant"));
		VerifiedPermissionsAuthorizer authorizer = authorizer(operations(new AtomicInteger()), List.of(),
				List.of(provider), new ConcurrentDecisionCache(100));

		assertThatThrownBy(
				() -> authorizer.authorize(alice, "ViewPhoto", EntityRef.of("Photo", "1"), Map.of("tenant", "spoofed")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("protected");
	}

	@Test
	void loadsAllCollectionRootsWithOneLoaderCall() {
		AtomicInteger calls = new AtomicInteger();
		List<Set<EntityRef>> loads = new ArrayList<>();
		EntityLoader loader = new EntityLoader() {
			@Override
			public boolean supports(EntityRef ref) {
				return ref.type().equals("Photo") || ref.type().equals("Role");
			}

			@Override
			public Map<EntityRef, Entity> loadAll(Set<EntityRef> refs, EntityLoadContext context) {
				loads.add(refs);
				Map<EntityRef, Entity> result = new LinkedHashMap<>();
				refs.stream().filter(ref -> !ref.id().equals("missing"))
						.forEach(ref -> result.put(ref, new Entity(ref, Map.of("owner", "alice"), Set.of())));
				return result;
			}
		};
		VerifiedPermissionsAuthorizer authorizer = authorizer(operations(calls), List.of(loader), List.of(),
				new ConcurrentDecisionCache(100));
		List<EntityRef> photos = List.of(EntityRef.of("Photo", "1"), EntityRef.of("Photo", "2"),
				EntityRef.of("Photo", "missing"), EntityRef.of("Photo", "1"));

		List<EntityRef> allowed = authorizer.filterAllowed(alice, photos, "ViewPhoto");

		assertThat(loads).containsExactly(
				Set.of(EntityRef.of("Photo", "1"), EntityRef.of("Photo", "2"), EntityRef.of("Photo", "missing")),
				Set.of(EntityRef.of("Role", "USER")));
		assertThat(allowed).containsExactly(EntityRef.of("Photo", "1"), EntityRef.of("Photo", "2"),
				EntityRef.of("Photo", "1"));
		assertThat(calls).hasValue(1);
	}

	@Test
	void passesCurrentServletRequestToContextProviders() {
		List<Object> requests = new ArrayList<>();
		ContextProvider provider = source -> {
			requests.add(source.request());
			return new ContextContribution(Map.of(), Set.of());
		};
		VerifiedPermissionsAuthorizer authorizer = authorizer(operations(new AtomicInteger()), List.of(),
				List.of(provider), new ConcurrentDecisionCache(100));
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		try {
			authorizer.authorize(alice, "ViewPhoto", EntityRef.of("Photo", "1"), Map.of());
			authorizer.filterAllowed(alice, List.of(EntityRef.of("Photo", "2")), "ViewPhoto");
		}
		finally {
			RequestContextHolder.resetRequestAttributes();
		}
		authorizer.authorize(alice, "ViewPhoto", EntityRef.of("Photo", "3"), Map.of());

		assertThat(requests).hasSize(3);
		assertThat(requests.get(0)).isSameAs(request);
		assertThat(requests.get(1)).isSameAs(request);
		assertThat(requests.get(2)).isNull();
	}

	@Test
	void resolversSeeTheMappedEntityOfADomainObject() {
		List<EntityRef> seen = new ArrayList<>();
		PolicyStoreResolver storeResolver = call -> {
			seen.add(call.resource());
			return new PolicyStoreTarget("store", null);
		};
		List<EntityRef> modeSeen = new ArrayList<>();
		TokenModeResolver modeResolver = call -> {
			modeSeen.add(call.resource());
			return TokenMode.ENTITY;
		};
		EntityResolver photos = resource -> resource instanceof PhotoDto dto
				? new Entity(EntityRef.of("Photo", dto.id()), Map.of(), Set.of())
				: null;
		VerifiedPermissionsAuthorizer authorizer = new VerifiedPermissionsAuthorizer(operations(new AtomicInteger()),
				new DefaultPrincipalResolver(), List.of(photos), List.of(),
				new DefaultEntityGraphResolver(List.of(), 2), List.of(), List.of(), storeResolver, modeResolver,
				new ConcurrentDecisionCache(100), Duration.ofSeconds(10), Duration.ofSeconds(5), true);

		authorizer.authorize(alice, "ViewPhoto", new PhotoDto("tenant-a-1"), Map.of());
		authorizer.filterAllowed(alice, List.of(new PhotoDto("tenant-b-2")), "ViewPhoto");

		assertThat(seen).containsExactly(EntityRef.of("Photo", "tenant-a-1"), EntityRef.of("Photo", "tenant-b-2"));
		assertThat(modeSeen).isEqualTo(seen);
	}

	@Test
	void tokenPrincipalFromAPlainAuthenticationIsAuthorizedButNotCached() {
		AtomicInteger calls = new AtomicInteger();
		PrincipalResolver tokens = (authentication, mode) -> PrincipalSource.identityToken("header.payload.sig");
		VerifiedPermissionsAuthorizer authorizer = new VerifiedPermissionsAuthorizer(operations(calls), tokens,
				List.of(), List.of(), new DefaultEntityGraphResolver(List.of(), 2), List.of(), List.of(),
				call -> new PolicyStoreTarget("store", null), call -> TokenMode.IDENTITY_TOKEN,
				new ConcurrentDecisionCache(100), Duration.ofSeconds(10), Duration.ofSeconds(5), true);

		assertThat(authorizer.authorize(alice, "ViewPhoto", EntityRef.of("Photo", "1"), Map.of()).isGranted()).isTrue();
		assertThat(authorizer.authorize(alice, "ViewPhoto", EntityRef.of("Photo", "1"), Map.of()).isGranted()).isTrue();

		assertThat(calls).as("no trusted expiry, so the decision is not cached").hasValue(2);
	}

	private record PhotoDto(String id) {
	}

	private static VerifiedPermissionsAuthorizer authorizer(VerifiedPermissionsOperations operations,
			List<EntityLoader> loaders, List<ContextProvider> providers,
			io.awspring.cloud.verifiedpermissions.cache.DecisionCache cache) {
		return new VerifiedPermissionsAuthorizer(operations, new DefaultPrincipalResolver(), List.of(), loaders,
				new DefaultEntityGraphResolver(loaders, 2), List.of(), providers,
				call -> new PolicyStoreTarget("store", "PhotoFlash"), call -> TokenMode.ENTITY, cache,
				Duration.ofSeconds(10), Duration.ofSeconds(5), true);
	}

	private static VerifiedPermissionsOperations operations(AtomicInteger calls) {
		return new VerifiedPermissionsOperations() {
			@Override
			public AuthorizationOutcome authorize(PolicyStoreTarget target, AuthorizationRequest request) {
				calls.incrementAndGet();
				return AuthorizationOutcome.allow("policy");
			}

			@Override
			public List<AuthorizationOutcome> authorizeAll(PolicyStoreTarget target,
					BatchAuthorizationRequest request) {
				calls.incrementAndGet();
				return request.items().stream().map(item -> AuthorizationOutcome.allow("policy")).toList();
			}
		};
	}
}
