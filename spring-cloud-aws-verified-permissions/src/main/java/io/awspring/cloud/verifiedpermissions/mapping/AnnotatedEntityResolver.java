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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EntityResolver} for classes annotated with {@link AvpEntity}. Field metadata is read once per class and
 * cached.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class AnnotatedEntityResolver implements EntityResolver {

	private final Map<Class<?>, Optional<Metadata>> metadataCache = new ConcurrentHashMap<>();

	@Override
	@Nullable
	public Entity resolve(Object domainObject) {
		Metadata metadata = metadataCache.computeIfAbsent(domainObject.getClass(), AnnotatedEntityResolver::metadata)
				.orElse(null);
		if (metadata == null) {
			return null;
		}
		Object id = read(metadata.idField(), domainObject);
		if (id == null) {
			throw new IllegalArgumentException("@AvpEntityId field '" + metadata.idField().getName() + "' of "
					+ domainObject.getClass().getName() + " is null");
		}
		Map<String, @Nullable Object> attributes = new LinkedHashMap<>();
		for (AttributeField attribute : metadata.attributes()) {
			Object value = read(attribute.field(), domainObject);
			attributes.put(attribute.name(),
					attribute.entityType() == null ? value : references(attribute.entityType(), value));
		}
		Set<EntityRef> parents = new LinkedHashSet<>();
		for (ParentField parent : metadata.parents()) {
			Object value = read(parent.field(), domainObject);
			if (value instanceof Collection<?> ids) {
				ids.stream().filter(java.util.Objects::nonNull)
						.forEach(element -> parents.add(EntityRef.of(parent.type(), element)));
			}
			else if (value != null) {
				parents.add(EntityRef.of(parent.type(), value));
			}
		}
		return new Entity(EntityRef.of(metadata.type(), id), attributes, parents);
	}

	@Nullable
	private static Object references(String type, @Nullable Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Collection<?> ids) {
			return ids.stream().filter(java.util.Objects::nonNull).map(id -> EntityRef.of(type, id)).toList();
		}
		return EntityRef.of(type, value);
	}

	@Nullable
	private static Object read(Field field, Object target) {
		return ReflectionUtils.getField(field, target);
	}

	private static Optional<Metadata> metadata(Class<?> type) {
		AvpEntity entity = AnnotatedElementUtils.findMergedAnnotation(type, AvpEntity.class);
		if (entity == null) {
			return Optional.empty();
		}
		List<Field> idFields = new ArrayList<>();
		List<AttributeField> attributes = new ArrayList<>();
		List<ParentField> parents = new ArrayList<>();
		ReflectionUtils.doWithFields(type, field -> {
			// record component annotations are propagated to the backing field
			if (field.isAnnotationPresent(AvpEntityId.class)) {
				idFields.add(field);
			}
			AvpAttribute attribute = field.getAnnotation(AvpAttribute.class);
			if (attribute != null) {
				ReflectionUtils.makeAccessible(field);
				attributes.add(new AttributeField(field,
						StringUtils.hasText(attribute.value()) ? attribute.value() : field.getName(),
						StringUtils.hasText(attribute.entityType()) ? attribute.entityType() : null));
			}
			AvpParent parent = field.getAnnotation(AvpParent.class);
			if (parent != null) {
				ReflectionUtils.makeAccessible(field);
				parents.add(new ParentField(field, parent.value()));
			}
		});
		if (idFields.size() != 1) {
			throw new IllegalStateException(type.getName() + " is annotated with @AvpEntity and needs exactly one"
					+ " @AvpEntityId field, found " + idFields.size());
		}
		ReflectionUtils.makeAccessible(idFields.get(0));
		return Optional.of(new Metadata(entity.value(), idFields.get(0), attributes, parents));
	}

	private record Metadata(String type, Field idField, List<AttributeField> attributes, List<ParentField> parents) {
	}

	private record AttributeField(Field field, String name, @Nullable String entityType) {
	}

	private record ParentField(Field field, String type) {
	}
}
