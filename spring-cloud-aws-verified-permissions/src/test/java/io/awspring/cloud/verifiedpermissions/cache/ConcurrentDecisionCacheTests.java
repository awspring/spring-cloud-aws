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

import io.awspring.cloud.verifiedpermissions.AuthorizationOutcome;
import io.awspring.cloud.verifiedpermissions.EntityRef;
import io.awspring.cloud.verifiedpermissions.security.TokenMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ConcurrentDecisionCacheTests {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-05T07:00:00Z"));

	@Test
	void removesExpiredEntryOnRead() {
		ConcurrentDecisionCache cache = new ConcurrentDecisionCache(1, clock);
		DecisionCacheKey expired = key("expired");
		DecisionCacheKey replacement = key("replacement");
		cache.put(expired, AuthorizationOutcome.allow("old"), clock.instant().plusSeconds(1));
		clock.advanceSeconds(1);

		assertThat(cache.get(expired)).isNull();
		cache.put(replacement, AuthorizationOutcome.allow("new"), clock.instant().plusSeconds(1));
		assertThat(cache.get(replacement)).isEqualTo(AuthorizationOutcome.allow("new"));
	}

	@Test
	void doesNotInsertNewKeyWhenMapIsFull() {
		ConcurrentDecisionCache cache = new ConcurrentDecisionCache(1, clock);
		DecisionCacheKey retained = key("retained");
		DecisionCacheKey rejected = key("rejected");
		cache.put(retained, AuthorizationOutcome.allow("first"), clock.instant().plusSeconds(10));

		cache.put(rejected, AuthorizationOutcome.allow("second"), clock.instant().plusSeconds(20));

		assertThat(cache.get(retained)).isEqualTo(AuthorizationOutcome.allow("first"));
		assertThat(cache.get(rejected)).isNull();
	}

	@Test
	void putAtCapacityReclaimsEntriesThatExpiredUnread() {
		ConcurrentDecisionCache cache = new ConcurrentDecisionCache(2, clock);
		DecisionCacheKey first = key("first");
		DecisionCacheKey second = key("second");
		DecisionCacheKey third = key("third");
		cache.put(first, AuthorizationOutcome.allow("first"), clock.instant().plusSeconds(1));
		cache.put(second, AuthorizationOutcome.allow("second"), clock.instant().plusSeconds(1));
		clock.advanceSeconds(1);

		cache.put(third, AuthorizationOutcome.allow("third"), clock.instant().plusSeconds(10));

		assertThat(cache.get(third)).isEqualTo(AuthorizationOutcome.allow("third"));
	}

	@Test
	void sweepsAtMostOncePerSecondWhileLiveEntriesFillTheMap() {
		ConcurrentDecisionCache cache = new ConcurrentDecisionCache(1, clock);
		DecisionCacheKey live = key("live");
		DecisionCacheKey shortLived = key("short");
		cache.put(live, AuthorizationOutcome.allow("live"), clock.instant().plusSeconds(60));
		cache.put(key("rejected"), AuthorizationOutcome.allow("rejected"), clock.instant().plusSeconds(60));
		cache.evictPartition("partition");
		cache.put(shortLived, AuthorizationOutcome.allow("short"), clock.instant().plusMillis(100));
		clock.advanceMillis(200);

		cache.put(key("blocked"), AuthorizationOutcome.allow("blocked"), clock.instant().plusSeconds(60));
		assertThat(cache.get(key("blocked"))).as("the previous sweep was less than a second ago").isNull();

		clock.advanceSeconds(1);
		cache.put(key("admitted"), AuthorizationOutcome.allow("admitted"), clock.instant().plusSeconds(60));
		assertThat(cache.get(key("admitted"))).isEqualTo(AuthorizationOutcome.allow("admitted"));
	}

	private static DecisionCacheKey key(String resourceId) {
		return new DecisionCacheKey("partition", "store", null, TokenMode.ENTITY, "principal", "ViewPhoto",
				EntityRef.of("Photo", resourceId), "context", "entities");
	}

	private static final class MutableClock extends Clock {
		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("UTC");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}

		private void advanceSeconds(long seconds) {
			instant = instant.plusSeconds(seconds);
		}

		private void advanceMillis(long millis) {
			instant = instant.plusMillis(millis);
		}
	}
}
