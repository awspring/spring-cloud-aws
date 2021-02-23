/*
 * Copyright 2013-2020 the original author or authors.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwsCloudMapPropertySources {

	private final AwsCloudMapDiscoveryProperties properties;

	private static final Logger log = LoggerFactory.getLogger(CloudMapDiscoverService.class);

	public AwsCloudMapPropertySources(AwsCloudMapDiscoveryProperties properties) {
		this.properties = properties;
	}

	public AwsCloudMapPropertySource createPropertySource(boolean optional, AWSServiceDiscovery serviceDiscovery,
			CloudMapDiscoverService instanceDiscovery) {
		try {
			AwsCloudMapPropertySource propertySource = new AwsCloudMapPropertySource(serviceDiscovery,
					instanceDiscovery);
			propertySource.init(properties);
			return propertySource;
		}
		catch (Exception e) {
			if (!optional) {
				throw new AwsCloudMapPropertySourceNotFoundException(e);
			}
			else {
				log.warn("Unable to find CloudMap service {}" + e.getMessage());
			}
		}
		return null;
	}

	static class AwsCloudMapPropertySourceNotFoundException extends RuntimeException {

		AwsCloudMapPropertySourceNotFoundException(Exception source) {
			super(source);
		}

	}

}
