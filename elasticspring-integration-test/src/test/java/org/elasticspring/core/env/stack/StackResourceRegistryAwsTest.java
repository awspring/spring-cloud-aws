package org.elasticspring.core.env.stack;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

// TODO change to test against resource id resolver (not stack resource registry)
public class StackResourceRegistryAwsTest {

	private final List<ClassPathXmlApplicationContext> loadedApplicationContexts = new ArrayList<ClassPathXmlApplicationContext>();

	@After
	public void destroyApplicationContexts() {
		for (ClassPathXmlApplicationContext applicationContext : this.loadedApplicationContexts) {
			applicationContext.close();
		}
	}

	@Test
	public void stackResourceRegistry_staticStackNameProvider_stackResourceRegistryBeanExposedUnderStackName() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("staticStackName");

		// Act
		StackResourceRegistry staticStackNameProviderBasedStackResourceRegistry = applicationContext.getBean("IntegrationTestStack", StackResourceRegistry.class);

		// Assert
		assertThat(staticStackNameProviderBasedStackResourceRegistry, is(not(nullValue())));
	}

	@Test
	public void stackResourceRegistry_autoDetectingStackNameProvider_stackResourceRegistryBeanExposedUnderGeneratedName() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("autoDetectStackName");

		// Act
		StackResourceRegistry autoDetectingStackNameProviderBasedStackResourceRegistry = applicationContext.getBean("org.elasticspring.core.env.stack.config.StackResourceRegistryFactoryBean#0", StackResourceRegistry.class);

		// Assert
		assertThat(autoDetectingStackNameProviderBasedStackResourceRegistry, is(not(nullValue())));
	}

	@Test
	public void lookupPhysicalResourceId_staticStackNameProviderAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("staticStackName");
		StackResourceRegistry staticStackNameProviderBasedStackResourceRegistry = applicationContext.getBean(StackResourceRegistry.class);

		// Act
		String physicalResourceId = staticStackNameProviderBasedStackResourceRegistry.lookupPhysicalResourceId("EmptyBucket");

		// Assert
		assertThat(physicalResourceId, startsWith("integrationteststack-emptybucket-"));
	}

	@Test
	public void lookupPhysicalResourceId_autoDetectingStackNameProviderAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("autoDetectStackName");
		StackResourceRegistry staticStackNameProviderBasedStackResourceRegistry = applicationContext.getBean(StackResourceRegistry.class);

		// Act
		String physicalResourceId = staticStackNameProviderBasedStackResourceRegistry.lookupPhysicalResourceId("EmptyBucket");

		// Assert
		assertThat(physicalResourceId, startsWith("integrationteststack-emptybucket-"));
	}

	@Test
	public void lookupPhysicalResourceId_logicalResourceIdOfNonExistingResourceProvided_throwsException() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("staticStackName");
		StackResourceRegistry stackResourceRegistry = applicationContext.getBean(StackResourceRegistry.class);

		// Act
		String physicalResourceId = stackResourceRegistry.lookupPhysicalResourceId("nonExistingLogicalResourceId");

		// Assert
		assertThat(physicalResourceId, is(nullValue()));
	}

	private ClassPathXmlApplicationContext loadApplicationContext(String configurationName) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(StackResourceRegistryAwsTest.class.getSimpleName() + "-" + configurationName + ".xml", StackResourceRegistryAwsTest.class);
		this.loadedApplicationContexts.add(applicationContext);

		return applicationContext;
	}

}
