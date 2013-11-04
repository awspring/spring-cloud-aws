package org.elasticspring.core.env;

import org.elasticspring.core.env.stack.StackResourceRegistry;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceIdResolverTest {

	@Test
	public void resolveToPhysicalResourceId_logicalResourceIdOfNonStackResourceAndNoStackResourceRegistry_returnsLogicalResourceIdAsPhysicalResourceId() {
		// Arrange
		ResourceIdResolver resourceIdResolver = new ResourceIdResolver(null);

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("logicalResourceId");

		// Assert
		assertThat(physicalResourceId, is("logicalResourceId"));
	}

	@Test
	public void resolveToPhysicalResourceId_logicalResourceIdOfNonStackResourceAndStackResourceRegistry_returnsLogicalResourceIdAsPhysicalResourceId() {
		// Arrange
		StackResourceRegistry stackResourceRegistry = makeStackResourceRegistry();
		ResourceIdResolver resourceIdResolver = new ResourceIdResolver(stackResourceRegistry);

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("logicalResourceId");

		// Assert
		assertThat(physicalResourceId, is("logicalResourceId"));
	}

	@Test
	public void resolveToPhysicalResourceId_logicalResourceIdOfStackResourceAndStackResourceRegistry_returnsPhysicalResourceIdFromStackResourceRegistry() {
		// Arrange
		StackResourceRegistry stackResourceRegistry = makeStackResourceRegistry("logicalResourceId", "physicalResourceId");
		ResourceIdResolver resourceIdResolver = new ResourceIdResolver(stackResourceRegistry);

		// Act
		String physicalResourceId = resourceIdResolver.resolveToPhysicalResourceId("logicalResourceId");

		// Assert
		assertThat(physicalResourceId, is("physicalResourceId"));
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
