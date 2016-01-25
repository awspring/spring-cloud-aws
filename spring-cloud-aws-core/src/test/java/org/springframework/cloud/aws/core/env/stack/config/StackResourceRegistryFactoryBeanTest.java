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

import org.hamcrest.CustomMatcher;
import org.junit.Test;
import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StackResourceRegistryFactoryBeanTest {

	private static final String STACK_NAME = "myStack";

	@Test
	public void createInstance_stackWithTwoResources_returnsStackResourceRegistryWithTwoResources() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalResourceIdTwo", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(STACK_NAME, resourceIdMappings);

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdOne"), is("physicalResourceIdOne"));
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdTwo"), is("physicalResourceIdTwo"));
	}

	@Test
	public void createInstance_stackWithTwoResources_listsBothResources() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalResourceIdTwo", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(STACK_NAME, resourceIdMappings);

		// Act
		ListableStackResourceFactory stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getAllResources().size(), is(2));
		for (StackResource stackResource : stackResourceRegistry.getAllResources()) {
			assertThat(stackResource.getLogicalId(), anyOf(is("logicalResourceIdOne"), is("logicalResourceIdTwo")));
			assertThat(stackResource.getPhysicalId(), anyOf(is("physicalResourceIdOne"), is("physicalResourceIdTwo")));
			assertThat(stackResource.getType(), is("Amazon::SES::Test"));
		}
	}

	@Test
	public void createInstance_stackWithName_returnsStackResourceRegistryWithStackName() throws Exception {
		// Arrange
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(STACK_NAME, Collections.<String, String>emptyMap());

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getStackName(), is(STACK_NAME));
	}

	@Test
	public void createInstance_stackWithNoResources_returnsStackResourceRegistryAnsweringWithNullForNonExistingLogicalResourceId() throws Exception {
		// Arrange
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(STACK_NAME, Collections.<String, String>emptyMap());

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("nonExistingLogicalResourceId"), is(nullValue()));
	}

	@Test
	public void createInstance_stackWithNestedStack() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalNestedStack", "physicalStackId");
		resourceIdMappings.put("logicalNestedStack.logicalResourceIdTwo", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(STACK_NAME, resourceIdMappings);

		// Act
		ListableStackResourceFactory stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getAllResources().size(), is(3));
		for (StackResource stackResource : stackResourceRegistry.getAllResources()) {
			assertThat(stackResource.getLogicalId(), anyOf(is("logicalResourceIdOne"), is("logicalNestedStack"), is("logicalNestedStack.logicalResourceIdTwo")));
			assertThat(stackResource.getPhysicalId(), anyOf(is("physicalResourceIdOne"), is("physicalStackId"), is("physicalResourceIdTwo")));
			assertThat(stackResource.getType(), anyOf(is("AWS::CloudFormation::Stack"), is("Amazon::SES::Test")));
		}

		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalNestedStack.logicalResourceIdTwo"), notNullValue());
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdTwo"), notNullValue());
	}

	@Test
	public void createInstance_stackWithNestedStack_dontReturnDuplicateResourceId() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalNested1Stack", "physicalStackId");
		resourceIdMappings.put("logicalNested1Stack.logicalResource", "physicalResourceIdOne");
		resourceIdMappings.put("logicalNested2Stack", "physicalStackId");
		resourceIdMappings.put("logicalNested2Stack.logicalResource", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(STACK_NAME, resourceIdMappings);

		// Act
		ListableStackResourceFactory stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getAllResources().size(), is(4));
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalNested1Stack.logicalResource"), notNullValue());
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalNested2Stack.logicalResource"), notNullValue());
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResource"), nullValue());
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
		Map<String, List<StackResourceSummary>> stackResourceSummaries = new HashMap<>();
		stackResourceSummaries.put(STACK_NAME, new ArrayList<StackResourceSummary>()); // allow stack with no resources

		for (Map.Entry<String, String> entry : resourceIdMappings.entrySet()) {
			String logicalResourceId = entry.getKey();
			String physicalResourceId = entry.getValue();

			String physicalStackName;
			if (logicalResourceId.contains(".")) {
				physicalStackName = resourceIdMappings.get(logicalResourceId.substring(0, logicalResourceId.lastIndexOf(".")));
				logicalResourceId = logicalResourceId.substring(logicalResourceId.lastIndexOf(".") + 1);
			} else {
				physicalStackName = STACK_NAME;
			}

			List<StackResourceSummary> list = stackResourceSummaries.get(physicalStackName);
			if (list == null) {
				list = new ArrayList<>();
				stackResourceSummaries.put(physicalStackName, list);
			}

			list.add(makeStackResourceSummary(logicalResourceId, physicalResourceId));
		}

		AmazonCloudFormation amazonCloudFormationClient = mock(AmazonCloudFormation.class);

		for (Map.Entry<String, List<StackResourceSummary>> entry : stackResourceSummaries.entrySet()) {
			final String stackName = entry.getKey();

			ListStackResourcesResult listStackResourcesResult = mock(ListStackResourcesResult.class);
			when(listStackResourcesResult.getStackResourceSummaries()).thenReturn(entry.getValue());

			when(amazonCloudFormationClient.listStackResources(argThat(new CustomMatcher<ListStackResourcesRequest>("describe stack '" + entry.getKey() + "'") {

				@Override
				public boolean matches(Object item) {
					return item != null && stackName.equals(((ListStackResourcesRequest)item).getStackName());
				}


			}))).thenReturn(listStackResourcesResult);
		}

		return amazonCloudFormationClient;
	}

	private static StackResourceSummary makeStackResourceSummary(String logicalResourceId, String physicalResourceId) {
		StackResourceSummary stackResourceSummary = mock(StackResourceSummary.class);
		when(stackResourceSummary.getLogicalResourceId()).thenReturn(logicalResourceId);
		when(stackResourceSummary.getPhysicalResourceId()).thenReturn(physicalResourceId);
		when(stackResourceSummary.getResourceType()).thenReturn(logicalResourceId.endsWith("Stack") ? "AWS::CloudFormation::Stack" : "Amazon::SES::Test");
		return stackResourceSummary;
	}

}
