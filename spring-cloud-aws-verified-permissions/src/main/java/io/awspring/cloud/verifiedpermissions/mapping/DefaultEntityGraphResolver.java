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

import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.util.Assert;

/**
 * Batch-first {@link EntityGraphResolver} that follows related entities up to a maximum depth.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class DefaultEntityGraphResolver implements EntityGraphResolver {

	private final List<EntityLoader> loaders;
	private final int maxDepth;

	public DefaultEntityGraphResolver(List<EntityLoader> loaders, int maxDepth) {
		this.loaders = List.copyOf(loaders);
		Assert.isTrue(maxDepth >= 0, "maxDepth must not be negative");
		this.maxDepth = maxDepth;
	}

	@Override
	public EntityGraph resolve(Collection<Entity> roots, EntityLoadContext context) {
		PolicyStoreTarget target = context.target();
		Map<EntityRef, Entity> entities = new LinkedHashMap<>();
		roots.forEach(entity -> merge(entities, target, entity));
		Set<EntityRef> visited = new LinkedHashSet<>(entities.keySet());
		Set<EntityRef> unresolved = new LinkedHashSet<>();
		Set<EntityRef> frontier = references(entities.values());
		for (int depth = 0; depth < maxDepth && !frontier.isEmpty(); depth++) {
			frontier.removeIf(ref -> visited.contains(target.qualify(ref)));
			if (frontier.isEmpty()) {
				break;
			}
			frontier.forEach(ref -> visited.add(target.qualify(ref)));
			Map<EntityRef, Entity> loaded = load(frontier, context, unresolved);
			loaded.values().forEach(entity -> merge(entities, target, entity));
			frontier = references(loaded.values());
		}
		Set<EntityRef> remaining = references(entities.values());
		remaining.removeIf(ref -> entities.containsKey(target.qualify(ref)));
		unresolved.addAll(remaining);
		Map<EntityRef, Entity> result = new LinkedHashMap<>();
		entities.values().forEach(entity -> result.put(entity.ref(), entity));
		return new EntityGraph(result, unresolved);
	}

	private Map<EntityRef, Entity> load(Set<EntityRef> refs, EntityLoadContext context, Set<EntityRef> unresolved) {
		Map<EntityRef, Entity> loaded = new LinkedHashMap<>();
		Set<EntityRef> remaining = new LinkedHashSet<>(refs);
		for (EntityLoader loader : loaders) {
			Set<EntityRef> supported = new LinkedHashSet<>();
			for (EntityRef ref : remaining) {
				if (loader.supports(ref)) {
					supported.add(ref);
				}
			}
			if (!supported.isEmpty()) {
				Map<EntityRef, Entity> result = loader.loadAll(Set.copyOf(supported), context);
				result.forEach((ref, entity) -> {
					if (!ref.equals(entity.ref()) || !supported.contains(ref)) {
						throw new IllegalStateException("EntityLoader returned unexpected entity " + ref);
					}
					loaded.merge(ref, entity, DefaultEntityGraphResolver::mergeEntity);
				});
				for (EntityRef ref : supported) {
					if (!result.containsKey(ref)) {
						unresolved.add(ref);
					}
				}
				remaining.removeAll(supported);
			}
		}
		unresolved.addAll(remaining);
		return loaded;
	}

	private static Set<EntityRef> references(Collection<Entity> entities) {
		Set<EntityRef> result = new LinkedHashSet<>();
		for (Entity entity : entities) {
			result.addAll(entity.parents());
			entity.attributes().values().forEach(value -> collect(value, result));
		}
		return result;
	}

	private static void collect(Object value, Set<EntityRef> refs) {
		if (value instanceof EntityRef ref) {
			refs.add(ref);
		}
		else if (value instanceof Entity entity) {
			refs.add(entity.ref());
		}
		else if (value instanceof Optional<?> optional) {
			optional.ifPresent(element -> collect(element, refs));
		}
		else if (value instanceof Map<?, ?> map) {
			map.values().forEach(element -> collect(element, refs));
		}
		else if (value instanceof Iterable<?> iterable) {
			iterable.forEach(element -> collect(element, refs));
		}
		else if (value != null && value.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(value); i++) {
				collect(Array.get(value, i), refs);
			}
		}
	}

	private static void merge(Map<EntityRef, Entity> entities, PolicyStoreTarget target, Entity entity) {
		entities.merge(target.qualify(entity.ref()), entity, DefaultEntityGraphResolver::mergeEntity);
	}

	private static Entity mergeEntity(Entity first, Entity second) {
		Map<String, Object> attributes = new LinkedHashMap<>();
		second.attributes().forEach(attributes::put);
		first.attributes().forEach(attributes::put);
		Set<EntityRef> parents = new LinkedHashSet<>(first.parents());
		parents.addAll(second.parents());
		return new Entity(first.ref(), attributes, parents);
	}
}
