package io.awspring.cloud.secretsmanager;

import java.util.Map;

interface SecretValueReader {

	Map<String, Object> readSecretValue(String secretString);
}
