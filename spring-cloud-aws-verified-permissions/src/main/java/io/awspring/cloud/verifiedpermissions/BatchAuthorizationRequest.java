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
 * Several authorization questions for one principal.
 *
 * @param principal principal shared by all items
 * @param items authorization questions with item-local entity graphs
 * @param sharedEntities entities needed by every item
 * @author Matej Nedic
 * @since 4.3.0
 */
public record BatchAuthorizationRequest(PrincipalSource principal, List<Item> items, List<Entity> sharedEntities) {

	public BatchAuthorizationRequest {
		Assert.notNull(principal, "principal must not be null");
		items = List.copyOf(items);
		sharedEntities = List.copyOf(sharedEntities);
	}

	/**
	 * One question in a batch.
	 *
	 * @param action action id without namespace
	 * @param resource resource identifier
	 * @param context Cedar context
	 * @param entities entities needed by this item
	 */
	public record Item(String action, EntityRef resource, Map<String, @Nullable Object> context, List<Entity> entities) {
		public Item {
			Assert.hasText(action, "action must not be empty");
			Assert.notNull(resource, "resource must not be null");
			context = Collections.unmodifiableMap(new LinkedHashMap<>(context));
			entities = List.copyOf(entities);
		}

		public Item(String action, EntityRef resource, Map<String, @Nullable Object> context) {
			this(action, resource, context, List.of());
		}

		public static Item of(String action, EntityRef resource) {
			return new Item(action, resource, Map.of(), List.of());
		}
	}
}
