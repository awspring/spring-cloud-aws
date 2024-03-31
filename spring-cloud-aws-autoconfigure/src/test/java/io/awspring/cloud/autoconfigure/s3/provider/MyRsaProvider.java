package io.awspring.cloud.autoconfigure.s3.provider;

import io.awspring.cloud.autoconfigure.s3.S3RsaProvider;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class MyRsaProvider implements S3RsaProvider {
	@Override
	public KeyPair generateKeyPair() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator
				.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			return keyPairGenerator.generateKeyPair();
		} catch (Exception e) {
			return null;
		}
	}
}
