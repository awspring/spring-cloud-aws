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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * One action on one resource in a multi-query authorization.
 *
 * @param action Cedar action id
 * @param resource an {@code EntityRef}, an {@code Entity}, or an object an entity resolver can map
 * @param context annotation or caller context for this query
 * @param returnObject the method return value the query belongs to, if any
 * @author Matej Nedic
 * @since 4.3.0
 */
public record AuthorizationQuery(String action, Object resource, Map<String, ?> context,
		@Nullable Object returnObject) {

	public AuthorizationQuery {
		Assert.hasText(action, "action must not be empty");
		Assert.notNull(resource, "resource must not be null");
		context = Collections.unmodifiableMap(new LinkedHashMap<>(context));
	}
}
