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
package io.awspring.cloud.secretsmanager;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.core.config.AwsPropertySource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

/**
 * Retrieves secret value under the given context / path from the AWS Secrets Manager using the provided Secrets Manager
 * client.
 *
 * @author Fabio Maia
 * @author Maciej Walkowiak
 * @author Arun Patra
 * @author Matej Nedic
 * @since 2.0.0
 */
public class SecretsManagerPropertySource
		extends AwsPropertySource<SecretsManagerPropertySource, SecretsManagerClient> {

	private static Log LOG = LogFactory.getLog(SecretsManagerPropertySource.class);
	private static final String PREFIX_PART = "?prefix=";

	private final ObjectMapper jsonMapper = new ObjectMapper();

	/**
	 * Full secret path containing both secret id and prefix.
	 */
	private final String context;

	/**
	 * Id of a secret stored in AWS Secrets Manager.
	 */
	private final String secretId;

	/**
	 * Prefix that gets added to resolved property keys. Useful same property keys are returned by multiple property
	 * sources.
	 */
	@Nullable
	private final String prefix;

	private final Map<String, Object> properties = new LinkedHashMap<>();

	public SecretsManagerPropertySource(String context, SecretsManagerClient smClient) {
		super("aws-secretsmanager:" + context, smClient);
		Assert.notNull(context, "context is required");
		this.context = context;
		this.secretId = resolveSecretId(context);
		this.prefix = resolvePrefix(context);
	}

	/**
	 * Loads properties from the Secrets Manager secret.
	 * @throws ResourceNotFoundException if specified secret does not exist in the AWS Secret Manager service.
	 */
	@Override
	public void init() {
		readSecretValue(GetSecretValueRequest.builder().secretId(secretId).build());
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = properties.keySet();
		return strings.toArray(new String[strings.size()]);
	}

	@Override
	@Nullable
	public Object getProperty(String name) {
		return properties.get(name);
	}

	private void readSecretValue(GetSecretValueRequest secretValueRequest) {
		GetSecretValueResponse secretValueResponse = source.getSecretValue(secretValueRequest);
		if (secretValueResponse.secretString() != null) {
			try {
				Map<String, Object> secretMap = jsonMapper.readValue(secretValueResponse.secretString(),
						new TypeReference<>() {
						});

				for (Map.Entry<String, Object> secretEntry : secretMap.entrySet()) {
					LOG.debug("Populating property retrieved from AWS Secrets Manager: " + secretEntry.getKey());
					String propertyKey = prefix != null ? prefix + secretEntry.getKey() : secretEntry.getKey();
					properties.put(propertyKey, secretEntry.getValue());
				}
			}
			catch (JsonParseException e) {
				// If the secret is not a JSON string, then it is a simple "plain text" string
				String[] parts = secretValueResponse.name().split("/");
				String secretName = parts[parts.length - 1];
				LOG.debug("Populating property retrieved from AWS Secrets Manager: " + secretName);
				String propertyKey = prefix != null ? prefix + secretName : secretName;
				properties.put(propertyKey, secretValueResponse.secretString());
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			String[] parts = secretValueResponse.name().split("/");
			String secretName = parts[parts.length - 1];
			LOG.debug("Populating property retrieved from AWS Secrets Manager: " + secretName);
			String propertyKey = prefix != null ? prefix + secretName : secretName;
			properties.put(propertyKey, secretValueResponse.secretBinary().asByteArray());
		}
	}

	@Override
	public SecretsManagerPropertySource copy() {
		return new SecretsManagerPropertySource(this.context, this.source);
	}

	@Nullable
	String getPrefix() {
		return prefix;
	}

	String getContext() {
		return context;
	}

	String getSecretId() {
		return secretId;
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
