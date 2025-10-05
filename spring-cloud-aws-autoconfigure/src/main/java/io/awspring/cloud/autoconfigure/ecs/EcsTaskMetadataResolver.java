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

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;
import org.springframework.web.client.RestClientException;

/**
 * Resolves ECS task metadata. Supports ECS task metadata endpoint version 4.
 * <p>
 * See <a href="https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-metadata-endpoint-v4.html">AWS
 * documentation</a> for more details.
 *
 * @author Jukka Palomäki
 * @since 3.5.0
 */
public class EcsTaskMetadataResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(EcsTaskMetadataResolver.class);
	private static final String ECS_CONTAINER_METADATA_URI_V4 = System.getenv("ECS_CONTAINER_METADATA_URI_V4");

	/** We expose a subset of available task metadata */
	private static final Set<String> EXPOSED_PROPERTIES = Set.of("Cluster", "ServiceName", "TaskARN", "Family",
			"Revision", "AvailabilityZone", "LaunchType");

	private final RestClient restClient;

	public EcsTaskMetadataResolver(Builder restClientBuilder) {
		this.restClient = restClientBuilder.baseUrl(ECS_CONTAINER_METADATA_URI_V4).build();
	}

	/**
	 * Returns ECS task metadata. If unable to fetch the metadata, an empty Map is returned.
	 */
	public Map<String, String> getEcsTaskMetadata() {
		Map<String, String> properties = new LinkedHashMap<>();
		tryPopulateTaskMetadata(properties);
		return properties;
	}

	@SuppressWarnings("unchecked")
	private void tryPopulateTaskMetadata(Map<String, String> properties) {
		try {
			LOGGER.debug("Getting ECS task metadata from {}/task", ECS_CONTAINER_METADATA_URI_V4);
			Map<String, Object> taskMetadata = restClient.get().uri("/task").accept(MediaType.APPLICATION_JSON)
					.retrieve().body(Map.class);
			if (taskMetadata != null) {
				for (String key : EXPOSED_PROPERTIES) {
					if (taskMetadata.containsKey(key)) {
						properties.put("ecs-task-metadata." + key, taskMetadata.get(key).toString());
					}
				}
			}
		} catch (IllegalArgumentException | RestClientException e) {
			LOGGER.error("Error getting ECS task metadata", e);
		}
	}
}
