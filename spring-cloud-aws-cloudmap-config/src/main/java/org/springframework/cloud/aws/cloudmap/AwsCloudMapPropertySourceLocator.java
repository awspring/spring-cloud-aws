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

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

public class AwsCloudMapPropertySourceLocator implements PropertySourceLocator {

	private final AWSServiceDiscovery serviceDiscovery;

	private final AwsCloudMapDiscovery discovery;

	private final CloudMapDiscoverService instanceDiscovery;

	public AwsCloudMapPropertySourceLocator(AWSServiceDiscovery serviceDiscovery,
			AwsCloudMapDiscovery cloudMapDiscovery, CloudMapDiscoverService instanceDiscovery) {
		this.serviceDiscovery = serviceDiscovery;
		this.discovery = cloudMapDiscovery;
		this.instanceDiscovery = instanceDiscovery;
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		if (!(environment instanceof ConfigurableEnvironment)) {
			return null;
		}

		final CompositePropertySource composite = new CompositePropertySource(AwsCloudMapProperties.CONFIG_PREFIX);
		if (discovery != null) {
			discovery.getDiscoveryList().forEach(d -> {
				AwsCloudMapPropertySources sources = new AwsCloudMapPropertySources(d);
				PropertySource<AWSServiceDiscovery> propertySource = sources.createPropertySource(
						!this.discovery.isFailFast(), this.serviceDiscovery, this.instanceDiscovery);
				if (propertySource != null) {
					composite.addPropertySource(propertySource);
				}
			});
		}

		return composite;
	}

}
