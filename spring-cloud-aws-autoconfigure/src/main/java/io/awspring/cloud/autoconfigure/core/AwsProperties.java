package io.awspring.cloud.autoconfigure.core;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

@ConfigurationProperties("spring.cloud.aws")
public class AwsProperties {

	/**
	 * Overrides the default endpoint.
	 */
	@Nullable
	private URI endpoint;

	@Nullable
	public URI getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(@Nullable URI endpoint) {
		this.endpoint = endpoint;
	}
}
