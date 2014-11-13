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

package org.springframework.cloud.aws.autoconfigure.cache;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.aws.autoconfigure.context.ContextCredentialsProviderConfiguration;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextCredentialsProviderConfiguration.class)
@EnableCaching
public class CacheConfiguration {

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired
	private Environment environment;

	@Autowired(required = false)
	private ListableStackResourceFactory stackResourceFactory;

	@Bean
	@ConditionalOnMissingBean(name = "amazonElastiCache")
	public AmazonElastiCache amazonElastiCache(AWSCredentialsProvider credentialsProvider) {
		AmazonElastiCacheClient elastiCacheClient = new AmazonElastiCacheClient(credentialsProvider);
		if (this.regionProvider != null) {
			elastiCacheClient.setRegion(this.regionProvider.getRegion());
		}
		return elastiCacheClient;
	}

	@Bean
	public CachingConfigurer cachingConfigurer(AmazonElastiCache amazonElastiCache, ResourceIdResolver resourceIdResolver) {
		//TODO check how to configure multiple caches
		if (StringUtils.hasText(this.environment.getProperty("cloud.aws.cache.name"))) {
			return new ElasticCacheConfigurer(amazonElastiCache, resourceIdResolver,
					Collections.singletonList(this.environment.getProperty("cloud.aws.cache.name")));
		} else {
			return new ElasticCacheConfigurer(amazonElastiCache, resourceIdResolver,
					getConfiguredCachesInStack());
		}
	}

	private List<String> getConfiguredCachesInStack() {
		List<String> cacheNames = new ArrayList<>();
		if (this.stackResourceFactory != null) {
			for (StackResource stackResource : this.stackResourceFactory.
					resourcesByType("AWS::ElastiCache::CacheCluster")) {
				cacheNames.add(stackResource.getPhysicalId());
			}
		}
		return cacheNames;
	}
}