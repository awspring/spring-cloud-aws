package io.awspring.cloud.secretsmanager;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Deprecated
public class Jackson2SecretValueReader implements SecretValueReader {
	private final ObjectMapper objectMapper;

	public Jackson2SecretValueReader() {
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public Map<String, Object> readSecretValue(String secretString) {
		try {
			return objectMapper.readValue(secretString,
				new TypeReference<>() {
				});
		}
		catch (JacksonException e) {
			throw new SecretParseException(e);
		}
	}
}
