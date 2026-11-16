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

import io.awspring.cloud.verifiedpermissions.AuthorizationOutcome;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Simple bounded in-memory decision cache with per-entry expiry for tests and small applications. Reads remove their
 * expired entry. At capacity, expired entries are swept at most once per second; if none expired, the new key is not
 * cached and existing keys can still be replaced. It performs no LRU or size-based eviction. Use the Caffeine strategy
 * for production load.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class ConcurrentDecisionCache implements DecisionCache {

	private static final Duration SWEEP_INTERVAL = Duration.ofSeconds(1);

	private final ConcurrentHashMap<DecisionCacheKey, Entry> entries = new ConcurrentHashMap<>();
	private final AtomicInteger size = new AtomicInteger();
	private final AtomicLong nextSweepMillis = new AtomicLong(Long.MIN_VALUE);
	private final int maxSize;
	private final Clock clock;

	public ConcurrentDecisionCache(int maxSize) {
		this(maxSize, Clock.systemUTC());
	}

	ConcurrentDecisionCache(int maxSize, Clock clock) {
		Assert.isTrue(maxSize > 0, "maxSize must be positive");
		this.maxSize = maxSize;
		this.clock = clock;
	}

	@Override
	@Nullable
	public AuthorizationOutcome get(DecisionCacheKey key) {
		Entry entry = entries.get(key);
		if (entry != null && !entry.expiresAt().isAfter(clock.instant())) {
			if (entries.remove(key, entry)) {
				size.decrementAndGet();
			}
			return null;
		}
		return entry == null ? null : entry.outcome();
	}

	@Override
	public void put(DecisionCacheKey key, AuthorizationOutcome outcome, Instant expiresAt) {
		if (!expiresAt.isAfter(clock.instant())) {
			return;
		}
		Entry replacement = new Entry(outcome, expiresAt);
		for (;;) {
			Entry existing = entries.get(key);
			if (existing != null) {
				if (entries.replace(key, existing, replacement)) {
					return;
				}
				continue;
			}
			int current = size.get();
			if (current >= maxSize) {
				if (sweepExpired() == 0) {
					return;
				}
				continue;
			}
			if (!size.compareAndSet(current, current + 1)) {
				continue;
			}
			if (entries.putIfAbsent(key, replacement) == null) {
				return;
			}
			size.decrementAndGet();
		}
	}

	/**
	 * Removes every expired entry, but only when the cache is full and at most once per {@link #SWEEP_INTERVAL}, so the
	 * scan cost is bounded in time rather than paid by every insert. Returns the number of entries removed.
	 */
	private int sweepExpired() {
		Instant now = clock.instant();
		long nowMillis = now.toEpochMilli();
		long next = nextSweepMillis.get();
		if (nowMillis < next || !nextSweepMillis.compareAndSet(next, nowMillis + SWEEP_INTERVAL.toMillis())) {
			return 0;
		}
		int removed = 0;
		for (Map.Entry<DecisionCacheKey, Entry> candidate : entries.entrySet()) {
			if (!candidate.getValue().expiresAt().isAfter(now)
					&& entries.remove(candidate.getKey(), candidate.getValue())) {
				size.decrementAndGet();
				removed++;
			}
		}
		return removed;
	}

	@Override
	public void evictPartition(String cachePartition) {
		entries.forEach((key, entry) -> {
			if (key.cachePartition().equals(cachePartition) && entries.remove(key, entry)) {
				size.decrementAndGet();
			}
		});
	}

	private record Entry(AuthorizationOutcome outcome, Instant expiresAt) {
	}
}
