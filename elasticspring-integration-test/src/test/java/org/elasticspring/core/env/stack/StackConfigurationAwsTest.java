package org.elasticspring.core.env.stack;

import org.elasticspring.core.env.ResourceIdResolver;
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
		StackResourceRegistry autoDetectingStackNameProviderBasedStackResourceRegistry = applicationContext.getBean("org.elasticspring.core.env.stack.config.StackResourceRegistryFactoryBean#0", StackResourceRegistry.class);

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
