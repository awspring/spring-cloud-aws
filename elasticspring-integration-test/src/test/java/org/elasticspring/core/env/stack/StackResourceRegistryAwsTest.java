package org.elasticspring.core.env.stack;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

// TODO discuss where which types of tests should live (e.g. tests requiring amazon environment)
public class StackResourceRegistryAwsTest {

	@Test
	public void stackResourceRegistry_staticStackNameProvider_stackResourceRegistryBeanExposed() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("staticStackName");

		// Act
		StackResourceRegistry staticStackNameProviderBasedStackResourceRegistry = applicationContext.getBean(StackResourceRegistry.class);

		// Assert
		assertThat(staticStackNameProviderBasedStackResourceRegistry, is(not(nullValue())));
	}

	@Test
	public void stackResourceRegistry_autoDetectingStackNameProvider_stackResourceRegistryBeanExposed() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("autoDetectStackName");

		// Act
		StackResourceRegistry autoDetectingStackNameProviderBasedStackResourceRegistry = applicationContext.getBean(StackResourceRegistry.class);

		// Assert
		assertThat(autoDetectingStackNameProviderBasedStackResourceRegistry, is(not(nullValue())));
	}

	@Test
	public void lookupPhysicalResourceId_staticStackNameProviderAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("staticStackName");
		StackResourceRegistry staticStackNameProviderBasedStackResourceRegistry = applicationContext.getBean(StackResourceRegistry.class);

		// Act
		String physicalResourceId = staticStackNameProviderBasedStackResourceRegistry.lookupPhysicalResourceId("RdsSingleMicroInstance");

		// Assert
		assertThat(physicalResourceId, is(not(nullValue())));
	}

	@Test
	public void lookupPhysicalResourceId_autoDetectingStackNameProviderAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId() {
		// Arrange
		ClassPathXmlApplicationContext applicationContext = loadApplicationContext("autoDetectStackName");
		StackResourceRegistry staticStackNameProviderBasedStackResourceRegistry = applicationContext.getBean(StackResourceRegistry.class);

		// Act
		String physicalResourceId = staticStackNameProviderBasedStackResourceRegistry.lookupPhysicalResourceId("RdsSingleMicroInstance");

		// Assert
		assertThat(physicalResourceId, is(not(nullValue())));
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

	private static ClassPathXmlApplicationContext loadApplicationContext(String configurationName) {
		return new ClassPathXmlApplicationContext(StackResourceRegistryAwsTest.class.getSimpleName() + "-" + configurationName + ".xml", StackResourceRegistryAwsTest.class);
	}

}
