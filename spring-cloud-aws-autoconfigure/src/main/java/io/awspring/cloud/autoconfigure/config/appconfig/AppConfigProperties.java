/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.autoconfigure.config.appconfig;

import static io.awspring.cloud.autoconfigure.config.appconfig.AppConfigProperties.PREFIX;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.autoconfigure.config.reload.ReloadProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for AWS AppConfig integration.
 * @author Matej Nedic
 * @since 4.1.0
 */
@ConfigurationProperties(prefix = PREFIX)
public class AppConfigProperties extends AwsClientProperties {

	/**
	 * The prefix used for App Config related properties.
	 */
	public static final String PREFIX = "spring.cloud.aws.appconfig";

	/**
	 * Enables App Config import integration.
	 */
	private boolean enabled = true;

	/**
	 * Properties related to configuration reload.
	 */
	@NestedConfigurationProperty
	private ReloadProperties reload = new ReloadProperties();

	private String separator = "#";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public ReloadProperties getReload() {
		return reload;
	}

	public void setReload(ReloadProperties reload) {
		this.reload = reload;
	}
}
