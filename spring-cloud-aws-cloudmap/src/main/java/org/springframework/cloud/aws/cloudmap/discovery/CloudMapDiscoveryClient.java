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

package org.springframework.cloud.aws.cloudmap.discovery;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;

import org.springframework.cloud.aws.cloudmap.CloudMapUtils;
import org.springframework.cloud.aws.cloudmap.model.CloudMapProperties;
import org.springframework.cloud.aws.cloudmap.model.discovery.CloudMapDiscoveryProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

// @checkstyle: off
public class CloudMapDiscoveryClient implements DiscoveryClient {

	/**
	 * Description of the service.
	 */
	public static final String DESCRIPTION = "AWS CloudMap Discovery Client";

	private static final CloudMapUtils UTILS = CloudMapUtils.INSTANCE.getInstance();

	private final AWSServiceDiscovery serviceDiscovery;

	private final CloudMapProperties properties;

	public CloudMapDiscoveryClient(AWSServiceDiscovery serviceDiscovery, CloudMapProperties properties) {
		this.serviceDiscovery = serviceDiscovery;
		this.properties = properties;
	}

	@Override
	public int getOrder() {
		return DiscoveryClient.super.getOrder();
	}

	@Override
	public String description() {
		return DESCRIPTION;
	}

	@Override
	public List<String> getServices() {
		final List<CloudMapDiscoveryProperties> discoveryProperties = properties.getDiscovery().getDiscoveryList();
		if (discoveryProperties != null && !discoveryProperties.isEmpty()) {
			return UTILS.listServices(serviceDiscovery, discoveryProperties);
		}

		return Collections.emptyList();
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		// Service ID maintained as <namespace>_<serviceId>
		String[] split = serviceId.split("@");
		if (split.length == 2) {
			return UTILS.listInstances(serviceDiscovery, split[0], split[1]).stream().map(UTILS::getServiceInstance)
					.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

}
