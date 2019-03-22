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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;

/**
 * Annotation that enables caching based on the ElastiCache caching service. This
 * annotation is a meta-annotation of
 * {@link org.springframework.cache.annotation.EnableCaching} with the same configuration
 * attributes. In contrast to the
 * {@link org.springframework.cache.annotation.EnableCaching} annotation this annotation
 * does not require a {@link org.springframework.cache.annotation.CachingConfigurer}
 * instance, because it is provided by this annotation itself. Therefore users only need
 * to add this annotation into one configuration file and configure the necessary cache
 * names.
 *
 * If there is no
 * {@link org.springframework.cloud.aws.cache.config.annotation.EnableElastiCache#value()}
 * ()} attribute configured, then all caches inside a stack (if the application is running
 * in one stack) will be used.
 *
 * @author Agim Emruli
 */
@EnableCaching
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(ElastiCacheCachingConfiguration.class)
public @interface EnableElastiCache {

	/**
	 * Configures the cache clusters for the caching configuration. Support one or
	 * multiple caches
	 * {@link org.springframework.cloud.aws.cache.config.annotation.CacheClusterConfig}
	 * configurations with their physical cache name (as configured in the ElastiCache
	 * service) or their logical cache name if the caches are configured inside a stack
	 * and
	 * {@link org.springframework.cloud.aws.context.config.annotation.EnableStackConfiguration}
	 * annotation is used inside the application.
	 *
	 * The CacheClusterConfig annotation also configures cache specific attributes like
	 * the expiration time.
	 * @return - the configured cache instances for the application.
	 */
	CacheClusterConfig[] value() default {};

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed to
	 * standard Java interface-based proxies. The default is {@code false}. <strong>
	 * Applicable only if {@link #mode()} is set to {@link AdviceMode#PROXY}</strong>.
	 * <p>
	 * Note that setting this attribute to {@code true} will affect <em>all</em>
	 * Spring-managed beans requiring proxying, not just those marked with
	 * {@code @Cacheable}. For example, other beans marked with Spring's
	 * {@code @Transactional} annotation will be upgraded to subclass proxying at the same
	 * time. This approach has no negative impact in practice unless one is explicitly
	 * expecting one type of proxy vs another, e.g. in tests.
	 * @return whether proxy should be created around a class
	 */
	boolean proxyTargetClass() default false;

	/**
	 * Indicate how caching advice should be applied. The default is
	 * {@link AdviceMode#PROXY}.
	 *
	 * @see AdviceMode
	 * @return type of advice mode to choose
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * Configures the default expiration time in seconds if there is no custom expiration
	 * time configuration with a
	 * {@link org.springframework.cloud.aws.cache.config.annotation.CacheClusterConfig}
	 * configuration for the cache. The expiration time is implementation specific (e.g.
	 * Redis or Memcached) and could therefore differ in the behaviour based on the cache
	 * implementation.
	 * @return - the default expiration time for all caches that do not contain a specific
	 * expiration time on cache level
	 */
	int defaultExpiration() default 0;

}
