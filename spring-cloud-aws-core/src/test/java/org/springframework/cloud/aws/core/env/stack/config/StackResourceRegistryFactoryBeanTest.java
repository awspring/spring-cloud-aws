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

package org.springframework.cloud.aws.core.env.stack.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import org.junit.Test;

import org.springframework.cloud.aws.core.env.stack.ListableStackResourceFactory;
import org.springframework.cloud.aws.core.env.stack.StackResource;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StackResourceRegistryFactoryBeanTest {

	private static final String STACK_NAME = "myStack";

	private static StackResourceRegistryFactoryBean makeStackResourceRegistryFactoryBean(
			String stackName, Map<String, String> resourceIdMappings) {
		AmazonCloudFormation amazonCloudFormationClient = makeAmazonCloudFormationClient(
				resourceIdMappings);
		StackNameProvider stackNameProvider = makeStackNameProvider(stackName);

		return new StackResourceRegistryFactoryBean(amazonCloudFormationClient,
				stackNameProvider);
	}

	private static StackNameProvider makeStackNameProvider(String stackName) {
		StackNameProvider stackNameProvider = mock(StackNameProvider.class);
		when(stackNameProvider.getStackName()).thenReturn(stackName);

		return stackNameProvider;
	}

	private static AmazonCloudFormation makeAmazonCloudFormationClient(
			Map<String, String> resourceIdMappings) {
		Map<String, List<StackResourceSummary>> stackResourceSummaries = new HashMap<>();
		stackResourceSummaries.put(STACK_NAME, new ArrayList<>()); // allow stack with no
																	// resources

		for (Map.Entry<String, String> entry : resourceIdMappings.entrySet()) {
			String logicalResourceId = entry.getKey();
			String physicalResourceId = entry.getValue();

			String physicalStackName;
			if (logicalResourceId.contains(".")) {
				physicalStackName = resourceIdMappings.get(logicalResourceId.substring(0,
						logicalResourceId.lastIndexOf(".")));
				logicalResourceId = logicalResourceId
						.substring(logicalResourceId.lastIndexOf(".") + 1);
			}
			else {
				physicalStackName = STACK_NAME;
			}

			List<StackResourceSummary> list = stackResourceSummaries
					.computeIfAbsent(physicalStackName, k -> new ArrayList<>());

			list.add(makeStackResourceSummary(logicalResourceId, physicalResourceId));
		}

		AmazonCloudFormation amazonCloudFormationClient = mock(
				AmazonCloudFormation.class);

		for (Map.Entry<String, List<StackResourceSummary>> entry : stackResourceSummaries
				.entrySet()) {
			String stackName = entry.getKey();

			ListStackResourcesResult listStackResourcesResult = mock(
					ListStackResourcesResult.class);
			when(listStackResourcesResult.getStackResourceSummaries())
					.thenReturn(entry.getValue());

			when(amazonCloudFormationClient.listStackResources(argThat(
					item -> item != null && stackName.equals((item).getStackName()))))
							.thenReturn(listStackResourcesResult);
		}

		return amazonCloudFormationClient;
	}

	private static StackResourceSummary makeStackResourceSummary(String logicalResourceId,
			String physicalResourceId) {
		StackResourceSummary stackResourceSummary = mock(StackResourceSummary.class);
		when(stackResourceSummary.getLogicalResourceId()).thenReturn(logicalResourceId);
		when(stackResourceSummary.getPhysicalResourceId()).thenReturn(physicalResourceId);
		when(stackResourceSummary.getResourceType())
				.thenReturn(logicalResourceId.endsWith("Stack")
						? "AWS::CloudFormation::Stack" : "Amazon::SES::Test");
		return stackResourceSummary;
	}

	@Test
	public void createInstance_stackWithTwoResources_returnsStackResourceRegistryWithTwoResources()
			throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalResourceIdTwo", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, resourceIdMappings);

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean
				.createInstance();

		// Assert
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdOne"))
				.isEqualTo("physicalResourceIdOne");
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdTwo"))
				.isEqualTo("physicalResourceIdTwo");
	}

	@Test
	public void createInstance_stackWithNextTag_returnsStackResourceRegistryBuildWithTwoPages()
			throws Exception {
		// Arrange
		AmazonCloudFormation cloudFormationClient = mock(AmazonCloudFormation.class);
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = new StackResourceRegistryFactoryBean(
				cloudFormationClient, new StaticStackNameProvider(STACK_NAME));

		when(cloudFormationClient.listStackResources(
				new ListStackResourcesRequest().withStackName(STACK_NAME)))
						.thenReturn(new ListStackResourcesResult().withNextToken("2")
								.withStackResourceSummaries(new StackResourceSummary()
										.withLogicalResourceId("log1")));
		when(cloudFormationClient.listStackResources(new ListStackResourcesRequest()
				.withStackName(STACK_NAME).withNextToken("2")))
						.thenReturn(new ListStackResourcesResult()
								.withStackResourceSummaries(new StackResourceSummary()
										.withLogicalResourceId("log2")));

		// Act
		ListableStackResourceFactory stackResourceFactory = stackResourceRegistryFactoryBean
				.createInstance();

		// Assert
		verify(cloudFormationClient, times(2))
				.listStackResources(isA(ListStackResourcesRequest.class));
		assertThat(stackResourceFactory.getAllResources().size()).isEqualTo(2);
	}

	@Test
	public void createInstance_stackWithTwoResources_listsBothResources()
			throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalResourceIdTwo", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, resourceIdMappings);

		// Act
		ListableStackResourceFactory stackResourceRegistry = stackResourceRegistryFactoryBean
				.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getAllResources().size()).isEqualTo(2);
		for (StackResource stackResource : stackResourceRegistry.getAllResources()) {
			assertThat(stackResource.getLogicalId()).isIn("logicalResourceIdOne",
					"logicalResourceIdTwo");
			assertThat(stackResource.getPhysicalId()).isIn("physicalResourceIdOne",
					"physicalResourceIdTwo");
			assertThat(stackResource.getType()).isEqualTo("Amazon::SES::Test");
		}
	}

	@Test
	public void createInstance_stackWithName_returnsStackResourceRegistryWithStackName()
			throws Exception {
		// Arrange
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, Collections.emptyMap());

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean
				.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getStackName()).isEqualTo(STACK_NAME);
	}

	// @checkstyle:off
	@Test
	public void createInstance_stackWithNoResources_returnsStackResourceRegistryAnsweringWithNullForNonExistingLogicalResourceId()
			throws Exception {
		// @checkstyle:on
		// Arrange
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, Collections.emptyMap());

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean
				.createInstance();

		// Assert
		assertThat(stackResourceRegistry
				.lookupPhysicalResourceId("nonExistingLogicalResourceId")).isNull();
	}

	@Test
	public void createInstance_stackWithNestedStack() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalNestedStack", "physicalStackId");
		resourceIdMappings.put("logicalNestedStack.logicalResourceIdTwo",
				"physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, resourceIdMappings);

		// Act
		ListableStackResourceFactory stackResourceRegistry = stackResourceRegistryFactoryBean
				.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getAllResources().size()).isEqualTo(3);
		for (StackResource stackResource : stackResourceRegistry.getAllResources()) {
			assertThat(stackResource.getLogicalId()).isIn("logicalResourceIdOne",
					"logicalNestedStack", "logicalNestedStack.logicalResourceIdTwo");
			assertThat(stackResource.getPhysicalId()).isIn("physicalResourceIdOne",
					"physicalStackId", "physicalResourceIdTwo");
			assertThat(stackResource.getType()).isIn("AWS::CloudFormation::Stack",
					"Amazon::SES::Test");
		}

		assertThat(stackResourceRegistry
				.lookupPhysicalResourceId("logicalNestedStack.logicalResourceIdTwo"))
						.isNotNull();
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdTwo"))
				.isNotNull();
	}

	@Test
	public void createInstance_stackWithNestedStack_dontReturnDuplicateResourceId()
			throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalNested1Stack", "physicalStackId");
		resourceIdMappings.put("logicalNested1Stack.logicalResource",
				"physicalResourceIdOne");
		resourceIdMappings.put("logicalNested2Stack", "physicalStackId");
		resourceIdMappings.put("logicalNested2Stack.logicalResource",
				"physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, resourceIdMappings);

		// Act
		ListableStackResourceFactory stackResourceRegistry = stackResourceRegistryFactoryBean
				.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getAllResources().size()).isEqualTo(4);
		assertThat(stackResourceRegistry
				.lookupPhysicalResourceId("logicalNested1Stack.logicalResource"))
						.isNotNull();
		assertThat(stackResourceRegistry
				.lookupPhysicalResourceId("logicalNested2Stack.logicalResource"))
						.isNotNull();
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResource"))
				.isNull();
	}

}
