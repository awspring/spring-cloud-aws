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

package org.springframework.cloud.aws.paramstore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathResult;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;

import org.springframework.core.env.EnumerablePropertySource;

/**
 * Recursively retrieves all parameters under the given context / path with decryption
 * from the AWS Parameter Store using the provided SSM client.
 *
 * @author Joris Kuipers
 * @since 2.0.0
 */
public class AwsParamStorePropertySource
		extends EnumerablePropertySource<AWSSimpleSystemsManagement> {

	private String context;

	private Map<String, Object> properties = new LinkedHashMap<>();

	public AwsParamStorePropertySource(String context,
			AWSSimpleSystemsManagement ssmClient) {
		super(context, ssmClient);
		this.context = context;
	}

	public void init() {
		GetParametersByPathRequest paramsRequest = new GetParametersByPathRequest()
				.withPath(context).withRecursive(true).withWithDecryption(true);
		getParameters(paramsRequest);
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = properties.keySet();
		return strings.toArray(new String[strings.size()]);
	}

	@Override
	public Object getProperty(String name) {
		return properties.get(name);
	}

	private void getParameters(GetParametersByPathRequest paramsRequest) {
		GetParametersByPathResult paramsResult = source
				.getParametersByPath(paramsRequest);
		for (Parameter parameter : paramsResult.getParameters()) {
			String key = parameter.getName().replace(context, "").replace('/', '.');
			properties.put(key, parameter.getValue());
		}
		if (paramsResult.getNextToken() != null) {
			getParameters(paramsRequest.withNextToken(paramsResult.getNextToken()));
		}
	}

}
