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
package io.awspring.cloud.autoconfigure.config.parameterstore;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import io.awspring.cloud.autoconfigure.config.reload.ReloadProperties;
import io.awspring.cloud.autoconfigure.config.secretsmanager.ReloadableProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for the AWS Parameter Store integration. Mostly based on the Spring Cloud Consul
 * Configuration equivalent.
 *
 * @author Joris Kuipers
 * @author Matej Nedic
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@ConfigurationProperties(ParameterStoreProperties.CONFIG_PREFIX)
public class ParameterStoreProperties extends AwsClientProperties implements ReloadableProperties {

	/**
	 * Configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.cloud.aws.parameterstore";

	/**
	 * Properties related to configuration reload.
	 */
	@NestedConfigurationProperty
	private ReloadProperties reload = new ReloadProperties();

	private boolean monitored;

	@Override
	public boolean isMonitored() {
		return monitored;
	}

	public void setMonitored(boolean monitored) {
		this.monitored = monitored;
	}

	@Override
	public ReloadProperties getReload() {
		return reload;
	}

	public void setReload(ReloadProperties reload) {
		this.reload = reload;
	}

}
