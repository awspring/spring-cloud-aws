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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * An entity with its attributes and parents, sent to Verified Permissions in the {@code entities} part of an
 * authorization request.
 * <p>
 * Attribute values may be {@link String}, {@link Boolean}, integral numbers, {@link java.math.BigDecimal} (Cedar
 * {@code decimal}), {@link java.net.InetAddress} (Cedar {@code ipaddr}), {@link java.time.Instant} and other temporal
 * types (Cedar {@code datetime}), {@link java.time.Duration} (Cedar {@code duration}), {@link EntityRef}, {@link Map}
 * (Cedar record), {@link java.util.Collection} or array (Cedar set), {@link ExtensionValue}, or an SDK
 * {@link software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue}. {@code null} values are left out.
 *
 * @param ref the entity identifier
 * @param attributes the entity attributes
 * @param parents the parent entities, for example the groups a user belongs to
 * @author Matej Nedic
 * @since 4.3.0
 */
public record Entity(EntityRef ref, Map<String, @Nullable Object> attributes, Set<EntityRef> parents) {

	public Entity {
		Assert.notNull(ref, "ref must not be null");
		attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
		parents = Collections.unmodifiableSet(new LinkedHashSet<>(parents));
	}

	public static Entity of(EntityRef ref) {
		return new Entity(ref, Map.of(), Set.of());
	}

	public static Entity of(EntityRef ref, Map<String, @Nullable Object> attributes) {
		return new Entity(ref, attributes, Set.of());
	}

	@Override
	public String toString() {
		// attribute values are deliberately left out: they may contain personal data
		return "Entity[" + ref + ", attributes=" + attributes.keySet() + ", parents=" + parents + "]";
	}
}
