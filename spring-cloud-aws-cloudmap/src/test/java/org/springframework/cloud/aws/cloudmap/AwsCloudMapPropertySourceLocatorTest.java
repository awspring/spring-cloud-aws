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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.servicediscovery.AWSServiceDiscovery;
import com.amazonaws.services.servicediscovery.model.DiscoverInstancesRequest;
import com.amazonaws.services.servicediscovery.model.DiscoverInstancesResult;
import com.amazonaws.services.servicediscovery.model.HttpInstanceSummary;
import com.amazonaws.services.servicediscovery.model.NamespaceNotFoundException;
import com.amazonaws.services.servicediscovery.model.ServiceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AwsCloudMapPropertySourceLocatorTest}.
 *
 * @author Har Ohm Prasath
 * @since 1.0
 */
public class AwsCloudMapPropertySourceLocatorTest {

	private final AWSServiceDiscovery serviceDiscovery = mock(AWSServiceDiscovery.class);

	private final MockEnvironment env = new MockEnvironment();

	@Test
	void cloudMapServiceInstanceExists() {
		CloudMapDiscovery cloudMapDiscovery = new CloudMapDiscovery();
		cloudMapDiscovery.setDiscoveryList(Collections.singletonList(getDiscoveryProperties()));
		DiscoverInstancesResult firstResult = getFirstResult(cloudMapDiscovery.getDiscoveryList().get(0));
		when(this.serviceDiscovery.discoverInstances(any(DiscoverInstancesRequest.class))).thenReturn(firstResult);

		AwsCloudMapPropertySourceLocator locator = new AwsCloudMapPropertySourceLocator(this.serviceDiscovery,
				cloudMapDiscovery, new CloudMapDiscoverService());
		PropertySource<?> source = locator.locate(this.env);
		assertThat(source.getProperty(getName(cloudMapDiscovery.getDiscoveryList().get(0)))).hasToString(
				"[{\"instanceId\":\"INSTANCE_ID\",\"namespaceName\":\"namespace\",\"serviceName\":\"service\",\"healthStatus\":null,\"attributes\":null}]");
	}

	@Test
	void cloudMapInvalidResponseError() {
		when(this.serviceDiscovery.discoverInstances(any(DiscoverInstancesRequest.class))).thenAnswer(innovation -> {
			throw new JsonProcessingException("Exception") {
			};
		});

		CloudMapDiscovery cloudMapDiscovery = new CloudMapDiscovery();
		cloudMapDiscovery.setDiscoveryList(Collections.singletonList(getDiscoveryProperties()));
		AwsCloudMapPropertySourceLocator locator = new AwsCloudMapPropertySourceLocator(this.serviceDiscovery,
				cloudMapDiscovery, new CloudMapDiscoverService());
		PropertySource<?> source = locator.locate(this.env);
		assertThat(source.getProperty(getName(cloudMapDiscovery.getDiscoveryList().get(0)))).isNull();
	}

	@Test
	void cloudMapNameSpaceNotFoundException() {
		when(this.serviceDiscovery.discoverInstances(any(DiscoverInstancesRequest.class))).thenAnswer(innovation -> {
			throw new NamespaceNotFoundException("namespace not found") {
			};
		});

		CloudMapDiscovery cloudMapDiscovery = new CloudMapDiscovery();
		cloudMapDiscovery.setDiscoveryList(Collections.singletonList(getDiscoveryProperties()));
		AwsCloudMapPropertySourceLocator locator = new AwsCloudMapPropertySourceLocator(this.serviceDiscovery,
				cloudMapDiscovery, new CloudMapDiscoverService());
		PropertySource<?> source = locator.locate(this.env);
		assertThat(source.getProperty(getName(cloudMapDiscovery.getDiscoveryList().get(0)))).hasToString("");
	}

	@Test
	void cloudMapNameServiceNotFoundException() {
		when(this.serviceDiscovery.discoverInstances(any(DiscoverInstancesRequest.class))).thenAnswer(innovation -> {
			throw new ServiceNotFoundException("service not found") {
			};
		});

		CloudMapDiscovery cloudMapDiscovery = new CloudMapDiscovery();
		cloudMapDiscovery.setDiscoveryList(Collections.singletonList(getDiscoveryProperties()));
		AwsCloudMapPropertySourceLocator locator = new AwsCloudMapPropertySourceLocator(this.serviceDiscovery,
				cloudMapDiscovery, new CloudMapDiscoverService());
		PropertySource<?> source = locator.locate(this.env);
		assertThat(source.getProperty(getName(cloudMapDiscovery.getDiscoveryList().get(0)))).hasToString("");
	}

	@Test
	void cloudMapNoServiceFoundNotOptional() {
		try {
			when(this.serviceDiscovery.discoverInstances(any(DiscoverInstancesRequest.class)))
					.thenAnswer(innovation -> {
						throw new AwsCloudMapPropertySources.AwsCloudMapPropertySourceNotFoundException(
								new Exception()) {
						};
					});

			CloudMapDiscovery cloudMapDiscovery = new CloudMapDiscovery();
			cloudMapDiscovery.setDiscoveryList(Collections.singletonList(getDiscoveryProperties()));
			cloudMapDiscovery.setFailFast(true);
			AwsCloudMapPropertySourceLocator locator = new AwsCloudMapPropertySourceLocator(this.serviceDiscovery,
					cloudMapDiscovery, new CloudMapDiscoverService());
			locator.locate(this.env);

			Assertions.fail();
		}
		catch (AwsCloudMapPropertySources.AwsCloudMapPropertySourceNotFoundException e) {
			// Dont do anything
		}
		catch (Exception e) {
			Assertions.fail();
		}
	}

	private static CloudMapDiscoveryProperties getDiscoveryProperties() {
		CloudMapDiscoveryProperties properties = new CloudMapDiscoveryProperties();
		properties.setServiceNameSpace("namespace");
		properties.setService("service");
		Map<String, String> filterMap = new HashMap<>();
		filterMap.put("name", "value");
		properties.setFilterAttributes(filterMap);

		return properties;
	}

	private static DiscoverInstancesResult getFirstResult(CloudMapDiscoveryProperties properties) {
		DiscoverInstancesResult dResult = new DiscoverInstancesResult();
		HttpInstanceSummary summary = new HttpInstanceSummary();
		summary.setNamespaceName(properties.getServiceNameSpace());
		summary.setServiceName(properties.getService());
		summary.setInstanceId("INSTANCE_ID");
		dResult.setInstances(Collections.singleton(summary));
		return dResult;
	}

	private static String getName(CloudMapDiscoveryProperties properties) {
		return properties.getServiceNameSpace() + "/" + properties.getService();
	}

}
