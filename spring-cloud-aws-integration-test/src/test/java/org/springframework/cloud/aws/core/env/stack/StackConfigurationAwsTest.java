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

package org.springframework.cloud.aws.core.env.stack;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.elasticspring.support.TestStackInstanceIdService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("StackConfigurationAwsTest-context.xml")
public class StackConfigurationAwsTest {

	private final List<ClassPathXmlApplicationContext> loadedApplicationContexts = new ArrayList<ClassPathXmlApplicationContext>();

	@Autowired
	private TestStackInstanceIdService testStackInstanceIdService;

	@Before
	public void enableInstanceIdMetadataService() {
		this.testStackInstanceIdService.enable();
	}

	@After
	public void disableInstanceIdMetadataService() {
		this.testStackInstanceIdService.disable();
	}

	@After
	public void destroyApplicationContexts() {
		for (ClassPathXmlApplicationContext applicationContext : this.loadedApplicationContexts) {
			applicationContext.close();
		}
	}

	@Test
	public void resourceIdResolver_stackConfiguration_resourceIdResolverBeanExposed() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("staticStackName");

		// Act
		ResourceIdResolver resourceIdResolver = applicationContext.getBean(ResourceIdResolver.class);

		// Assert
		assertThat(resourceIdResolver, is(not(nullValue())));
	}

	@Test
	public void stackResourceRegistry_stackConfigurationWithStaticName_stackResourceRegistryBeanExposedUnderStaticStackName() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("staticStackName");

		// Act
		StackResourceRegistry staticStackNameProviderBasedStackResourceRegistry = applicationContext.getBean("IntegrationTestStack", StackResourceRegistry.class);

		// Assert
		assertThat(staticStackNameProviderBasedStackResourceRegistry, is(not(nullValue())));
	}

	@Test
	public void stackResourceRegistry_stackConfigurationWithoutStaticName_stackResourceRegistryBeanExposedUnderGeneratedName() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("autoDetectStackName");

		// Act
		StackResourceRegistry autoDetectingStackNameProviderBasedStackResourceRegistry = applicationContext.getBean("org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean#0", StackResourceRegistry.class);

		// Assert
		assertThat(autoDetectingStackNameProviderBasedStackResourceRegistry, is(not(nullValue())));
	}

	@Test
	public void resourceIdResolverResolveToPhysicalResourceId_stackConfigurationWithStaticNameAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("staticStackName");
		ResourceIdResolver resourceIdResolver = applicationContext.getBean(ResourceIdResolver.class);

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("EmptyBucket");

		// Assert
		assertThat(physicalResourceId, startsWith("integrationteststack-emptybucket-"));
	}

	@Test
	public void resourceIdResolverResolveToPhysicalResourceId_stackConfigurationWithoutStaticNameAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("autoDetectStackName");
		ResourceIdResolver resourceIdResolver = applicationContext.getBean(ResourceIdResolver.class);

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("EmptyBucket");

		// Assert
		assertThat(physicalResourceId, startsWith("integrationteststack-emptybucket-"));
	}

	@Test
	public void resourceIdResolverResolveToPhysicalResourceId_logicalResourceIdOfNonExistingResourceProvided_returnsLogicalResourceIdAsPhysicalResourceId() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("staticStackName");
		ResourceIdResolver resourceIdResolver = applicationContext.getBean(ResourceIdResolver.class);

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("nonExistingLogicalResourceId");

		// Assert
		assertThat(physicalResourceId, is("nonExistingLogicalResourceId"));
	}

	private ClassPathXmlApplicationContext loadApplicationContext(String configurationName) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(StackConfigurationAwsTest.class.getSimpleName() + "-" + configurationName + ".xml", StackConfigurationAwsTest.class);
		this.loadedApplicationContexts.add(applicationContext);

		return applicationContext;
	}

}
