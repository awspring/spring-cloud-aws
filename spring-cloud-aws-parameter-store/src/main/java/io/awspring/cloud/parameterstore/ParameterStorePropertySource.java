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
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
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
 * @since 2.0.0
 */
public class ParameterStorePropertySource extends AwsPropertySource<ParameterStorePropertySource, SsmClient> {

	// logger must stay static non-final so that it can be set with a value in
	// ParameterStoreConfigDataLoader
	private static Log LOG = LogFactory.getLog(ParameterStorePropertySource.class);
	private static final String PREFIX_PART = "?prefix=";
	private final String context;

	private final String parameterPath;

	/**
	 * Prefix that gets added to resolved property keys. Useful when same property keys are returned by multiple
	 * parameter paths. sources.
	 */
	@Nullable
	private final String prefix;

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
			String key = parameter.name().replace(this.parameterPath, "").replace('/', '.')
					.replaceAll("_(\\d)_", "[$1]");
			LOG.debug("Populating property retrieved from AWS Parameter Store: " + key);
			String propertyKey = prefix != null ? prefix + key : key;
			this.properties.put(propertyKey, parameter.value());
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
	private static String resolvePrefix(String context) {
		int prefixIndex = context.indexOf(PREFIX_PART);
		if (prefixIndex != -1) {
			return context.substring(prefixIndex + PREFIX_PART.length());
		}
		return null;
	}

	private static String resolveParameterPath(String context) {
		int prefixIndex = context.indexOf(PREFIX_PART);
		if (prefixIndex != -1) {
			return context.substring(0, prefixIndex);
		}
		return context;
	}

}
