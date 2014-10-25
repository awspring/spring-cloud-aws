/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.core.env.stack.config;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StackResourceRegistryFactoryBeanTest {

	@Test
	public void createInstance_stackWithTwoResources_returnsStackResourceRegistryWithTwoResources() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalResourceIdTwo", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean("myStack", resourceIdMappings);

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdOne"), is("physicalResourceIdOne"));
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdTwo"), is("physicalResourceIdTwo"));
	}

	@Test
	public void createInstance_stackWithName_returnsStackResourceRegistryWithStackName() throws Exception {
		// Arrange
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean("myStack", Collections.<String, String>emptyMap());

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getStackName(), is("myStack"));
	}

	@Test
	public void createInstance_stackWithNoResources_returnsStackResourceRegistryAnsweringWithNullForNonExistingLogicalResourceId() throws Exception {
		// Arrange
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean("myStack", Collections.<String, String>emptyMap());

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("nonExistingLogicalResourceId"), is(nullValue()));
	}

	private static StackResourceRegistryFactoryBean makeStackResourceRegistryFactoryBean(String stackName, Map<String, String> resourceIdMappings) {
		AmazonCloudFormation amazonCloudFormationClient = makeAmazonCloudFormationClient(resourceIdMappings);
		StackNameProvider stackNameProvider = makeStackNameProvider(stackName);

		return new StackResourceRegistryFactoryBean(amazonCloudFormationClient, stackNameProvider);
	}

	private static StackNameProvider makeStackNameProvider(String stackName) {
		StackNameProvider stackNameProvider = mock(StackNameProvider.class);
		when(stackNameProvider.getStackName()).thenReturn(stackName);

		return stackNameProvider;
	}

	private static AmazonCloudFormation makeAmazonCloudFormationClient(Map<String, String> resourceIdMappings) {
		List<StackResourceSummary> stackResourceSummaries = new ArrayList<>();

		for (Map.Entry<String, String> entry : resourceIdMappings.entrySet()) {
			String logicalResourceId = entry.getKey();
			String physicalResourceId = entry.getValue();

			stackResourceSummaries.add(makeStackResourceSummary(logicalResourceId, physicalResourceId));
		}

		ListStackResourcesResult listStackResourcesResult = mock(ListStackResourcesResult.class);
		when(listStackResourcesResult.getStackResourceSummaries()).thenReturn(stackResourceSummaries);

		AmazonCloudFormation amazonCloudFormationClient = mock(AmazonCloudFormation.class);
		when(amazonCloudFormationClient.listStackResources(any(ListStackResourcesRequest.class))).thenReturn(listStackResourcesResult);

		return amazonCloudFormationClient;
	}

	private static StackResourceSummary makeStackResourceSummary(String logicalResourceId, String physicalResourceId) {
		StackResourceSummary stackResourceSummary = mock(StackResourceSummary.class);
		when(stackResourceSummary.getLogicalResourceId()).thenReturn(logicalResourceId);
		when(stackResourceSummary.getPhysicalResourceId()).thenReturn(physicalResourceId);

		return stackResourceSummary;
	}

}
