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
package io.awspring.cloud.verifiedpermissions.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.PolicyStoreTarget;
import io.awspring.cloud.verifiedpermissions.PrincipalSource;
import io.awspring.cloud.verifiedpermissions.security.TokenMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.verifiedpermissions.model.AttributeValue;

class DecisionCacheKeyTests {

	private static final PolicyStoreTarget TARGET = new PolicyStoreTarget("store", null);

	private static final PrincipalSource PRINCIPAL = PrincipalSource.entity(EntityRef.of("User", "alice"));

	@Test
	void sdkAttributeValuesWithRedactedToStringStillProduceDistinctKeys() {
		assertThat(key(Map.of("mfa", AttributeValue.fromBooleanValue(true))))
				.isNotEqualTo(key(Map.of("mfa", AttributeValue.fromBooleanValue(false))));
		assertThat(key(Map.of("level", AttributeValue.fromLongValue(1L))))
				.isNotEqualTo(key(Map.of("level", AttributeValue.fromLongValue(2L))));
		assertThat(key(Map.of("name", AttributeValue.fromString("a"))))
				.isNotEqualTo(key(Map.of("name", AttributeValue.fromString("b"))));
	}

	@Test
	void stringValuesCannotForgeExtraContextEntries() {
		Map<String, Object> forged = Map.of("a", "x,java.lang.String:b:java.lang.String:y");
		Map<String, Object> genuine = new LinkedHashMap<>();
		genuine.put("a", "x");
		genuine.put("b", "y");

		assertThat(key(forged)).isNotEqualTo(key(genuine));
	}

	@Test
	void stringValuesCannotForgeExtraEntityAttributes() {
		Entity forged = new Entity(EntityRef.of("Photo", "1"), Map.of("title", "t\u0000owner"), Set.of());
		Entity genuine = new Entity(EntityRef.of("Photo", "1"), Map.of("title", "t", "owner", ""), Set.of());

		assertThat(DecisionCacheKey.of(TARGET, TokenMode.ENTITY, PRINCIPAL, "View", EntityRef.of("Photo", "1"),
				Map.of(), List.of(forged)))
				.isNotEqualTo(DecisionCacheKey.of(TARGET, TokenMode.ENTITY, PRINCIPAL, "View",
						EntityRef.of("Photo", "1"), Map.of(), List.of(genuine)));
	}

	@Test
	void distinguishesTypesThatShareAToString() {
		assertThat(key(Map.of("v", "1"))).isNotEqualTo(key(Map.of("v", 1L)));
		assertThat(key(Map.of("v", "true"))).isNotEqualTo(key(Map.of("v", true)));
		assertThat(key(Map.of("v", List.of("a,b")))).isNotEqualTo(key(Map.of("v", List.of("a", "b"))));
	}

	@Test
	void equalValuesProduceEqualKeysRegardlessOfOrder() {
		Map<String, Object> first = new LinkedHashMap<>();
		first.put("a", 1);
		first.put("b", List.of("x", "y"));
		Map<String, Object> second = new LinkedHashMap<>();
		second.put("b", List.of("y", "x"));
		second.put("a", 1L);

		assertThat(key(first)).isEqualTo(key(second));
		assertThat(key(Map.of("mfa", true))).isEqualTo(key(Map.of("mfa", AttributeValue.fromBooleanValue(true))));
	}

	@Test
	void rejectsValuesThatCannotBeSentToVerifiedPermissions() {
		record Opaque(String secret) {
		}
		assertThatIllegalArgumentException().isThrownBy(() -> key(Map.of("v", new Opaque("s"))))
				.withMessageContaining("Opaque");
	}

	private static DecisionCacheKey key(Map<String, ?> context) {
		return DecisionCacheKey.of(TARGET, TokenMode.ENTITY, PRINCIPAL, "View", EntityRef.of("Photo", "1"), context,
				List.of());
	}
}
