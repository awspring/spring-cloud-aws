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

package io.awspring.cloud.secretsmanager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.lang.Nullable;

/**
 * Retrieves secret value under the given context / path from the AWS Secrets Manager
 * using the provided Secrets Manager client.
 *
 * @author Fabio Maia
 * @author Maciej Walkowiak
 * @since 2.0.0
 */
public class AwsSecretsManagerPropertySource extends EnumerablePropertySource<AWSSecretsManager> {

	private static Log LOG = LogFactory.getLog(AwsSecretsManagerPropertySource.class);

	private static final String PREFIX_PART = "?prefix=";

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private final String secretId;

	/**
	 * Prefix that gets added to resolved property keys. Useful same property keys are
	 * returned by multiple property sources.
	 */
	@Nullable
	private final String prefix;

	private final Map<String, Object> properties = new LinkedHashMap<>();

	public AwsSecretsManagerPropertySource(String context, AWSSecretsManager smClient) {
		super(context, smClient);
		this.secretId = resolveSecretId(context);
		this.prefix = resolvePrefix(context);
	}

	/**
	 * Loads properties from the Secrets Manager secret.
	 * @throws ResourceNotFoundException if specified secret does not exist in the
	 * database.
	 */
	public void init() {
		readSecretValue(new GetSecretValueRequest().withSecretId(secretId));
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

	private void readSecretValue(GetSecretValueRequest secretValueRequest) {
		try {
			GetSecretValueResult secretValueResult = source.getSecretValue(secretValueRequest);
			Map<String, Object> secretMap = jsonMapper.readValue(secretValueResult.getSecretString(),
					new TypeReference<Map<String, Object>>() {
					});

			for (Map.Entry<String, Object> secretEntry : secretMap.entrySet()) {
				LOG.debug("Populating property retrieved from AWS Secrets Manager: " + secretEntry.getKey());
				String propertyKey = prefix != null ? prefix + secretEntry.getKey() : secretEntry.getKey();
				properties.put(propertyKey, secretEntry.getValue());
			}
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Nullable
	private static String resolvePrefix(String context) {
		int prefixIndex = context.indexOf(PREFIX_PART);
		if (prefixIndex != -1) {
			return context.substring(prefixIndex + PREFIX_PART.length());
		}
		return null;
	}

	private static String resolveSecretId(String context) {
		int prefixIndex = context.indexOf(PREFIX_PART);
		if (prefixIndex != -1) {
			return context.substring(0, prefixIndex);
		}
		return context;
	}

}
