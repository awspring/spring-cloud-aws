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

import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Immutable destination and naming rules for one authorization call.
 *
 * @param policyStoreId policy store id or alias
 * @param namespace Cedar namespace, if any
 * @param tokenManagedEntityTypes entity types supplied by the selected identity source
 * @param cachePartition application-controlled cache partition or policy epoch
 * @author Matej Nedic
 * @since 4.3.0
 */
public record PolicyStoreTarget(String policyStoreId, @Nullable String namespace, Set<String> tokenManagedEntityTypes,
		String cachePartition) {

	public PolicyStoreTarget {
		Assert.hasText(policyStoreId, "policyStoreId must not be empty");
		namespace = StringUtils.hasText(namespace) ? namespace : null;
		tokenManagedEntityTypes = Set.copyOf(tokenManagedEntityTypes);
		Assert.hasText(cachePartition, "cachePartition must not be empty");
	}

	public PolicyStoreTarget(String policyStoreId, @Nullable String namespace) {
		this(policyStoreId, namespace, Set.of(), policyStoreId);
	}

	/**
	 * Returns the entity type prefixed with this target's namespace, unless it is already namespaced.
	 */
	public String qualifyType(String type) {
		return namespace == null || type.contains("::") ? type : namespace + "::" + type;
	}

	/**
	 * Returns the reference with its type qualified by {@link #qualifyType(String)}.
	 */
	public EntityRef qualify(EntityRef ref) {
		String type = qualifyType(ref.type());
		return type.equals(ref.type()) ? ref : new EntityRef(type, ref.id());
	}
}
