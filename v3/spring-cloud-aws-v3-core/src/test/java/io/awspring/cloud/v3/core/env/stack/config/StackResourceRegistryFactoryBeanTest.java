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

package io.awspring.cloud.v3.core.env.stack.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.awspring.cloud.v3.core.env.stack.ListableStackResourceFactory;
import io.awspring.cloud.v3.core.env.stack.StackResource;
import io.awspring.cloud.v3.core.env.stack.StackResourceRegistry;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;

class StackResourceRegistryFactoryBeanTest {

	private static final String STACK_NAME = "myStack";

	private static StackResourceRegistryFactoryBean makeStackResourceRegistryFactoryBean(String stackName,
																						 Map<String, String> resourceIdMappings) {
		CloudFormationClient amazonCloudFormationClient = makeAmazonCloudFormationClient(resourceIdMappings);
		StackNameProvider stackNameProvider = makeStackNameProvider(stackName);

		return new StackResourceRegistryFactoryBean(amazonCloudFormationClient, stackNameProvider);
	}

	private static StackNameProvider makeStackNameProvider(String stackName) {
		StackNameProvider stackNameProvider = mock(StackNameProvider.class);
		when(stackNameProvider.getStackName()).thenReturn(stackName);

		return stackNameProvider;
	}

	private static CloudFormationClient makeAmazonCloudFormationClient(Map<String, String> resourceIdMappings) {
		Map<String, List<StackResourceSummary>> stackResourceSummaries = new HashMap<>();
		stackResourceSummaries.put(STACK_NAME, new ArrayList<>()); // allow stack with no
																	// resources

		for (Map.Entry<String, String> entry : resourceIdMappings.entrySet()) {
			String logicalResourceId = entry.getKey();
			String physicalResourceId = entry.getValue();

			String physicalStackName;
			if (logicalResourceId.contains(".")) {
				physicalStackName = resourceIdMappings
						.get(logicalResourceId.substring(0, logicalResourceId.lastIndexOf(".")));
				logicalResourceId = logicalResourceId.substring(logicalResourceId.lastIndexOf(".") + 1);
			}
			else {
				physicalStackName = STACK_NAME;
			}

			List<StackResourceSummary> list = stackResourceSummaries.computeIfAbsent(physicalStackName,
					k -> new ArrayList<>());

			list.add(makeStackResourceSummary(logicalResourceId, physicalResourceId));
		}

		CloudFormationClient amazonCloudFormationClient = mock(CloudFormationClient.class);

		for (Map.Entry<String, List<StackResourceSummary>> entry : stackResourceSummaries.entrySet()) {
			String stackName = entry.getKey();

			ListStackResourcesResponse listStackResourcesResult = mock(ListStackResourcesResponse.class);
			when(listStackResourcesResult.stackResourceSummaries()).thenReturn(entry.getValue());


			when(amazonCloudFormationClient
				.listStackResources(eq(ListStackResourcesRequest.builder()
						.stackName(stackName)
						.build())))
							.thenReturn(listStackResourcesResult);
		}

		return amazonCloudFormationClient;
	}

	private static StackResourceSummary makeStackResourceSummary(String logicalResourceId, String physicalResourceId) {
		StackResourceSummary stackResourceSummary = mock(StackResourceSummary.class);
		when(stackResourceSummary.logicalResourceId()).thenReturn(logicalResourceId);
		when(stackResourceSummary.physicalResourceId()).thenReturn(physicalResourceId);
		when(stackResourceSummary.resourceType())
				.thenReturn(logicalResourceId.endsWith("Stack") ? "AWS::CloudFormation::Stack" : "Amazon::SES::Test");
		return stackResourceSummary;
	}

	@Test
	void createInstance_stackWithTwoResources_returnsStackResourceRegistryWithTwoResources() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalResourceIdTwo", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, resourceIdMappings);

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdOne"))
				.isEqualTo("physicalResourceIdOne");
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdTwo"))
				.isEqualTo("physicalResourceIdTwo");
	}

	@Test
	void createInstance_stackWithNextTag_returnsStackResourceRegistryBuildWithTwoPages() throws Exception {
		// Arrange
		CloudFormationClient cloudFormationClient = mock(CloudFormationClient.class);
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = new StackResourceRegistryFactoryBean(
				cloudFormationClient, new StaticStackNameProvider(STACK_NAME));

		when(cloudFormationClient.listStackResources(
			ListStackResourcesRequest.builder()
				.stackName(STACK_NAME)
				.build()))
				.thenReturn(ListStackResourcesResponse.builder()
					.nextToken("2")
					.stackResourceSummaries(StackResourceSummary.builder()
						.logicalResourceId("log1")
						.build())
					.build());
		when(cloudFormationClient.listStackResources(
			ListStackResourcesRequest.builder()
				.stackName(STACK_NAME)
				.nextToken("2")
				.build()))
			.thenReturn(ListStackResourcesResponse.builder()
				.stackResourceSummaries(StackResourceSummary.builder()
					.logicalResourceId("log2")
					.build())
				.build());

		// Act
		ListableStackResourceFactory stackResourceFactory = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		verify(cloudFormationClient, times(2)).listStackResources(isA(ListStackResourcesRequest.class));
		assertThat(stackResourceFactory.getAllResources().size()).isEqualTo(2);
	}

	@Test
	void createInstance_stackWithTwoResources_listsBothResources() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalResourceIdTwo", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, resourceIdMappings);

		// Act
		ListableStackResourceFactory stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getAllResources().size()).isEqualTo(2);
		for (StackResource stackResource : stackResourceRegistry.getAllResources()) {
			assertThat(stackResource.getLogicalId()).isIn("logicalResourceIdOne", "logicalResourceIdTwo");
			assertThat(stackResource.getPhysicalId()).isIn("physicalResourceIdOne", "physicalResourceIdTwo");
			assertThat(stackResource.getType()).isEqualTo("Amazon::SES::Test");
		}
	}

	@Test
	void createInstance_stackWithName_returnsStackResourceRegistryWithStackName() throws Exception {
		// Arrange
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, Collections.emptyMap());

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getStackName()).isEqualTo(STACK_NAME);
	}

	// @checkstyle:off
	@Test
	void createInstance_stackWithNoResources_returnsStackResourceRegistryAnsweringWithNullForNonExistingLogicalResourceId()
			throws Exception {
		// @checkstyle:on
		// Arrange
		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, Collections.emptyMap());

		// Act
		StackResourceRegistry stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("nonExistingLogicalResourceId")).isNull();
	}

	@Test
	void createInstance_stackWithNestedStack() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalResourceIdOne", "physicalResourceIdOne");
		resourceIdMappings.put("logicalNestedStack", "physicalStackId");
		resourceIdMappings.put("logicalNestedStack.logicalResourceIdTwo", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, resourceIdMappings);

		// Act
		ListableStackResourceFactory stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getAllResources()).hasSize(3);
		for (StackResource stackResource : stackResourceRegistry.getAllResources()) {
			assertThat(stackResource.getLogicalId()).isIn("logicalResourceIdOne", "logicalNestedStack",
					"logicalNestedStack.logicalResourceIdTwo");
			assertThat(stackResource.getPhysicalId()).isIn("physicalResourceIdOne", "physicalStackId",
					"physicalResourceIdTwo");
			assertThat(stackResource.getType()).isIn("AWS::CloudFormation::Stack", "Amazon::SES::Test");
		}

		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalNestedStack.logicalResourceIdTwo"))
				.isNotNull();
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResourceIdTwo")).isNotNull();
	}

	@Test
	void createInstance_stackWithNestedStack_dontReturnDuplicateResourceId() throws Exception {
		// Arrange
		Map<String, String> resourceIdMappings = new HashMap<>();
		resourceIdMappings.put("logicalNested1Stack", "physicalStackId");
		resourceIdMappings.put("logicalNested1Stack.logicalResource", "physicalResourceIdOne");
		resourceIdMappings.put("logicalNested2Stack", "physicalStackId");
		resourceIdMappings.put("logicalNested2Stack.logicalResource", "physicalResourceIdTwo");

		StackResourceRegistryFactoryBean stackResourceRegistryFactoryBean = makeStackResourceRegistryFactoryBean(
				STACK_NAME, resourceIdMappings);

		// Act
		ListableStackResourceFactory stackResourceRegistry = stackResourceRegistryFactoryBean.createInstance();

		// Assert
		assertThat(stackResourceRegistry.getAllResources().size()).isEqualTo(4);
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalNested1Stack.logicalResource")).isNotNull();
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalNested2Stack.logicalResource")).isNotNull();
		assertThat(stackResourceRegistry.lookupPhysicalResourceId("logicalResource")).isNull();
	}

}
