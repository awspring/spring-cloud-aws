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

import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Additional attributes, parents, and related entities for an entity principal.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public record PrincipalContribution(Map<String, @Nullable Object> attributes, Set<EntityRef> parents,
		List<Entity> relatedEntities) {
	public PrincipalContribution {
		attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
		parents = Set.copyOf(parents);
		relatedEntities = List.copyOf(relatedEntities);
	}

	public static PrincipalContribution empty() {
		return new PrincipalContribution(Map.of(), Set.of(), List.of());
	}
}
