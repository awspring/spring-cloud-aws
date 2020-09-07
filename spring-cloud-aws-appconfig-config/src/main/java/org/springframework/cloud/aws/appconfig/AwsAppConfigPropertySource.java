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

package org.springframework.cloud.aws.appconfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import com.amazonaws.services.appconfig.AmazonAppConfig;
import com.amazonaws.services.appconfig.model.GetConfigurationRequest;
import com.amazonaws.services.appconfig.model.GetConfigurationResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.ReflectionUtils;

/**
 * @author jarpz
 */
public class AwsAppConfigPropertySource
		extends EnumerablePropertySource<AmazonAppConfig> {

	private static final String SUPPORTED_TYPE_JSON = "application/json";

	private static final String SUPPORTED_TYPE_YAML = "application/x-yaml";

	private Properties properties;

	private final AwsAppConfigProperties configProperties;

	public AwsAppConfigPropertySource(String name, AmazonAppConfig appConfigClient,
			AwsAppConfigProperties configProperties) {
		super(name, appConfigClient);
		this.configProperties = configProperties;
	}

	public void init() {
		GetConfigurationRequest request = new GetConfigurationRequest()
				.withClientId(configProperties.getAccountId())
				.withApplication(configProperties.getApplication())
				.withConfiguration(name)
				.withClientConfigurationVersion(
						configProperties.getConfigurationVersion())
				.withEnvironment(configProperties.getEnvironment());

		getAppConfig(request);
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = properties.stringPropertyNames();
		return strings.toArray(new String[strings.size()]);
	}

	@Override
	public Object getProperty(String name) {
		return properties.get(name);
	}

	private void getAppConfig(GetConfigurationRequest request) {
		GetConfigurationResult configuration = this.source.getConfiguration(request);

		logger.trace(String.format("loading file: %s/%s/%s/%s",
				configProperties.getApplication(),
				configProperties.getConfigurationProfile(),
				configProperties.getEnvironment(),
				configuration.getConfigurationVersion()));

		switch (configuration.getContentType()) {
		case SUPPORTED_TYPE_YAML:
			processYamlContent(configuration.getContent());
			break;
		case SUPPORTED_TYPE_JSON:
			processJsonContent(configuration.getContent());
			break;
		default:
			throw new IllegalStateException(String.format("Unsupported content type: %s",
					configuration.getContentType()));
		}
	}

	private void processYamlContent(ByteBuffer byteBuffer) {

		YamlPropertiesFactoryBean bean = new YamlPropertiesFactoryBean();

		bean.setResources(new ByteArrayResource(byteBuffer.array()));

		properties = bean.getObject();
	}

	private void processJsonContent(ByteBuffer byteBuffer) {

		try {
			Map<String, Object> map = new ObjectMapper().readValue(byteBuffer.array(),
					new TypeReference<Map<String, Object>>() {
					});

			Map<String, Object> result = new LinkedHashMap<>();
			this.flatten(null, result, map);

			properties = new Properties();
			properties.putAll(result);
		}
		catch (IOException ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
		}
	}

	/**
	 * flatten json structure.
	 */
	private void flatten(String prefix, Map<String, Object> result,
			Map<String, Object> map) {
		String namePrefix = Objects.nonNull(prefix) ? prefix + "." : "";

		map.forEach((key, value) -> this.extract(namePrefix + key, result, value));
	}

	private void extract(String name, Map<String, Object> result, Object value) {
		if (value instanceof Map) {
			this.flatten(name, result, (Map) value);
		}
		else if (value instanceof Collection) {
			int index = 0;

			for (Iterator it = ((Collection) value).iterator(); it.hasNext(); ++index) {
				Object object = it.next();
				this.extract(name + "[" + index + "]", result, object);
			}
		}
		else {
			result.put(name, value);
		}
	}

}
