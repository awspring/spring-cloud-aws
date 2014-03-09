package org.elasticspring.core.env.stack.config;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import org.elasticspring.core.env.stack.StackResourceRegistry;
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
		Map<String, String> resourceIdMappings = new HashMap<String, String>();
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
		AmazonCloudFormationClient amazonCloudFormationClient = makeAmazonCloudFormationClient(resourceIdMappings);
		StackNameProvider stackNameProvider = makeStackNameProvider(stackName);

		return new StackResourceRegistryFactoryBean(amazonCloudFormationClient, stackNameProvider);
	}

	private static StackNameProvider makeStackNameProvider(String stackName) {
		StackNameProvider stackNameProvider = mock(StackNameProvider.class);
		when(stackNameProvider.getStackName()).thenReturn(stackName);

		return stackNameProvider;
	}

	private static AmazonCloudFormationClient makeAmazonCloudFormationClient(Map<String, String> resourceIdMappings) {
		List<StackResourceSummary> stackResourceSummaries = new ArrayList<StackResourceSummary>();

		for (Map.Entry<String, String> entry : resourceIdMappings.entrySet()) {
			String logicalResourceId = entry.getKey();
			String physicalResourceId = entry.getValue();

			stackResourceSummaries.add(makeStackResourceSummary(logicalResourceId, physicalResourceId));
		}

		ListStackResourcesResult listStackResourcesResult = mock(ListStackResourcesResult.class);
		when(listStackResourcesResult.getStackResourceSummaries()).thenReturn(stackResourceSummaries);

		AmazonCloudFormationClient amazonCloudFormationClient = mock(AmazonCloudFormationClient.class);
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
