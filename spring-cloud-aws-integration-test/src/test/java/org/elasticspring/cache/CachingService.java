/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.cache;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Agim Emruli
 */
@Service
@CacheConfig(cacheNames = "CacheCluster")
public class CachingService {

	private final AtomicInteger invocationCount = new AtomicInteger(0);

	@Cacheable
	public String expensiveMethod(String key) {
		this.invocationCount.incrementAndGet();
		return key.toUpperCase();
	}

	public AtomicInteger getInvocationCount() {
		return this.invocationCount;
	}

	public void resetInvocationCount() {
		this.invocationCount.set(0);
	}
}