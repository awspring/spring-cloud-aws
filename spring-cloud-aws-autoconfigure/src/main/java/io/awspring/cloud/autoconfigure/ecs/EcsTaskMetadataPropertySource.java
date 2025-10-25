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

import io.awspring.cloud.core.config.AwsPropertySource;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * Exposes ECS task metadata as Spring properties.
 *
 * @author Jukka Palomäki
 * @since 3.5.0
 */
public class EcsTaskMetadataPropertySource
		extends AwsPropertySource<EcsTaskMetadataPropertySource, EcsTaskMetadataResolver> {

	private final String context;

	private final EcsTaskMetadataResolver ecsTaskMetadataResolver;

	private final Map<String, String> properties = new LinkedHashMap<>();

	public EcsTaskMetadataPropertySource(String context, EcsTaskMetadataResolver ecsTaskMetadataResolver) {
		super(context, ecsTaskMetadataResolver);
		this.context = context;
		this.ecsTaskMetadataResolver = ecsTaskMetadataResolver;
	}

	@Override
	public EcsTaskMetadataPropertySource copy() {
		return new EcsTaskMetadataPropertySource(context, source);
	}

	@Override
	public void init() {
		properties.putAll(ecsTaskMetadataResolver.getEcsTaskMetadata());
	}

	@Override
	public String[] getPropertyNames() {
		return properties.keySet().toArray(String[]::new);
	}

	@Override
	@Nullable
	public Object getProperty(String name) {
		return properties.get(name);
	}
}
