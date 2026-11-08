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
import io.awspring.cloud.verifiedpermissions.AuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.PrincipalSource;
import io.awspring.cloud.verifiedpermissions.VerifiedPermissionsOperations;
import io.awspring.cloud.verifiedpermissions.cache.DecisionCache;
import io.awspring.cloud.verifiedpermissions.cache.DecisionCacheKey;
import io.awspring.cloud.verifiedpermissions.cache.NoOpDecisionCache;
import io.awspring.cloud.verifiedpermissions.mapping.DefaultEntityGraphResolver;
import io.awspring.cloud.verifiedpermissions.mapping.EntityGraphResolver;
import io.awspring.cloud.verifiedpermissions.mapping.EntityLoader;
import io.awspring.cloud.verifiedpermissions.mapping.EntityResolver;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.util.Assert;

/**
 * Blocking Spring Security authorization facade for Amazon Verified Permissions.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class VerifiedPermissionsAuthorizer {

	private final VerifiedPermissionsOperations operations;
	private final RequestAssembler assembler;
	private final PolicyStoreResolver policyStoreResolver;
	private final TokenModeResolver tokenModeResolver;
	private final DecisionCache decisionCache;
	private final RequestAuthorizationStateStore requestStateStore = new RequestAuthorizationStateStore();
	private final Duration cacheTtl;
	private final Duration tokenExpirySkew;
	private final boolean requestDeduplicationEnabled;
	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
			.getContextHolderStrategy();

	public VerifiedPermissionsAuthorizer(VerifiedPermissionsOperations operations, PrincipalResolver principalResolver,
			List<EntityResolver> entityResolvers) {
		this(operations, principalResolver, entityResolvers, List.of(), new DefaultEntityGraphResolver(List.of(), 2),
				List.of(), List.of(), call -> new PolicyStoreTarget("default", null), call -> TokenMode.ENTITY,
				new NoOpDecisionCache(), Duration.ofSeconds(10), Duration.ofSeconds(5), true);
	}

	public VerifiedPermissionsAuthorizer(VerifiedPermissionsOperations operations, PrincipalResolver principalResolver,
			List<EntityResolver> entityResolvers, List<EntityLoader> entityLoaders, EntityGraphResolver graphResolver,
			List<PrincipalEntityContributor> principalContributors, List<ContextProvider> contextProviders,
			PolicyStoreResolver policyStoreResolver, TokenModeResolver tokenModeResolver, DecisionCache decisionCache,
			Duration cacheTtl, Duration tokenExpirySkew, boolean requestDeduplicationEnabled) {
		Assert.notNull(operations, "operations must not be null");
		this.operations = operations;
		this.assembler = new RequestAssembler(principalResolver, principalContributors, entityResolvers, entityLoaders,
				graphResolver, contextProviders);
		this.policyStoreResolver = policyStoreResolver;
		this.tokenModeResolver = tokenModeResolver;
		this.decisionCache = decisionCache;
		this.cacheTtl = cacheTtl;
		this.tokenExpirySkew = tokenExpirySkew;
		this.requestDeduplicationEnabled = requestDeduplicationEnabled;
	}

	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy strategy) {
		Assert.notNull(strategy, "securityContextHolderStrategy must not be null");
		this.securityContextHolderStrategy = strategy;
	}

	public AvpAuthorizationResult authorize(String action, Object resource) {
		return authorize(currentAuthentication(), action, resource, Map.of());
	}

	public AvpAuthorizationResult authorize(String action, String resourceType, Object resourceId) {
		return authorize(currentAuthentication(), action, EntityRef.of(resourceType, resourceId), Map.of());
	}

	public AvpAuthorizationResult authorize(@Nullable Authentication authentication, String action, Object resource,
			Map<String, ?> context) {
		return authorize(authentication, action, resource, context, null, null, TokenModeOverride.DEFAULT);
	}

	public AvpAuthorizationResult authorize(@Nullable Authentication authentication, String action, Object resource,
			Map<String, ?> context, @Nullable MethodInvocation invocation, @Nullable Object returnObject,
			TokenModeOverride override) {
		return requestStateStore.within(
				() -> authorizeWithin(authentication, action, resource, context, invocation, returnObject, override));
	}

	private AvpAuthorizationResult authorizeWithin(@Nullable Authentication authentication, String action,
			Object resource, Map<String, ?> context, @Nullable MethodInvocation invocation,
			@Nullable Object returnObject, TokenModeOverride override) {
		Assert.hasText(action, "action must not be empty");
		Object seeded = assembler.seed(resource);
		AuthorizationCall unresolved = new AuthorizationCall(authentication, action, RequestAssembler.refOf(seeded),
				invocation, returnObject, override);
		PolicyStoreTarget target = policyStoreResolver.resolve(unresolved);
		TokenMode tokenMode = override.apply(tokenModeResolver.resolve(unresolved));
		RequestAuthorizationState state = requestDeduplicationEnabled ? requestStateStore.current()
				: new RequestAuthorizationState();
		RequestAssembler.Resource resolved = assembler.resource(seeded, target, unresolved, state);
		AuthorizationCall call = new AuthorizationCall(authentication, action, resolved.ref(), invocation, returnObject,
				override);
		if (!resolved.found()) {
			return AvpAuthorizationResult.denied(action, resolved.ref(), "resource not found");
		}
		PrincipalSource principal = assembler.principal(authentication, tokenMode, target, call);
		if (principal == null) {
			return AvpAuthorizationResult.unauthenticated(action, resolved.ref());
		}
		Map<String, @Nullable Object> mergedContext = assembler.context(call, context,
				requestStateStore.currentRequest());
		AuthorizationRequest request = assembler.request(principal, action, resolved, mergedContext);
		DecisionCacheKey key = DecisionCacheKey.of(target, tokenMode, principal, action, resolved.ref(), mergedContext,
				request.entities());
		AuthorizationOutcome outcome = cached(key, authentication, principal,
				() -> operations.authorize(target, request), state);
		return AvpAuthorizationResult.of(action, resolved.ref(), outcome);
	}

	public boolean isAuthorized(String action, Object resource) {
		return authorize(action, resource).isGranted();
	}

	public boolean isAuthorized(String action, String resourceType, Object resourceId) {
		return authorize(action, resourceType, resourceId).isGranted();
	}

	public Set<String> allowedActions(Object resource, String... actions) {
		return allowedActions(currentAuthentication(), resource, Arrays.asList(actions));
	}

	public Set<String> allowedActions(@Nullable Authentication authentication, Object resource,
			Collection<String> actions) {
		List<Query> queries = actions.stream()
				.map(action -> new Query(action, resource, Map.of(), null, null, TokenModeOverride.DEFAULT)).toList();
		List<AvpAuthorizationResult> results = authorizeBatch(authentication, queries);
		Set<String> allowed = new LinkedHashSet<>();
		int index = 0;
		for (String action : actions) {
			if (results.get(index++).isGranted()) {
				allowed.add(action);
			}
		}
		return allowed;
	}

	public <T> List<T> filterAllowed(Collection<? extends T> resources, String action) {
		return filterAllowed(currentAuthentication(), resources, action);
	}

	public <T> List<T> filterAllowed(@Nullable Authentication authentication, Collection<? extends T> resources,
			String action) {
		return filterAllowed(authentication, resources, action, ignored -> Map.of(), null, TokenModeOverride.DEFAULT);
	}

	public <T> List<T> filterAllowed(@Nullable Authentication authentication, Collection<? extends T> resources,
			String action, Function<? super T, Map<String, ?>> context, @Nullable MethodInvocation invocation,
			TokenModeOverride override) {
		List<Query> queries = resources.stream()
				.map(resource -> new Query(action, resource, context.apply(resource), resource, invocation, override))
				.toList();
		List<AvpAuthorizationResult> results = authorizeBatch(authentication, queries);
		List<T> allowed = new ArrayList<>();
		int index = 0;
		for (T resource : resources) {
			if (results.get(index++).isGranted()) {
				allowed.add(resource);
			}
		}
		return allowed;
	}

	/**
	 * Authorizes several queries for one principal with batched entity loading and batched Verified Permissions calls.
	 * Results are in query order.
	 */
	public List<AvpAuthorizationResult> authorizeAll(@Nullable Authentication authentication,
			List<AuthorizationQuery> queries, @Nullable MethodInvocation invocation, TokenModeOverride override) {
		return authorizeBatch(authentication, queries.stream().map(query -> new Query(query.action(), query.resource(),
				query.context(), query.returnObject(), invocation, override)).toList());
	}

	private List<AvpAuthorizationResult> authorizeBatch(@Nullable Authentication authentication, List<Query> queries) {
		return requestStateStore.within(() -> {
			RequestAuthorizationState state = requestDeduplicationEnabled ? requestStateStore.current()
					: new RequestAuthorizationState();
			List<Pending> pending = queries.stream().map(query -> pending(authentication, query)).toList();
			assembler.preload(pending.stream()
					.map(item -> new RequestAssembler.Root(item.resource(), item.target(), item.call())).toList(),
					state);
			Object request = requestStateStore.currentRequest();
			Map<PrincipalKey, Optional<PrincipalSource>> principals = new HashMap<>();
			List<Prepared> prepared = pending.stream()
					.map(item -> prepare(authentication, item, state, request, principals)).toList();
			Map<GroupKey, List<Prepared>> groups = new LinkedHashMap<>();
			for (Prepared item : prepared) {
				if (item.localResult() == null) {
					groups.computeIfAbsent(new GroupKey(item.target(), item.tokenMode(), item.principal()),
							ignored -> new ArrayList<>()).add(item);
				}
			}
			Map<DecisionCacheKey, AuthorizationOutcome> decisions = new LinkedHashMap<>();
			for (Map.Entry<GroupKey, List<Prepared>> entry : groups.entrySet()) {
				List<Prepared> misses = new ArrayList<>();
				for (Prepared item : entry.getValue()) {
					CompletableFuture<AuthorizationOutcome> requestCached = state.decisions().get(item.key());
					AuthorizationOutcome cached = requestCached == null ? decisionCache.get(item.key())
							: join(requestCached);
					if (cached != null) {
						decisions.put(item.key(), cached);
					}
					else if (!decisions.containsKey(item.key())
							&& misses.stream().noneMatch(candidate -> candidate.key().equals(item.key()))) {
						misses.add(item);
					}
				}
				if (!misses.isEmpty()) {
					List<BatchAuthorizationRequest.Item> requestItems = misses.stream()
							.map(item -> new BatchAuthorizationRequest.Item(item.query().action(),
									item.resource().ref(), item.context(), item.resource().entities()))
							.toList();
					List<AuthorizationOutcome> outcomes = operations.authorizeAll(entry.getKey().target(),
							new BatchAuthorizationRequest(entry.getKey().principal(), requestItems, List.of()));
					if (outcomes.size() != misses.size()) {
						throw new IllegalStateException("Verified Permissions returned " + outcomes.size()
								+ " outcomes for " + misses.size() + " authorization items");
					}
					for (int i = 0; i < misses.size(); i++) {
						Prepared item = misses.get(i);
						AuthorizationOutcome outcome = outcomes.get(i);
						decisions.put(item.key(), outcome);
						state.decisions().putIfAbsent(item.key(), CompletableFuture.completedFuture(outcome));
						Instant expiry = expiry(authentication, item.principal());
						if (outcome.errors().isEmpty() && expiry != null) {
							decisionCache.put(item.key(), outcome, expiry);
						}
					}
				}
			}
			return prepared.stream()
					.map(item -> item.localResult() != null ? item.localResult()
							: AvpAuthorizationResult.of(item.query().action(), item.resource().ref(),
									decisions.get(item.key())))
					.toList();
		});
	}

	private Pending pending(@Nullable Authentication authentication, Query query) {
		Object seeded = assembler.seed(query.resource());
		AuthorizationCall unresolved = new AuthorizationCall(authentication, query.action(),
				RequestAssembler.refOf(seeded), query.invocation(), query.returnObject(), query.override());
		PolicyStoreTarget target = policyStoreResolver.resolve(unresolved);
		TokenMode tokenMode = query.override().apply(tokenModeResolver.resolve(unresolved));
		return new Pending(query, seeded, unresolved, target, tokenMode);
	}

	private Prepared prepare(@Nullable Authentication authentication, Pending pending, RequestAuthorizationState state,
			@Nullable Object request, Map<PrincipalKey, Optional<PrincipalSource>> principals) {
		Query query = pending.query();
		PolicyStoreTarget target = pending.target();
		TokenMode tokenMode = pending.tokenMode();
		RequestAssembler.Resource resource = assembler.resource(pending.resource(), target, pending.call(), state);
		if (!resource.found()) {
			return new Prepared(query, target, tokenMode, PrincipalSource.entity(EntityRef.of("Unknown", "missing")),
					resource, Map.of(), null,
					AvpAuthorizationResult.denied(query.action(), resource.ref(), "resource not found"));
		}
		AuthorizationCall call = new AuthorizationCall(authentication, query.action(), resource.ref(),
				query.invocation(), query.returnObject(), query.override());
		PrincipalSource principal = principals
				.computeIfAbsent(new PrincipalKey(target, tokenMode),
						key -> Optional.ofNullable(assembler.principal(authentication, tokenMode, target, call)))
				.orElse(null);
		if (principal == null) {
			return new Prepared(query, target, tokenMode, PrincipalSource.entity(EntityRef.of("Unknown", "anonymous")),
					resource, Map.of(), null, AvpAuthorizationResult.unauthenticated(query.action(), resource.ref()));
		}
		Map<String, @Nullable Object> context = assembler.context(call, query.context(), request);
		DecisionCacheKey key = DecisionCacheKey.of(target, tokenMode, principal, query.action(), resource.ref(),
				context, resource.entities());
		return new Prepared(query, target, tokenMode, principal, resource, context, key, null);
	}

	private AuthorizationOutcome cached(DecisionCacheKey key, @Nullable Authentication authentication,
			PrincipalSource principal, Supplier<AuthorizationOutcome> invocation, RequestAuthorizationState state) {
		AuthorizationOutcome cached = decisionCache.get(key);
		if (cached != null) {
			return cached;
		}
		CompletableFuture<AuthorizationOutcome> created = new CompletableFuture<>();
		CompletableFuture<AuthorizationOutcome> existing = state.decisions().putIfAbsent(key, created);
		if (existing != null) {
			return join(existing);
		}
		try {
			AuthorizationOutcome outcome = invocation.get();
			created.complete(outcome);
			Instant expiry = expiry(authentication, principal);
			if (outcome.errors().isEmpty() && expiry != null) {
				decisionCache.put(key, outcome, expiry);
			}
			return outcome;
		}
		catch (RuntimeException | Error e) {
			created.completeExceptionally(e);
			state.decisions().remove(key, created);
			throw e;
		}
	}

	private static AuthorizationOutcome join(CompletableFuture<AuthorizationOutcome> future) {
		try {
			return future.join();
		}
		catch (CompletionException e) {
			throw e.getCause() instanceof RuntimeException runtime ? runtime : e;
		}
	}

	@Nullable
	private Instant expiry(@Nullable Authentication authentication, PrincipalSource principal) {
		Instant expiry = Instant.now().plus(cacheTtl);
		if (principal instanceof PrincipalSource.EntityPrincipal) {
			return expiry;
		}
		Instant tokenExpiresAt = DefaultPrincipalResolver.tokenExpiry(authentication, tokenValue(principal));
		if (tokenExpiresAt != null) {
			Instant tokenExpiry = tokenExpiresAt.minus(tokenExpirySkew);
			return tokenExpiry.isBefore(expiry) ? tokenExpiry : expiry;
		}
		return null;
	}

	private static String tokenValue(PrincipalSource principal) {
		if (principal instanceof PrincipalSource.IdentityToken token) {
			return token.token();
		}
		return ((PrincipalSource.AccessToken) principal).token();
	}

	private record Query(String action, Object resource, Map<String, ?> context, @Nullable Object returnObject,
			@Nullable MethodInvocation invocation, TokenModeOverride override) {
	}

	private record PrincipalKey(PolicyStoreTarget target, TokenMode tokenMode) {
	}

	private record Pending(Query query, Object resource, AuthorizationCall call, PolicyStoreTarget target,
			TokenMode tokenMode) {
	}

	private record GroupKey(PolicyStoreTarget target, TokenMode tokenMode, PrincipalSource principal) {
	}

	private record Prepared(Query query, PolicyStoreTarget target, TokenMode tokenMode, PrincipalSource principal,
			RequestAssembler.Resource resource, Map<String, @Nullable Object> context, @Nullable DecisionCacheKey key,
			@Nullable AvpAuthorizationResult localResult) {
	}

	@Nullable
	private Authentication currentAuthentication() {
		return securityContextHolderStrategy.getContext().getAuthentication();
	}
}
