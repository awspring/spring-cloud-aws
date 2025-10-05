/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.ecs;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.client.RestClient;

/**
 * Configuration for exposing ECS task metadata as properties in the Spring application context.
 *
 * @author Jukka Palomäki
 * @since 3.5.0
 */
@AutoConfiguration
@ConditionalOnProperty(name = "spring.cloud.aws.ecs-task-metadata.enabled", havingValue = "true", matchIfMissing = false)
@ConditionalOnExpression("environment['ECS_CONTAINER_METADATA_URI_V4'] != null and environment['ECS_CONTAINER_METADATA_URI_V4'].trim() != ''")
public class EcsTaskMetadataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public EcsTaskMetadataPropertySource ecsTaskMetadataPropertySource(ConfigurableEnvironment env,
			EcsTaskMetadataResolver ecsTaskMetadataResolver) {
		EcsTaskMetadataPropertySource propertySource = new EcsTaskMetadataPropertySource("EcsTaskMetadata",
				ecsTaskMetadataResolver);
		propertySource.init();
		env.getPropertySources().addFirst(propertySource);
		return propertySource;
	}

	@Bean
	@ConditionalOnMissingBean
	public EcsTaskMetadataResolver ecsTaskMetadataResolver(RestClient.Builder restClientBuilder) {
		return new EcsTaskMetadataResolver(restClientBuilder);
	}
}
