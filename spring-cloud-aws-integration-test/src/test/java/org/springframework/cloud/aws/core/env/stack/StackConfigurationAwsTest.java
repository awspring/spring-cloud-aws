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

package org.springframework.cloud.aws.core.env.stack;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.AWSIntegration;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.support.TestStackEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@AWSIntegration
abstract class StackConfigurationAwsTest {

	@Autowired
	private ListableStackResourceFactory stackResourceFactory;

	@Autowired
	private ResourceIdResolver resourceIdResolver;

	@Test
	void resourcesByType_withResourceType_containsMinimumResources() throws Exception {
		// Arrange

		// Act
		Collection<StackResource> resourcesByType = this.stackResourceFactory
				.resourcesByType("AWS::EC2::Instance");

		// Assert
		assertThat(resourcesByType.size()).isEqualTo(1);

		StackResource stackResource = resourcesByType.iterator().next();
		assertThat(stackResource.getLogicalId()).isEqualTo("UserTagAndUserDataInstance");
		assertThat(stackResource.getType()).isEqualTo("AWS::EC2::Instance");
	}

	@Test
	void lookupPhysicalResourceId_withEC2Instance_returnsPhysicalName() throws Exception {
		// Arrange

		// Act
		String physicalResourceId = this.stackResourceFactory
				.lookupPhysicalResourceId("UserTagAndUserDataInstance");

		// Assert
		assertThat(physicalResourceId).isNotNull();
		assertThat(physicalResourceId).isNotEqualTo("UserTagAndUserDataInstance");
	}

	@Test
	void getAllResources_withConfiguredStack_returnsNonEmptyResourceList()
			throws Exception {
		// Arrange

		// Act
		Collection<StackResource> allResources = this.stackResourceFactory
				.getAllResources();

		// Assert
		assertThat(allResources.isEmpty()).isFalse();
	}

	@Test
	void getStackName_withManuallyConfiguredStackName_returnsManuallyConfiguredStackName()
			throws Exception {
		// Arrange

		// Act
		String stackName = this.stackResourceFactory.getStackName();

		// Assert
		assertThat(stackName).isEqualTo(TestStackEnvironment.DEFAULT_STACK_NAME);
	}

	@Test
	void resourceIdResolver_configuredByDefault_notNull() {
		// Arrange

		// Act

		// Assert
		assertThat(this.resourceIdResolver).isNotNull();
	}

}
