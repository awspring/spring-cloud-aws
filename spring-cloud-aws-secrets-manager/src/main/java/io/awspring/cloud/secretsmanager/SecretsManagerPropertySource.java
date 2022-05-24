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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.lang.Nullable;
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
 * @since 2.0.0
 */
public class SecretsManagerPropertySource extends EnumerablePropertySource<SecretsManagerClient> {

	private static Log LOG = LogFactory.getLog(SecretsManagerPropertySource.class);

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private final String context;

	private final Map<String, Object> properties = new LinkedHashMap<>();

	public SecretsManagerPropertySource(String context, SecretsManagerClient smClient) {
		super(context, smClient);
		this.context = context;
	}

	/**
	 * Loads properties from the Secrets Manager secret.
	 * @throws ResourceNotFoundException if specified secret does not exist in the AWS Secret Manager service.
	 */
	public void init() {
		readSecretValue(GetSecretValueRequest.builder().secretId(context).build());
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
		try {
			Map<String, Object> secretMap = jsonMapper.readValue(secretValueResponse.secretString(),
					new TypeReference<Map<String, Object>>() {
					});

			for (Map.Entry<String, Object> secretEntry : secretMap.entrySet()) {
				LOG.debug("Populating property retrieved from AWS Secrets Manager: " + secretEntry.getKey());
				properties.put(secretEntry.getKey(), secretEntry.getValue());
			}
		}
		catch (JsonParseException e) {
			// If the secret is not a JSON string, then it is a simple "plain text" string
			if (secretValueResponse.name().endsWith("/")) {
				throw new RuntimeException(
						"Plain Text Secret which name ends with / cannot be resolved. Please change SecretName so it does not end with `/`   SecretName is:"
								+ secretValueResponse.name());
			}
			String secretName = secretValueResponse.name().lastIndexOf("/") != 0
					? secretValueResponse.name().substring(secretValueResponse.name().lastIndexOf("/") + 1)
					: secretValueResponse.name();
			LOG.debug("Populating property retrieved from AWS Secrets Manager: " + secretName);
			properties.put(secretName, secretValueResponse.secretString());
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
