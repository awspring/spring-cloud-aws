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
package io.awspring.cloud.autoconfigure.core;

import static io.awspring.cloud.autoconfigure.core.AwsProperties.CONFIG_PREFIX;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

/**
 * Configuration properties for AWS environment.
 *
 * @author Maciej Walkowiak
 * @since 3.0
 */
@ConfigurationProperties(CONFIG_PREFIX)
public class AwsProperties {
	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.cloud.aws";

	/**
	 * Overrides the default endpoint for all auto-configured AWS clients.
	 */
	@Nullable
	private URI endpoint;

	@Nullable
	public URI getEndpoint() {
		return this.endpoint;
	}

	public void setEndpoint(@Nullable URI endpoint) {
		this.endpoint = endpoint;
	}
}
