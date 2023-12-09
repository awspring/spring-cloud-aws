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
package io.awspring.cloud.autoconfigure.imds;

import io.awspring.cloud.core.config.AwsPropertySource;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * Adds properties from the EC2 Instance MetaData Service (IDMS) when it is available.
 *
 * @author Ken Krueger
 * @since 3.1.0
 */
public class ImdsPropertySource extends AwsPropertySource<ImdsPropertySource, ImdsUtils> {

	private final String context;

	private final ImdsUtils imdsUtils;

	private final Map<String, String> properties = new LinkedHashMap<>();

	public ImdsPropertySource(String context, ImdsUtils imdsUtils) {
		super(context, imdsUtils);
		this.context = context;
		this.imdsUtils = imdsUtils;
	}

	@Override
	public ImdsPropertySource copy() {
		return new ImdsPropertySource(context, source);
	}

	@Override
	public void init() {
		if (!imdsUtils.isRunningOnCloudEnvironment())
			return;
		properties.putAll(imdsUtils.getEc2InstanceMetadata());
	}

	@Override
	public String[] getPropertyNames() {
		return properties.keySet().stream().toArray(String[]::new);
	}

	@Override
	@Nullable
	public Object getProperty(String name) {
		return properties.get(name);
	}

}
