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

package org.elasticspring.messaging.support.destination;

import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class CachingDestinationResolver<P> implements DestinationResolver<P> {

	private final ConcurrentHashMap<String, P> destinationCache = new ConcurrentHashMap<String, P>();
	private final DestinationResolver<P> delegate;

	public CachingDestinationResolver(DestinationResolver<P> delegate) {
		this.delegate = delegate;
	}

	@Override
	public P resolveDestination(String name) throws DestinationResolutionException {
		if (this.destinationCache.containsKey(name)) {
			return this.destinationCache.get(name);
		}

		P result = this.delegate.resolveDestination(name);
		this.destinationCache.putIfAbsent(name, result);
		return result;
	}
}