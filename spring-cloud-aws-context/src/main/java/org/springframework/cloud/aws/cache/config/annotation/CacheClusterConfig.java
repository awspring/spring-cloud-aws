/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.cache.config.annotation;

/**
 * Configuration annotation used by the
 * {@link org.springframework.cloud.aws.cache.config.annotation.EnableElastiCache}
 * annotation to configure cache specific attributes. This annotation allows to explicitly
 * to define the cache names and also to configure further cache specific attributes like
 * the expiration.
 *
 * @author Agim Emruli
 */
public @interface CacheClusterConfig {

	/**
	 * Defines the name for the cache cluster. The name might be the physical name of the
	 * cache cluster itself or a logical name of a cache cluster inside a stack. The
	 * caching infrastructure will automatically retrieve the cache engine (redis or
	 * memcached) and configured the appropriate cache driver with the cache
	 * implementation. Caches can be used inside the application code with the
	 * {@link org.springframework.cache.annotation.Cacheable} annotation or others
	 * referring to this name attribute inside the
	 * {@link org.springframework.cache.annotation.Cacheable#value()} attribute.
	 * @return - the name of the cache cluster
	 */
	String name();

	/**
	 * Configures the expiration time of the particular cache cluster in seconds. The
	 * expiration time is based on the cache level and is implementation specific.
	 * Typically this expiration time will configure the expiration of one item at the
	 * time the item is inserted into the cache regardless of the last access.
	 * @return the expiration with a default of 0. If this value is not explicitly
	 * configured then the value of {@link EnableElastiCache#defaultExpiration()} will be
	 * used.
	 */
	int expiration() default 0;

}
