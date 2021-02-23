/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.cloudmap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(AwsCloudMapProperties.CONFIG_PREFIX)
public class AwsCloudMapProperties {

	/**
	 * Default cloudmap prefix.
	 */
	public static final String CONFIG_PREFIX = "aws.cloudmap";

	private AwsCloudMapRegistryProperties registry;

	private AwsCloudMapDiscoveryProperties discovery;

	private String region;

	private String annotationBasePackage;

	public String getAnnotationBasePackage() {
		return annotationBasePackage;
	}

	public void setAnnotationBasePackage(String annotationBasePackage) {
		this.annotationBasePackage = annotationBasePackage;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public AwsCloudMapRegistryProperties getRegistry() {
		return registry;
	}

	public void setRegistry(AwsCloudMapRegistryProperties registry) {
		this.registry = registry;
	}

	public AwsCloudMapDiscoveryProperties getDiscovery() {
		return discovery;
	}

	public void setDiscovery(AwsCloudMapDiscoveryProperties discovery) {
		this.discovery = discovery;
	}

}
