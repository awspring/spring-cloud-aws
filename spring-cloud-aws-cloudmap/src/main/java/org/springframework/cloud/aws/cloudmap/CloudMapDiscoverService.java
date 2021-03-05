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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.model.DiscoverInstancesRequest;
import com.amazonaws.services.servicediscovery.model.HttpInstanceSummary;
import com.amazonaws.services.servicediscovery.model.NamespaceNotFoundException;
import com.amazonaws.services.servicediscovery.model.ServiceNotFoundException;

/**
 * Builds cloudmap discovery request based and fetching the httpinstances using AWS
 * service discovery.
 *
 * @author Hari Ohm Prasath
 * @since 1.0
 */
public class CloudMapDiscoverService {

	/**
	 * Get Http instances from cloudmap based on the namespace, service name and filter
	 * attributes.
	 * @param serviceDiscovery AWS Service discovery
	 * @param properties cloudmap discovery properties
	 * @return list of http instances
	 * @throws NamespaceNotFoundException thrown if the namespace with the given name
	 * doesnt exist
	 * @throws ServiceNotFoundException thrown if the service with the given name doesnt
	 * exist
	 */
	public List<HttpInstanceSummary> discoverInstances(AWSServiceDiscovery serviceDiscovery,
			CloudMapDiscoveryProperties properties) throws NamespaceNotFoundException, ServiceNotFoundException {
		final String namespace = properties.getServiceNameSpace();
		final String serviceName = properties.getService();
		DiscoverInstancesRequest dRequest = new DiscoverInstancesRequest();
		dRequest.setNamespaceName(namespace);
		dRequest.setServiceName(serviceName);

		if (properties.getFilterAttributes() != null && !properties.getFilterAttributes().isEmpty()) {
			Map<String, String> filterMap = new HashMap<>();
			for (String key : properties.getFilterAttributes().keySet()) {
				filterMap.put(key, properties.getFilterAttributes().get(key));
			}
			dRequest.setQueryParameters(filterMap);
		}

		return serviceDiscovery.discoverInstances(dRequest).getInstances();
	}

}
