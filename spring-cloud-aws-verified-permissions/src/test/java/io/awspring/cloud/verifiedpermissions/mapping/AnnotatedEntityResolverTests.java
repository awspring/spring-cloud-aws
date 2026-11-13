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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.awspring.cloud.verifiedpermissions.Entity;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnnotatedEntityResolverTests {

	private final AnnotatedEntityResolver resolver = new AnnotatedEntityResolver();

	@Test
	void mapsAnnotatedRecord() {
		Entity entity = resolver.resolve(new Photo("42", "alice", true, "a1", List.of("t1", "t2"), "not sent"));

		assertThat(entity).isNotNull();
		assertThat(entity.ref()).isEqualTo(new EntityRef("Photo", "42"));
		assertThat(entity.attributes()).containsEntry("owner", new EntityRef("User", "alice"))
				.containsEntry("private", true).containsEntry("tags", List.of("t1", "t2"))
				.doesNotContainKey("internalNote");
		assertThat(entity.parents()).containsExactly(new EntityRef("Album", "a1"));
	}

	@Test
	void mapsAnnotatedClass() {
		Entity entity = resolver.resolve(new Album(7L, List.of("u1", "u2")));

		assertThat(entity.ref()).isEqualTo(new EntityRef("Album", "7"));
		assertThat(entity.attributes()).containsEntry("members",
				List.of(new EntityRef("User", "u1"), new EntityRef("User", "u2")));
	}

	@Test
	void ignoresUnannotatedTypes() {
		assertThat(resolver.resolve("plain string")).isNull();
	}

	@Test
	void rejectsNullId() {
		assertThatThrownBy(() -> resolver.resolve(new Photo(null, "a", false, null, List.of(), "")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("id");
	}

	@AvpEntity("Photo")
	record Photo(@AvpEntityId String id, @AvpAttribute(entityType = "User") String owner,
			@AvpAttribute("private") boolean isPrivate, @AvpParent("Album") String albumId,
			@AvpAttribute List<String> tags, String internalNote) {
	}

	@AvpEntity("Album")
	static class Album {

		@AvpEntityId
		private final Long id;

		@AvpAttribute(entityType = "User")
		private final List<String> members;

		Album(Long id, List<String> members) {
			this.id = id;
			this.members = members;
		}
	}
}
