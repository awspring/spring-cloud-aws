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

package org.springframework.cloud.aws.autoconfigure.cloudmap;

import java.util.Collections;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.cloud.aws.cloudmap.AwsCloudMapPropertySource;
import org.springframework.cloud.aws.cloudmap.CloudMapDiscoverService;

public class AwsCloudMapConfigDataLoader implements ConfigDataLoader<AwsCloudMapConfigDataResource> {

	@Override
	public ConfigData load(ConfigDataLoaderContext context, AwsCloudMapConfigDataResource resource) {
		try {
			AWSServiceDiscovery discoveryClient = context.getBootstrapContext().get(AWSServiceDiscovery.class);
			CloudMapDiscoverService instanceDiscovery = context.getBootstrapContext()
					.get(CloudMapDiscoverService.class);
			AwsCloudMapPropertySource propertySource = resource.getPropertySources()
					.createPropertySource(resource.isOptional(), discoveryClient, instanceDiscovery);
			if (propertySource != null) {
				return new ConfigData(Collections.singletonList(propertySource));
			}
			else {
				return null;
			}
		}
		catch (Exception e) {
			throw new ConfigDataResourceNotFoundException(resource, e);
		}
	}

}
