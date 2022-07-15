/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.s3.crossregion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConcurrentLruMapTests {

	@Test
	void setsTheSizeLimit() {
		ConcurrentLruMap<String, String> map = new ConcurrentLruMap<>(3);
		assertThat(map.sizeLimit()).isEqualTo(3);
	}

	@Test
	void removesTheLeastAddedEntryIfNoEntryWasRetrieved() {
		ConcurrentLruMap<String, String> map = new ConcurrentLruMap<>(3);
		map.put("k1", "v1");
		map.put("k2", "v2");
		map.put("k3", "v3");
		map.put("k4", "v4");
		assertThat(map.queue()).containsExactly("k2", "k3", "k4");
		assertThat(map.size()).isEqualTo(3);
	}

	@Test
	void removesTheLeastUsedEntry() {
		ConcurrentLruMap<String, String> map = new ConcurrentLruMap<>(3);
		map.put("k1", "v1");
		map.put("k2", "v2");
		map.put("k3", "v3");
		map.get("k1");
		map.put("k4", "v4");
		assertThat(map.queue()).containsExactly("k3", "k1", "k4");
	}

	@Test
	void clearsAllEntries() {
		ConcurrentLruMap<String, String> map = new ConcurrentLruMap<>(3);
		map.put("k1", "v1");

		map.clear();
		assertThat(map.size()).isZero();
		assertThat(map.queue()).isEmpty();
		assertThat(map.cache()).isEmpty();
	}

	@Test
	void putsElementToMap() {
		ConcurrentLruMap<String, String> map = new ConcurrentLruMap<>(3);
		map.put("k1", "v1");
		assertThat(map.get("k1")).isEqualTo("v1");
		assertThat(map.contains("k1")).isTrue();
		assertThat(map.size()).isOne();
		assertThat(map.queue()).hasSize(1);
		assertThat(map.cache()).hasSize(1);
	}

	@Test
	void removesElementFromMap() {
		ConcurrentLruMap<String, String> map = new ConcurrentLruMap<>(3);
		map.put("k1", "v1");
		map.put("k2", "v2");

		assertThat(map.size()).isEqualTo(2);
		assertThat(map.queue()).hasSize(2);
		assertThat(map.cache()).hasSize(2);

		map.remove("k1");

		assertThat(map.get("k1")).isNull();
		assertThat(map.contains("k1")).isFalse();
		assertThat(map.size()).isOne();
		assertThat(map.queue()).hasSize(1);
		assertThat(map.cache()).hasSize(1);
	}

}
