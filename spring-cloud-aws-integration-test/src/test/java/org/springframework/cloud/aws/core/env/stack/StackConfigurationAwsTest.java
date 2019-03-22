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

package org.springframework.cloud.aws.core.env.stack;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.support.TestStackEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
public abstract class StackConfigurationAwsTest {

	@Autowired
	private ListableStackResourceFactory stackResourceFactory;

	@Autowired
	private ResourceIdResolver resourceIdResolver;

	@Test
	public void resourcesByType_withResourceType_containsMinimumResources()
			throws Exception {
		// Arrange

		// Act
		Collection<StackResource> resourcesByType = this.stackResourceFactory
				.resourcesByType("AWS::EC2::Instance");

		// Assert
		assertEquals(1, resourcesByType.size());

		StackResource stackResource = resourcesByType.iterator().next();
		assertEquals("UserTagAndUserDataInstance", stackResource.getLogicalId());
		assertEquals("AWS::EC2::Instance", stackResource.getType());
	}

	@Test
	public void lookupPhysicalResourceId_withEC2Instance_returnsPhysicalName()
			throws Exception {
		// Arrange

		// Act
		String physicalResourceId = this.stackResourceFactory
				.lookupPhysicalResourceId("UserTagAndUserDataInstance");

		// Assert
		assertNotNull(physicalResourceId);
		assertNotEquals("UserTagAndUserDataInstance", physicalResourceId);
	}

	@Test
	public void getAllResources_withConfiguredStack_returnsNonEmptyResourceList()
			throws Exception {
		// Arrange

		// Act
		Collection<StackResource> allResources = this.stackResourceFactory
				.getAllResources();

		// Assert
		assertFalse(allResources.isEmpty());
	}

	@Test
	public void getStackName_withManuallyConfiguredStackName_returnsManuallyConfiguredStackName()
			throws Exception {
		// Arrange

		// Act
		String stackName = this.stackResourceFactory.getStackName();

		// Assert
		assertEquals(TestStackEnvironment.DEFAULT_STACK_NAME, stackName);
	}

	public void resourceIdResolver_configuredByDefault_notNull() {
		// Arrange

		// Act

		// Assert
		assertNotNull(this.resourceIdResolver);
	}

}
