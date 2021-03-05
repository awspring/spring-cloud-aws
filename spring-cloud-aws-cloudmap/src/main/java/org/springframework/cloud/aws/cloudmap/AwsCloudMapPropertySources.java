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

package org.springframework.cloud.aws.cloudmap;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;

/**
 * Recursively retrieves all Http instances based on cloudmap namespace and services from
 * the AWS CloudMap using the provided AWS ServiceDiscovery client.
 *
 * @author Hari Ohm Prasath
 * @since 1.0
 */
public class AwsCloudMapPropertySources {

	private final CloudMapDiscoveryProperties properties;

	public AwsCloudMapPropertySources(CloudMapDiscoveryProperties properties) {
		this.properties = properties;
	}

	/**
	 * Create property store and initialize it.
	 * @param optional based on failFast attribute
	 * @param serviceDiscovery AWS service discovery
	 * @param instanceDiscovery helps to query cloudmap service with discovery parameters
	 * @return property source with key "namespace/service, httpinstance"
	 * @throws AwsCloudMapPropertySources.AwsCloudMapPropertySourceNotFoundException
	 * thrown in case of error and failFast=true
	 */
	public AwsCloudMapPropertySource createPropertySource(boolean optional, AWSServiceDiscovery serviceDiscovery,
			CloudMapDiscoverService instanceDiscovery)
			throws AwsCloudMapPropertySources.AwsCloudMapPropertySourceNotFoundException {
		AwsCloudMapPropertySource propertySource = new AwsCloudMapPropertySource(serviceDiscovery, instanceDiscovery);
		propertySource.init(optional, properties);
		return propertySource;
	}

	static class AwsCloudMapPropertySourceNotFoundException extends RuntimeException {

		AwsCloudMapPropertySourceNotFoundException(Exception source) {
			super(source);
		}

	}

}
