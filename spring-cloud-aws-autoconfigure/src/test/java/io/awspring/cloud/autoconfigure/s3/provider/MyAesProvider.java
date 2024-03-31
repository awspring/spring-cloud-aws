package io.awspring.cloud.autoconfigure.s3.provider;

import io.awspring.cloud.autoconfigure.s3.S3AesProvider;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class MyAesProvider implements S3AesProvider {
	@Override
	public SecretKey generateSecretKey() {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(256);
			return  keyGenerator.generateKey();
		} catch (Exception e) {
			return null;
		}
	}
}
