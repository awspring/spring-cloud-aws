/*
 * Copyright 2013-2021 the original author or authors.
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

package io.awspring.cloud.v3.autoconfigure.secretsmanager;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the AWS Secrets Manager integration. Mostly based on the
 * Spring Cloud Consul Configuration equivalent.
 *
 * @author Fabio Maia
 * @author Matej Nedic
 * @author Hari Ohm Prasath
 * @author Arun Patra
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = SecretsManagerProperties.CONFIG_PREFIX)
public class SecretsManagerProperties {

	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.cloud.aws.secretsmanager";

	/**
	 * If region value is not null or empty it will be used in creation of
	 * AWSSecretsManager.
	 */
	private String region;

	/**
	 * Overrides the default endpoint.
	 */
	private URI endpoint;

	public String getRegion() {
		return region;
	}

	public void setRegion(final String region) {
		this.region = region;
	}

	public URI getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(URI endpoint) {
		this.endpoint = endpoint;
	}

}
