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
package io.awspring.cloud.autoconfigure.verifiedpermissions;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.awspring.cloud.verifiedpermissions.AuthorizationOutcome;
import io.awspring.cloud.verifiedpermissions.cache.DecisionCache;
import io.awspring.cloud.verifiedpermissions.cache.DecisionCacheKey;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

final class CaffeineDecisionCache implements DecisionCache {

	private final Cache<DecisionCacheKey, Entry> cache;

	CaffeineDecisionCache(long maxSize) {
		this.cache = Caffeine.newBuilder().maximumSize(maxSize)
				.expireAfter(Expiry.writing((DecisionCacheKey key, Entry entry) -> remaining(entry))).build();
	}

	@Override
	@Nullable
	public AuthorizationOutcome get(DecisionCacheKey key) {
		Entry entry = cache.getIfPresent(key);
		if (entry == null || !entry.expiresAt().isAfter(Instant.now())) {
			return null;
		}
		return entry.outcome();
	}

	private static Duration remaining(Entry entry) {
		Duration remaining = Duration.between(Instant.now(), entry.expiresAt());
		return remaining.isNegative() ? Duration.ZERO : remaining;
	}

	@Override
	public void put(DecisionCacheKey key, AuthorizationOutcome outcome, Instant expiresAt) {
		if (expiresAt.isAfter(Instant.now())) {
			cache.put(key, new Entry(outcome, expiresAt));
		}
	}

	@Override
	public void evictPartition(String cachePartition) {
		cache.asMap().keySet().removeIf(key -> key.cachePartition().equals(cachePartition));
	}

	private record Entry(AuthorizationOutcome outcome, Instant expiresAt) {
	}
}
