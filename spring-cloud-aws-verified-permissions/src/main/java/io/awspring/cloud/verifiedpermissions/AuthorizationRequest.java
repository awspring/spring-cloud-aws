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
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * A single authorization question: may the principal perform the action on the resource?
 *
 * @param principal where the principal comes from
 * @param action the action id without namespace, for example {@code ViewPhoto}
 * @param resource the resource
 * @param context the Cedar context
 * @param entities entities the policies need, for example the resource with its attributes
 * @author Matej Nedic
 * @since 4.3.0
 */
public record AuthorizationRequest(PrincipalSource principal, String action, EntityRef resource,
		Map<String, @Nullable Object> context, List<Entity> entities) {

	public AuthorizationRequest {
		Assert.notNull(principal, "principal must not be null");
		Assert.hasText(action, "action must not be empty");
		Assert.notNull(resource, "resource must not be null");
		context = Collections.unmodifiableMap(new LinkedHashMap<>(context));
		entities = List.copyOf(entities);
	}

	public static AuthorizationRequest of(PrincipalSource principal, String action, EntityRef resource) {
		return new AuthorizationRequest(principal, action, resource, Map.of(), List.of());
	}
}
