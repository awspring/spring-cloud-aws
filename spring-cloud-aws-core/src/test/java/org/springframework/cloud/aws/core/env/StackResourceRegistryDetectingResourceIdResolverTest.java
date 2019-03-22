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

package org.springframework.cloud.aws.core.env;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StackResourceRegistryDetectingResourceIdResolverTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private static ListableBeanFactory makeListableBeanFactory(
			StackResourceRegistry... stackResourceRegistries) {
		Map<String, StackResourceRegistry> stackResourceRegistryMap = new HashMap<>();

		for (StackResourceRegistry stackResourceRegistry : stackResourceRegistries) {
			stackResourceRegistryMap.put(String.valueOf(stackResourceRegistry.hashCode()),
					stackResourceRegistry);
		}

		ListableBeanFactory listableBeanFactory = mock(ListableBeanFactory.class);
		when(listableBeanFactory.getBeansOfType(StackResourceRegistry.class))
				.thenReturn(stackResourceRegistryMap);

		return listableBeanFactory;
	}

	private static StackResourceRegistry makeStackResourceRegistry() {
		return makeStackResourceRegistry(null, null);
	}

	private static StackResourceRegistry makeStackResourceRegistry(
			String logicalResourceId, String physicalResourceId) {
		StackResourceRegistry stackResourceRegistry = mock(StackResourceRegistry.class);
		when(stackResourceRegistry.lookupPhysicalResourceId(logicalResourceId))
				.thenReturn(physicalResourceId);

		return stackResourceRegistry;
	}

	// @checkstyle:off
	@Test
	public void resolveToPhysicalResourceId_logicalResourceIdOfNonStackResourceAndNoStackResourceRegistryAvailable_returnsLogicalResourceIdAsPhysicalResourceId()
			throws Exception {
		// @checkstyle:on
		// Arrange
		StackResourceRegistryDetectingResourceIdResolver resourceIdResolver;
		resourceIdResolver = new StackResourceRegistryDetectingResourceIdResolver();
		resourceIdResolver.setBeanFactory(makeListableBeanFactory());
		resourceIdResolver.afterPropertiesSet();

		// Act
		String physicalResourceId = resourceIdResolver
				.resolveToPhysicalResourceId("logicalResourceId");

		// Assert
		assertThat(physicalResourceId).isEqualTo("logicalResourceId");
	}

	// @checkstyle:off
	@Test
	public void resolveToPhysicalResourceId_logicalResourceIdOfNonStackResourceAndStackResourceRegistryAvailable_returnsLogicalResourceIdAsPhysicalResourceId()
			throws Exception {
		// @checkstyle:on
		// Arrange
		StackResourceRegistryDetectingResourceIdResolver resourceIdResolver;
		resourceIdResolver = new StackResourceRegistryDetectingResourceIdResolver();
		resourceIdResolver
				.setBeanFactory(makeListableBeanFactory(makeStackResourceRegistry()));
		resourceIdResolver.afterPropertiesSet();

		// Act
		String physicalResourceId = resourceIdResolver
				.resolveToPhysicalResourceId("logicalResourceId");

		// Assert
		assertThat(physicalResourceId).isEqualTo("logicalResourceId");
	}

	// @checkstyle:off
	@Test
	public void resolveToPhysicalResourceId_logicalResourceIdOfStackResourceAndStackResourceRegistryAvailable_returnsPhysicalResourceIdFromStackResourceRegistry()
			throws Exception {
		// @checkstyle:on
		// Arrange
		StackResourceRegistryDetectingResourceIdResolver resourceIdResolver;
		resourceIdResolver = new StackResourceRegistryDetectingResourceIdResolver();
		resourceIdResolver.setBeanFactory(makeListableBeanFactory(
				makeStackResourceRegistry("logicalResourceId", "physicalResourceId")));
		resourceIdResolver.afterPropertiesSet();

		// Act
		String physicalResourceId = resourceIdResolver
				.resolveToPhysicalResourceId("logicalResourceId");

		// Assert
		assertThat(physicalResourceId).isEqualTo("physicalResourceId");
	}

	// @checkstyle:off
	@Test
	public void createInstance_multipleStackResourceRegistriesAvailable_throwsException()
			throws Exception {
		// @checkstyle:on
		// Arrange
		StackResourceRegistryDetectingResourceIdResolver resourceIdResolver;
		resourceIdResolver = new StackResourceRegistryDetectingResourceIdResolver();
		resourceIdResolver.setBeanFactory(makeListableBeanFactory(
				makeStackResourceRegistry(), makeStackResourceRegistry()));

		// Assert
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("Multiple stack resource registries found");

		// Act
		resourceIdResolver.afterPropertiesSet();
	}

}
