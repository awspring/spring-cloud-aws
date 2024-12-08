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
package io.awspring.cloud.core;

import java.io.IOException;
import java.util.Properties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;

/**
 * Utility class for creating {@link ClientOverrideConfiguration} containing "Spring Cloud AWS" user agent. When used,
 * server side (AWS or AWS-compatible service) can measure how many requests to its services come from Spring Cloud AWS.
 *
 * @author Eddú Meléndez
 * @author Maciej Walkowiak
 */
public final class SpringCloudClientConfiguration {
	private static final String PROPERTIES_FILE_LOCATION = "/io/awspring/cloud/core/SpringCloudClientConfiguration.properties";

	private static final String NAME = "spring-cloud-aws";

	private final String version;
	private final ClientOverrideConfiguration clientOverrideConfiguration;

	public SpringCloudClientConfiguration(String version) {
		this.version = version;
		this.clientOverrideConfiguration = ClientOverrideConfiguration.builder()
				.putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX, getUserAgent()).build();
	}

	public SpringCloudClientConfiguration() {
		this(loadVersion());
	}

	private static String loadVersion() {
		try {
			Properties properties = PropertiesLoaderUtils
					.loadProperties(new ClassPathResource(PROPERTIES_FILE_LOCATION));
			return properties.getProperty("build.version");
		}
		catch (IOException e) {
			throw new IllegalStateException("Error when loading properties from " + PROPERTIES_FILE_LOCATION
					+ " for resolving Spring Cloud AWS version.", e);
		}
	}

	public ClientOverrideConfiguration clientOverrideConfiguration() {
		return clientOverrideConfiguration;
	}

	private String getUserAgent() {
		return NAME + "/" + version;
	}
}
