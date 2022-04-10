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

package org.springframework.cloud.aws.cloudmap.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.aws.cloudmap.model.discovery.CloudMapDiscovery;
import org.springframework.cloud.aws.cloudmap.model.registration.CloudMapRegistryProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * POJO to capture all cloudmap integration parameters (both registry and discovery).
 *
 * @author Hari Ohm Prasath Rajagopal
 * @since 2.3.2
 */
@ConfigurationProperties(CloudMapProperties.CONFIG_PREFIX)
public class CloudMapProperties implements EnvironmentAware {

	/**
	 * Default cloudmap prefix.
	 */
	public static final String CONFIG_PREFIX = "aws.cloudmap";

	private CloudMapRegistryProperties registry;

	private CloudMapDiscovery discovery;

	private String region;

	private boolean enabled;

	private String annotationBasePackage;

	private Environment environment;

	public String getAnnotationBasePackage() {
		return this.annotationBasePackage;
	}

	public void setAnnotationBasePackage(String annotationBasePackage) {
		this.annotationBasePackage = annotationBasePackage;
	}

	public String getRegion() {
		return this.region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public CloudMapRegistryProperties getRegistry() {
		return this.registry;
	}

	public void setRegistry(CloudMapRegistryProperties registry) {
		this.registry = registry;
	}

	public CloudMapDiscovery getDiscovery() {
		return this.discovery;
	}

	public void setDiscovery(CloudMapDiscovery discovery) {
		this.discovery = discovery;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Environment getEnvironment() {
		return environment;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public String toString() {
		return "AwsCloudMapProperties{" + "registry=" + registry + ", discovery=" + discovery + ", region='" + region
				+ '\'' + ", enabled=" + enabled + ", annotationBasePackage='" + annotationBasePackage + '\'' + '}';
	}

}
