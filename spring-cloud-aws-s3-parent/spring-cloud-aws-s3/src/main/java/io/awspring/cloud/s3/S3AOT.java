package io.awspring.cloud.s3;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class S3AOT implements RuntimeHintsRegistrar {
	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.resources().registerPattern("io/awspring/cloud/s3/S3ObjectContentTypeResolver.properties");
	}
}
