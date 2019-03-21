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

package org.springframework.cloud.aws.autoconfigure.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.aws.autoconfigure.context.ContextCredentialsAutoConfiguration;
import org.springframework.cloud.aws.cache.config.annotation.EnableElastiCache;
import org.springframework.cloud.aws.context.annotation.ConditionalOnAwsCloudEnvironment;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Agim Emruli
 */
@Configuration
@Import(ContextCredentialsAutoConfiguration.class)
@EnableElastiCache
@ConditionalOnClass(name = "com.amazonaws.services.elasticache.AmazonElastiCache")
@ConditionalOnAwsCloudEnvironment
public class ElastiCacheAutoConfiguration {

}
