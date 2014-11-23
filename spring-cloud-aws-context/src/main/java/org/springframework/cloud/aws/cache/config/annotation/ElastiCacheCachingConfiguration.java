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

package org.springframework.cloud.aws.cache.config.annotation;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cloud.aws.context.annotation.ConditionalOnMissingAmazonClient;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.StackResourceRegistryDetectingResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Agim Emruli
 */
@Configuration
public class ElastiCacheCachingConfiguration implements ImportAware {

	private AnnotationAttributes annotationAttributes;

	@Autowired(required = false)
	private RegionProvider regionProvider;

	@Autowired(required = false)
	private AWSCredentialsProvider credentialsProvider;

	@Autowired(required = false)
	private ListableStackResourceFactory stackResourceFactory;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.annotationAttributes = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableElastiCache.class.getName(), false));
		Assert.notNull(this.annotationAttributes,
				"@EnableElasticache is not present on importing class " + importMetadata.getClassName());
	}

	@Bean
	@ConditionalOnMissingAmazonClient(AmazonElastiCache.class)
	public AmazonElastiCache amazonElastiCache() {
		AmazonElastiCacheClient elastiCacheClient;
		if (this.credentialsProvider != null) {
			elastiCacheClient = new AmazonElastiCacheClient(this.credentialsProvider);
		} else {
			elastiCacheClient = new AmazonElastiCacheClient();
		}

		if (this.regionProvider != null) {
			elastiCacheClient.setRegion(this.regionProvider.getRegion());
		}
		return elastiCacheClient;
	}

	@Bean
	public CachingConfigurer cachingConfigurer(AmazonElastiCache amazonElastiCache, ResourceIdResolver resourceIdResolver) {
		if (this.annotationAttributes != null && this.annotationAttributes.getStringArray("clusters").length > 0) {
			return new ElastiCacheCacheConfigurer(amazonElastiCache, resourceIdResolver,
					Arrays.asList(this.annotationAttributes.getStringArray("clusters")));
		} else {
			return new ElastiCacheCacheConfigurer(amazonElastiCache, resourceIdResolver,
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

	//TODO: Check if there is a better place for this
	@Bean(name = "org.springframework.cloud.aws.core.env.ResourceIdResolver.BEAN_NAME")
	public ResourceIdResolver resourceIdResolver() {
		return new StackResourceRegistryDetectingResourceIdResolver();
	}
}
