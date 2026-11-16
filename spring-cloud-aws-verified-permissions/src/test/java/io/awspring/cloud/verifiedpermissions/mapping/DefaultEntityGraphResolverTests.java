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

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.security.AuthorizationCall;
import io.awspring.cloud.verifiedpermissions.security.TokenModeOverride;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultEntityGraphResolverTests {

	private final PolicyStoreTarget target = new PolicyStoreTarget("store", "PhotoFlash");
	private final AuthorizationCall call = new AuthorizationCall(null, "ViewPhoto", EntityRef.of("Photo", "1"), null,
			null, TokenModeOverride.DEFAULT);

	@Test
	void loadsRelatedEntitiesByDepth() {
		EntityLoader loader = new EntityLoader() {
			@Override
			public boolean supports(EntityRef ref) {
				return ref.type().equals("Album");
			}

			@Override
			public Map<EntityRef, Entity> loadAll(Set<EntityRef> refs, EntityLoadContext context) {
				EntityRef album = refs.iterator().next();
				return Map.of(album, new Entity(album, Map.of("owner", EntityRef.of("User", "alice")), Set.of()));
			}
		};
		Entity photo = new Entity(EntityRef.of("Photo", "1"), Map.of(), Set.of(EntityRef.of("Album", "a1")));

		EntityGraph graph = new DefaultEntityGraphResolver(java.util.List.of(loader), 2)
				.resolve(java.util.List.of(photo), new EntityLoadContext(target, call));

		assertThat(graph.entities()).containsKeys(photo.ref(), EntityRef.of("Album", "a1"));
		assertThat(graph.unresolvedReferences()).contains(EntityRef.of("User", "alice"));
	}

	@Test
	void treatsNamespacedAndPlainReferencesAsTheSameEntity() {
		java.util.List<EntityRef> requested = new java.util.ArrayList<>();
		EntityLoader loader = new EntityLoader() {
			@Override
			public boolean supports(EntityRef ref) {
				return true;
			}

			@Override
			public Map<EntityRef, Entity> loadAll(Set<EntityRef> refs, EntityLoadContext context) {
				requested.addAll(refs);
				EntityRef album = EntityRef.of("Album", "a1");
				return refs.contains(album)
						? Map.of(album,
								new Entity(album, Map.of("cover", EntityRef.of("PhotoFlash::Photo", "1")), Set.of()))
						: Map.of();
			}
		};
		Entity photo = new Entity(EntityRef.of("Photo", "1"), Map.of(), Set.of(EntityRef.of("Album", "a1")));

		EntityGraph graph = new DefaultEntityGraphResolver(java.util.List.of(loader), 3)
				.resolve(java.util.List.of(photo), new EntityLoadContext(target, call));

		assertThat(requested).containsExactly(EntityRef.of("Album", "a1"));
		assertThat(graph.entities()).containsOnlyKeys(photo.ref(), EntityRef.of("Album", "a1"));
		assertThat(graph.unresolvedReferences()).isEmpty();
	}
}
