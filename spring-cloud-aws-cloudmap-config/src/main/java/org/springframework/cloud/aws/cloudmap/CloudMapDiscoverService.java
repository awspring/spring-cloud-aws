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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.model.DiscoverInstancesRequest;
import com.amazonaws.services.servicediscovery.model.HttpInstanceSummary;
import com.amazonaws.services.servicediscovery.model.NamespaceNotFoundException;
import com.amazonaws.services.servicediscovery.model.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudMapDiscoverService {

	private static final Logger log = LoggerFactory.getLogger(CloudMapDiscoverService.class);

	public List<HttpInstanceSummary> discoverInstances(AWSServiceDiscovery serviceDiscovery,
			AwsCloudMapDiscoveryProperties properties) {
		final String namespace = properties.getServiceNameSpace();
		final String serviceName = properties.getService();
		try {
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
		catch (NamespaceNotFoundException e) {
			log.error("Unable to find the namespace {} - {}", namespace, e.getMessage(), e);
		}
		catch (ServiceNotFoundException e) {
			log.error("Unable to find the service {} under namespace {} - {}", serviceName, namespace, e.getMessage(),
					e);
		}
		return Collections.emptyList();
	}

}
