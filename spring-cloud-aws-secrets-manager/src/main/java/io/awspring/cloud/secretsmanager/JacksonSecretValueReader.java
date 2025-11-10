package io.awspring.cloud.secretsmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

public class JacksonSecretValueReader implements SecretValueReader {
	private final JsonMapper jsonMapper;

	public JacksonSecretValueReader() {
		this.jsonMapper = new JsonMapper();
	}

	@Override
	public Map<String, Object> readSecretValue(String secretString) {
		try {
			return jsonMapper.readValue(secretString,
				new TypeReference<>() {
				});
		}
		catch (JacksonException e) {
			throw new SecretParseException(e);
		}
	}
}
