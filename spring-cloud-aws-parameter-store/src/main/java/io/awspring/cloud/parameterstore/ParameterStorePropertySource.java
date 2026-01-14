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
package io.awspring.cloud.parameterstore;

import io.awspring.cloud.core.config.AwsPropertySource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

/**
 * Recursively retrieves all parameters under the given context / path with decryption from the AWS Parameter Store
 * using the provided SSM client.
 *
 * @author Joris Kuipers
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 * @author Matej Nedic
 * @since 2.0.0
 */
public class ParameterStorePropertySource extends AwsPropertySource<ParameterStorePropertySource, SsmClient> {

	// logger must stay static non-final so that it can be set with a value in
	// ParameterStoreConfigDataLoader
	private static Log LOG = LogFactory.getLog(ParameterStorePropertySource.class);
	private static final String PREFIX_PART = "?prefix=";

	private static final String PREFIX_PROPERTIES_LOAD = "?extension=";
	private final String context;

	private final String parameterPath;

	/**
	 * Prefix that gets added to resolved property keys. Useful when same property keys are returned by multiple
	 * parameter paths.
	 */
	@Nullable
	private final String prefix;

	private boolean propertiesType = false;

	@Nullable
	private String prefixType;

	private static final Set<String> ALLOWED_TYPES = Set.of("properties", "json", "yaml");

	private final Map<String, Object> properties = new LinkedHashMap<>();

	public ParameterStorePropertySource(String context, SsmClient ssmClient) {
		super("aws-parameterstore:" + context, ssmClient);
		this.context = context;
		this.parameterPath = resolveParameterPath(context);
		this.prefix = resolvePrefix(context);
	}

	@Override
	public void init() {
		GetParametersByPathRequest paramsRequest = GetParametersByPathRequest.builder().path(parameterPath)
				.recursive(true).withDecryption(true).build();
		getParameters(paramsRequest);
	}

	@Override
	public ParameterStorePropertySource copy() {
		return new ParameterStorePropertySource(context, source);
	}

	@Override
	public String[] getPropertyNames() {
		return this.properties.keySet().stream().toArray(String[]::new);
	}

	@Override
	@Nullable
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	private void getParameters(GetParametersByPathRequest paramsRequest) {
		GetParametersByPathResponse paramsResult = this.source.getParametersByPath(paramsRequest);
		for (Parameter parameter : paramsResult.parameters()) {
			if (propertiesType) {
				Properties props;
				if (prefixType.equals("properties")) {
					props = readProperties(parameter.value());
				}
				else {
					props = readYaml(parameter.value());
				}
				for (Map.Entry<Object, Object> entry : props.entrySet()) {
					properties.put(String.valueOf(entry.getKey()), entry.getValue());
				}
			}
			else {
				String key = parameter.name().replace(this.parameterPath, "").replace('/', '.').replaceAll("_(\\d)_",
						"[$1]");
				LOG.debug("Populating property retrieved from AWS Parameter Store: " + key);
				String propertyKey = prefix != null ? prefix + key : key;
				this.properties.put(propertyKey, parameter.value());
			}
		}
		if (paramsResult.nextToken() != null) {
			getParameters(paramsRequest.toBuilder().nextToken(paramsResult.nextToken()).build());
		}
	}

	@Nullable
	String getPrefix() {
		return prefix;
	}

	String getContext() {
		return context;
	}

	String getParameterPath() {
		return parameterPath;
	}

	@Nullable
	private String resolvePrefix(String context) {
		int prefixIndex = context.indexOf(PREFIX_PART);
		if (prefixIndex != -1) {
			return context.substring(prefixIndex + PREFIX_PART.length());
		}
		return null;
	}

	private String resolveParameterPath(String context) {
		int prefixIndex = context.indexOf(PREFIX_PART);
		if (prefixIndex != -1) {
			return context.substring(0, prefixIndex);
		}
		prefixIndex = context.indexOf(PREFIX_PROPERTIES_LOAD);
		if (prefixIndex != -1) {
			this.propertiesType = true;
			String extracted = context.substring(prefixIndex + PREFIX_PROPERTIES_LOAD.length()).toLowerCase();
			if (ALLOWED_TYPES.contains(extracted)) {
				this.prefixType = extracted;
			}
			else {
				throw new IllegalArgumentException(
						"Invalid prefixType: " + extracted + ". Must be one of properties, json, or yaml.");
			}
			return context.substring(0, prefixIndex);
		}
		return context;
	}

	private Properties readProperties(String input) {
		Properties properties = new Properties();

		try (InputStream in = new ByteArrayInputStream(input.getBytes())) {
			properties.load(in);
		}
		catch (Exception e) {
			throw new IllegalStateException("Cannot load environment", e);
		}
		return properties;
	}

	private Properties readYaml(String input) {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
		yaml.setResources(new ByteArrayResource(input.getBytes()));
		return yaml.getObject();
	}

}
