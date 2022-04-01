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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.EnumerablePropertySource;
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
public class ParameterStorePropertySource extends EnumerablePropertySource<SsmClient> {

	// logger must stay static non-final so that it can be set with a value in
	// ParameterStoreConfigDataLoader
	private static Log LOG = LogFactory.getLog(ParameterStorePropertySource.class);

	private final String context;

	private final Map<String, Object> properties = new LinkedHashMap<>();

	public ParameterStorePropertySource(String context, SsmClient ssmClient) {
		super(context, ssmClient);
		this.context = context;
	}

	public void init() {
		GetParametersByPathRequest paramsRequest = GetParametersByPathRequest.builder().path(context).recursive(true)
				.withDecryption(true).build();
		getParameters(paramsRequest);
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = this.properties.keySet();
		return strings.toArray(new String[strings.size()]);
	}

	@Override
	@Nullable
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	private void getParameters(GetParametersByPathRequest paramsRequest) {
		GetParametersByPathResponse paramsResult = this.source.getParametersByPath(paramsRequest);
		for (Parameter parameter : paramsResult.parameters()) {
			String key = parameter.name().replace(this.context, "").replace('/', '.');
			LOG.debug("Populating property retrieved from AWS Parameter Store: " + key);
			this.properties.put(key, parameter.value());
		}
		if (paramsResult.nextToken() != null) {
			getParameters(paramsRequest.toBuilder().nextToken(paramsResult.nextToken()).build());
		}
	}

}
