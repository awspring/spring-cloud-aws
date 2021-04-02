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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.model.HttpInstanceSummary;
import com.amazonaws.services.servicediscovery.model.NamespaceNotFoundException;
import com.amazonaws.services.servicediscovery.model.ServiceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.EnumerablePropertySource;

/**
 * Read HttpInstanceSummary information based on cloudmap discovery parameters and add
 * them with the key cloudMap namespace/service.
 *
 * @author Hari Ohm Prasath
 * @since 2.3.2
 */
public class AwsCloudMapPropertySource extends EnumerablePropertySource<AWSServiceDiscovery> {

	private static final Logger log = LoggerFactory.getLogger(AwsCloudMapPropertySource.class);

	private static final String DEFAULT_CONTEXT = "cloudmap";

	private final AWSServiceDiscovery serviceDiscovery;

	private final CloudMapDiscoverService instanceDiscovery;

	private final ObjectMapper jsonMapper = new ObjectMapper();

	private final Map<String, Object> properties = new LinkedHashMap<>();

	public AwsCloudMapPropertySource(AWSServiceDiscovery serviceDiscovery, CloudMapDiscoverService instanceDiscovery) {
		super(DEFAULT_CONTEXT, serviceDiscovery);
		this.serviceDiscovery = serviceDiscovery;
		this.instanceDiscovery = instanceDiscovery;
	}

	/**
	 * Initialize cloudmap discovery, store them in property source.
	 * @param optional based on failFast attribute
	 * @param discoveryParameters cloudmap discovery parameters
	 * @throws AwsCloudMapPropertySources.AwsCloudMapPropertySourceNotFoundException
	 * thrown in case of error and failFast=true
	 */
	public void init(boolean optional, CloudMapDiscoveryProperties discoveryParameters)
			throws AwsCloudMapPropertySources.AwsCloudMapPropertySourceNotFoundException {
		getServices(optional, discoveryParameters);
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> strings = this.properties.keySet();
		return strings.toArray(new String[0]);
	}

	@Override
	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	/**
	 * Gets http instances based on discovery parameters, marshals them into string and
	 * stores them in property store.
	 * @param optional fail in case of error
	 * @param discoveryParameters cloudmap discovery parameters
	 * @throws AwsCloudMapPropertySources.AwsCloudMapPropertySourceNotFoundException
	 * thrown if failFast=true and unable to find the cloudmap instance
	 */
	private void getServices(boolean optional, CloudMapDiscoveryProperties discoveryParameters)
			throws AwsCloudMapPropertySources.AwsCloudMapPropertySourceNotFoundException {
		final String namespace = discoveryParameters.getNameSpace();
		final String serviceName = discoveryParameters.getService();
		final String key = namespace + "/" + serviceName;
		try {
			// AWS CloudMap API to get the http instances
			List<HttpInstanceSummary> summaryList = this.instanceDiscovery.discoverInstances(this.serviceDiscovery,
					discoveryParameters);
			this.properties.put(key, jsonMapper.writeValueAsString(summaryList));
		}
		catch (NamespaceNotFoundException | ServiceNotFoundException e) {
			// Throw error while starting up if failFast=true
			if (!optional) {
				throw new AwsCloudMapPropertySources.AwsCloudMapPropertySourceNotFoundException(e);
			}
			else {
				log.warn("Unable to find CloudMap service for {} - {}", key, e.getMessage());
				this.properties.put(key, "");
			}
		}
		catch (JsonProcessingException e) {
			log.error("Unable to marshal data to string {}", e.getMessage(), e);
		}
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

}
