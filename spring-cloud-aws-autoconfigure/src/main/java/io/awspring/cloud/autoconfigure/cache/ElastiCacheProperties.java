/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties related to ElastiCache configuration.
 *
 * @author Maciej Walkowiak
 */
@ConfigurationProperties(prefix = "cloud.aws.elasticache")
public class ElastiCacheProperties {

	/**
	 * Configures the cache clusters for the caching configuration. Support one or
	 * multiple caches {@link Cluster} configurations with their physical cache name (as
	 * configured in the ElastiCache service) or their logical cache name if the caches
	 * are configured inside a stack and
	 * {@link org.springframework.cloud.aws.context.config.annotation.EnableStackConfiguration}
	 * annotation is used inside the application.
	 */
	private List<Cluster> clusters = Collections.emptyList();

	/**
	 * Configures the default expiration time in seconds if there is no custom expiration
	 * time configuration with a {@link Cluster} configuration for the cache. The
	 * expiration time is implementation specific (e.g. Redis or Memcached) and could
	 * therefore differ in the behaviour based on the cache implementation.
	 */
	private int defaultExpiration;

	public List<Cluster> getClusters() {
		return clusters;
	}

	public void setClusters(List<Cluster> clusters) {
		this.clusters = clusters;
	}

	public int getDefaultExpiration() {
		return defaultExpiration;
	}

	public void setDefaultExpiration(int defaultExpiration) {
		this.defaultExpiration = defaultExpiration;
	}

	public List<String> getCacheNames() {
		return this.getClusters().stream().map(Cluster::getName).collect(Collectors.toList());
	}

	public Map<String, Integer> getExpiryTimePerCache() {
		Map<String, Integer> expiryTimePerCache = new HashMap<>(clusters.size());
		for (Cluster cluster : clusters) {
			expiryTimePerCache.put(cluster.getName(), cluster.getExpiration());
		}
		return expiryTimePerCache;
	}

	public static class Cluster {

		/**
		 * Defines the name for the cache cluster. The name might be the physical name of
		 * the cache cluster itself or a logical name of a cache cluster inside a stack.
		 * The caching infrastructure will automatically retrieve the cache engine (redis
		 * or memcached) and configured the appropriate cache driver with the cache
		 * implementation. Caches can be used inside the application code with the
		 * {@link org.springframework.cache.annotation.Cacheable} annotation or others
		 * referring to this name attribute inside the
		 * {@link org.springframework.cache.annotation.Cacheable#value()} attribute.
		 */
		private String name;

		/**
		 * Configures the expiration time of the particular cache cluster in seconds. The
		 * expiration time is based on the cache level and is implementation specific.
		 * Typically this expiration time will configure the expiration of one item at the
		 * time the item is inserted into the cache regardless of the last access. If this
		 * value is not explicitly configured then the value of
		 * {@link ElastiCacheProperties#getDefaultExpiration()} will be used.
		 */
		private int expiration;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getExpiration() {
			return expiration;
		}

		public void setExpiration(int expiration) {
			this.expiration = expiration;
		}

	}

}
