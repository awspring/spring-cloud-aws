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

import org.springframework.util.Assert;

/**
 * Identifies an entity in a Verified Permissions policy store, for example {@code Photo::"42"}.
 * <p>
 * The {@code type} is usually written without a namespace ({@code Photo}). The configured namespace is then added when
 * the request is sent. A type that already contains {@code ::} ({@code PhotoFlash::Photo}) is sent unchanged.
 *
 * @param type the entity type
 * @param id the entity id
 * @author Matej Nedic
 * @since 4.3.0
 */
public record EntityRef(String type, String id) {

	public EntityRef {
		Assert.hasText(type, "type must not be empty");
		Assert.notNull(id, "id must not be null");
	}

	public static EntityRef of(String type, Object id) {
		Assert.notNull(id, "id must not be null");
		return new EntityRef(type, String.valueOf(id));
	}

	@Override
	public String toString() {
		return type + "::\"" + id + "\"";
	}
}
