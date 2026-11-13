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

import io.awspring.cloud.verifiedpermissions.AuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.BatchAuthorizationRequest;
import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.PrincipalSource;
import io.awspring.cloud.verifiedpermissions.mapping.EntityGraph;
import io.awspring.cloud.verifiedpermissions.mapping.EntityGraphResolver;
import io.awspring.cloud.verifiedpermissions.mapping.EntityLoadContext;
import io.awspring.cloud.verifiedpermissions.mapping.EntityLoader;
import io.awspring.cloud.verifiedpermissions.mapping.EntityResolver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

/**
 * Resolves principals, resources, related entities, and trusted context before an authorization call.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
final class RequestAssembler {

	private final PrincipalResolver principalResolver;
	private final List<PrincipalEntityContributor> principalContributors;
	private final List<EntityResolver> entityResolvers;
	private final List<EntityLoader> entityLoaders;
	private final EntityGraphResolver graphResolver;
	private final List<ContextProvider> contextProviders;

	public RequestAssembler(PrincipalResolver principalResolver, List<PrincipalEntityContributor> principalContributors,
			List<EntityResolver> entityResolvers, List<EntityLoader> entityLoaders, EntityGraphResolver graphResolver,
			List<ContextProvider> contextProviders) {
		Assert.notNull(principalResolver, "principalResolver must not be null");
		this.principalResolver = principalResolver;
		this.principalContributors = List.copyOf(principalContributors);
		this.entityResolvers = List.copyOf(entityResolvers);
		this.entityLoaders = List.copyOf(entityLoaders);
		this.graphResolver = graphResolver;
		this.contextProviders = List.copyOf(contextProviders);
	}

	@Nullable
	public PrincipalSource principal(@Nullable Authentication authentication, TokenMode tokenMode,
			PolicyStoreTarget target, AuthorizationCall call) {
		PrincipalSource source = principalResolver.resolve(authentication, tokenMode);
		if (!(source instanceof PrincipalSource.EntityPrincipal entityPrincipal) || authentication == null) {
			return source;
		}
		Entity principal = entityPrincipal.principal();
		List<Entity> related = new ArrayList<>(entityPrincipal.relatedEntities());
		for (PrincipalEntityContributor contributor : principalContributors) {
			PrincipalContribution contribution = contributor.contribute(authentication, principal);
			Map<String, @Nullable Object> attributes = new LinkedHashMap<>(principal.attributes());
			attributes.putAll(contribution.attributes());
			Set<EntityRef> parents = new LinkedHashSet<>(principal.parents());
			parents.addAll(contribution.parents());
			principal = new Entity(principal.ref(), attributes, parents);
			related.addAll(contribution.relatedEntities());
		}
		List<Entity> roots = new ArrayList<>();
		roots.add(principal);
		roots.addAll(related);
		EntityGraph graph = graphResolver.resolve(roots, new EntityLoadContext(target, call));
		Entity resolvedPrincipal = graph.entities().getOrDefault(principal.ref(), principal);
		List<Entity> resolvedRelated = graph.entities().values().stream()
				.filter(entity -> !entity.ref().equals(resolvedPrincipal.ref())).toList();
		return new PrincipalSource.EntityPrincipal(resolvedPrincipal, resolvedRelated);
	}

	/**
	 * Maps a domain object to its {@link Entity} so that resolvers see the real type and id. {@link EntityRef} and
	 * {@link Entity} resources are returned unchanged.
	 */
	public Object seed(Object resource) {
		Assert.notNull(resource, "resource must not be null");
		return resource instanceof EntityRef || resource instanceof Entity ? resource : map(resource);
	}

	/**
	 * Returns the reference of a resource returned by {@link #seed(Object)}.
	 */
	public static EntityRef refOf(Object seeded) {
		return seeded instanceof Entity entity ? entity.ref() : (EntityRef) seeded;
	}

	public Resource resource(Object resource, PolicyStoreTarget target, AuthorizationCall call,
			RequestAuthorizationState state) {
		Assert.notNull(resource, "resource must not be null");
		if (resource instanceof Entity entity) {
			return expand(entity.ref(), entity, target, call);
		}
		EntityRef ref;
		if (resource instanceof EntityRef entityRef) {
			ref = entityRef;
		}
		else {
			Entity entity = map(resource);
			return expand(entity.ref(), entity, target, call);
		}
		EntityLoader loader = loaderFor(ref);
		if (loader == null) {
			return new Resource(ref, List.of(), true);
		}
		RequestEntityKey key = RequestEntityKey.of(target, ref, call.authentication());
		CompletableFuture<Optional<Entity>> cached = state.entities().get(key);
		if (cached == null) {
			load(loader, List.of(ref), new EntityLoadContext(target, call), state);
			cached = state.entities().get(key);
		}
		return join(cached).map(entity -> expand(ref, entity, target, call))
				.orElseGet(() -> new Resource(ref, List.of(), false));
	}

	/**
	 * Loads every {@link EntityRef} root that is not yet in the request state with one
	 * {@link EntityLoader#loadAll(Set, EntityLoadContext)} call per loader, policy store target and authentication. The
	 * {@link EntityLoadContext} of each batch lists every call it serves.
	 */
	public void preload(List<Root> roots, RequestAuthorizationState state) {
		Map<LoadGroup, List<EntityRef>> groups = new LinkedHashMap<>();
		Map<LoadGroup, List<AuthorizationCall>> calls = new LinkedHashMap<>();
		for (Root root : roots) {
			if (!(root.resource() instanceof EntityRef ref) || state.entities()
					.containsKey(RequestEntityKey.of(root.target(), ref, root.call().authentication()))) {
				continue;
			}
			EntityLoader loader = loaderFor(ref);
			if (loader != null) {
				LoadGroup group = new LoadGroup(loader, root.target(), root.call().authentication());
				groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(ref);
				calls.computeIfAbsent(group, ignored -> new ArrayList<>()).add(root.call());
			}
		}
		groups.forEach((group, refs) -> load(group.loader(), refs,
				new EntityLoadContext(group.target(), calls.get(group).get(0), calls.get(group)), state));
	}

	@Nullable
	private EntityLoader loaderFor(EntityRef ref) {
		return entityLoaders.stream().filter(candidate -> candidate.supports(ref)).findFirst().orElse(null);
	}

	private static void load(EntityLoader loader, List<EntityRef> refs, EntityLoadContext context,
			RequestAuthorizationState state) {
		Map<RequestEntityKey, EntityRef> owned = new LinkedHashMap<>();
		Map<RequestEntityKey, CompletableFuture<Optional<Entity>>> futures = new LinkedHashMap<>();
		for (EntityRef ref : refs) {
			RequestEntityKey key = RequestEntityKey.of(context.target(), ref, context.call().authentication());
			if (owned.containsKey(key)) {
				continue;
			}
			CompletableFuture<Optional<Entity>> future = new CompletableFuture<>();
			if (state.entities().putIfAbsent(key, future) == null) {
				owned.put(key, ref);
				futures.put(key, future);
			}
		}
		if (owned.isEmpty()) {
			return;
		}
		try {
			Set<EntityRef> requested = Set.copyOf(owned.values());
			Map<EntityRef, Entity> loaded = loader.loadAll(requested, context);
			loaded.forEach((ref, entity) -> {
				if (!requested.contains(ref) || !entity.ref().equals(ref)) {
					throw new IllegalStateException("EntityLoader returned " + entity.ref() + " for " + ref);
				}
			});
			owned.forEach((key, ref) -> futures.get(key).complete(Optional.ofNullable(loaded.get(ref))));
		}
		catch (RuntimeException | Error e) {
			futures.forEach((key, future) -> {
				state.entities().remove(key, future);
				future.completeExceptionally(e);
			});
			throw e;
		}
	}

	private static Optional<Entity> join(CompletableFuture<Optional<Entity>> future) {
		try {
			return future.join();
		}
		catch (CompletionException e) {
			throw e.getCause() instanceof RuntimeException runtime ? runtime : e;
		}
	}

	private Resource expand(EntityRef ref, Entity root, PolicyStoreTarget target, AuthorizationCall call) {
		EntityGraph graph = graphResolver.resolve(List.of(root), new EntityLoadContext(target, call));
		return new Resource(ref, List.copyOf(graph.entities().values()), true);
	}

	private Entity map(Object resource) {
		for (EntityResolver resolver : entityResolvers) {
			Entity entity = resolver.resolve(resource);
			if (entity != null) {
				return entity;
			}
		}
		throw new IllegalArgumentException("Cannot map " + resource.getClass().getName()
				+ " to a Verified Permissions entity. Annotate it with @AvpEntity, register an EntityResolver bean,"
				+ " or pass an EntityRef.");
	}

	public Map<String, @Nullable Object> context(AuthorizationCall call, Map<String, ?> annotationContext) {
		return context(call, annotationContext, null);
	}

	/**
	 * Merges trusted {@link ContextProvider} values with annotation context. The {@code request} is the current
	 * {@code HttpServletRequest}, if any, and is exposed through {@link ServletAuthorizationContext#request()}.
	 */
	public Map<String, @Nullable Object> context(AuthorizationCall call, Map<String, ?> annotationContext,
			@Nullable Object request) {
		Map<String, @Nullable Object> result = new LinkedHashMap<>();
		Set<String> protectedKeys = new LinkedHashSet<>();
		ServletAuthorizationContext source = new ServletAuthorizationContext(call.authentication(), call.action(),
				call.resource(), call.methodInvocation(), call.returnObject(), request);
		for (ContextProvider provider : contextProviders) {
			ContextContribution contribution = provider.context(source);
			for (Map.Entry<String, @Nullable Object> entry : contribution.values().entrySet()) {
				validateKey(entry.getKey());
				if (!protectedKeys.contains(entry.getKey())) {
					putOrRemove(result, entry.getKey(), entry.getValue());
				}
			}
			protectedKeys.addAll(contribution.protectedKeys());
		}
		for (Map.Entry<String, ?> entry : annotationContext.entrySet()) {
			validateKey(entry.getKey());
			if (protectedKeys.contains(entry.getKey())) {
				throw new IllegalArgumentException("Authorization context key '" + entry.getKey() + "' is protected");
			}
			putOrRemove(result, entry.getKey(), entry.getValue());
		}
		return result;
	}

	public AuthorizationRequest request(PrincipalSource principal, String action, Resource resource,
			Map<String, @Nullable Object> context) {
		return new AuthorizationRequest(principal, action, resource.ref(), context, resource.entities());
	}

	public BatchAuthorizationRequest sameResource(PrincipalSource principal, Resource resource,
			Collection<String> actions, Map<String, @Nullable Object> context) {
		List<BatchAuthorizationRequest.Item> items = actions.stream()
				.map(action -> new BatchAuthorizationRequest.Item(action, resource.ref(), context, resource.entities()))
				.toList();
		return new BatchAuthorizationRequest(principal, items, List.of());
	}

	public BatchAuthorizationRequest sameAction(PrincipalSource principal, String action, List<Resource> resources,
			Map<String, @Nullable Object> context) {
		List<BatchAuthorizationRequest.Item> items = resources.stream().map(
				resource -> new BatchAuthorizationRequest.Item(action, resource.ref(), context, resource.entities()))
				.toList();
		return new BatchAuthorizationRequest(principal, items, List.of());
	}

	private static void validateKey(String key) {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("Authorization context keys must be nonblank Strings");
		}
	}

	private static void putOrRemove(Map<String, @Nullable Object> target, String key, @Nullable Object value) {
		if (value == null) {
			target.remove(key);
		}
		else {
			target.put(key, value);
		}
	}

	public record Resource(EntityRef ref, List<Entity> entities, boolean found) {
		public Resource {
			entities = List.copyOf(entities);
		}
	}

	/**
	 * A resource to preload, with the policy store and call it is authorized for.
	 */
	public record Root(Object resource, PolicyStoreTarget target, AuthorizationCall call) {
	}

	private record LoadGroup(EntityLoader loader, PolicyStoreTarget target, @Nullable Authentication authentication) {
	}
}
