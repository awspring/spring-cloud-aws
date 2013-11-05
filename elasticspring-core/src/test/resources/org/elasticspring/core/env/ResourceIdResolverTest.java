package org.elasticspring.core.env;

import org.elasticspring.core.env.stack.StackResourceRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.ListableBeanFactory;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceIdResolverTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void resolveToPhysicalResourceId_logicalResourceIdOfNonStackResourceAndNoStackResourceRegistryAvailable_returnsLogicalResourceIdAsPhysicalResourceId() throws Exception {
		// Arrange
		ResourceIdResolver resourceIdResolver = new ResourceIdResolver();
		resourceIdResolver.setBeanFactory(makeListableBeanFactory());
		resourceIdResolver.afterPropertiesSet();

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("logicalResourceId");

		// Assert
		assertThat(physicalResourceId, is("logicalResourceId"));
	}

	@Test
	public void resolveToPhysicalResourceId_logicalResourceIdOfNonStackResourceAndStackResourceRegistryAvailable_returnsLogicalResourceIdAsPhysicalResourceId() throws Exception {
		// Arrange
		ResourceIdResolver resourceIdResolver = new ResourceIdResolver();
		resourceIdResolver.setBeanFactory(makeListableBeanFactory(makeStackResourceRegistry()));
		resourceIdResolver.afterPropertiesSet();

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("logicalResourceId");

		// Assert
		assertThat(physicalResourceId, is("logicalResourceId"));
	}

	@Test
	public void resolveToPhysicalResourceId_logicalResourceIdOfStackResourceAndStackResourceRegistryAvailable_returnsPhysicalResourceIdFromStackResourceRegistry() throws Exception {
		// Arrange
		ResourceIdResolver resourceIdResolver = new ResourceIdResolver();
		resourceIdResolver.setBeanFactory(makeListableBeanFactory(makeStackResourceRegistry("logicalResourceId", "physicalResourceId")));
		resourceIdResolver.afterPropertiesSet();

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("logicalResourceId");

		// Assert
		assertThat(physicalResourceId, is("physicalResourceId"));
	}

	@Test
	public void createInstance_multipleStackResourceRegistriesAvailable_throwsException() throws Exception {
		// Arrange
		ResourceIdResolver resourceIdResolver = new ResourceIdResolver();
		resourceIdResolver.setBeanFactory(makeListableBeanFactory(makeStackResourceRegistry(), makeStackResourceRegistry()));

		// Assert
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("Multiple stack resource registries found");

		// Act
		resourceIdResolver.afterPropertiesSet();
	}

	private static ListableBeanFactory makeListableBeanFactory(StackResourceRegistry... stackResourceRegistries) {
		Map<String, StackResourceRegistry> stackResourceRegistryMap = new HashMap<String, StackResourceRegistry>();

		for (StackResourceRegistry stackResourceRegistry : stackResourceRegistries) {
			stackResourceRegistryMap.put(String.valueOf(stackResourceRegistry.hashCode()), stackResourceRegistry);
		}

		ListableBeanFactory listableBeanFactory = mock(ListableBeanFactory.class);
		when(listableBeanFactory.getBeansOfType(StackResourceRegistry.class)).thenReturn(stackResourceRegistryMap);

		return listableBeanFactory;
	}

	private static StackResourceRegistry makeStackResourceRegistry() {
		return makeStackResourceRegistry(null, null);
	}

	private static StackResourceRegistry makeStackResourceRegistry(String logicalResourceId, String physicalResourceId) {
		StackResourceRegistry stackResourceRegistry = mock(StackResourceRegistry.class);
		when(stackResourceRegistry.lookupPhysicalResourceId(logicalResourceId)).thenReturn(physicalResourceId);

		return stackResourceRegistry;
	}

}
