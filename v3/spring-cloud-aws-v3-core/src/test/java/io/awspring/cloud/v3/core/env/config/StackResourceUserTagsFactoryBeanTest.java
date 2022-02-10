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

package io.awspring.cloud.v3.core.env.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import io.awspring.cloud.v3.core.env.stack.config.StackNameProvider;
import io.awspring.cloud.v3.core.env.stack.config.StackResourceUserTagsFactoryBean;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;

/**
 * @author Agim Emruli
 */
class StackResourceUserTagsFactoryBeanTest {

	@Test
	void getObject_stackWithTagsDefined_createTagsMap() throws Exception {
		// Arrange
		CloudFormationClient cloudFormation = mock(CloudFormationClient.class);
		StackNameProvider stackNameProvider = mock(StackNameProvider.class);

		when(stackNameProvider.getStackName()).thenReturn("testStack");
		when(cloudFormation.describeStacks(
			DescribeStacksRequest.builder()
				.stackName("testStack")
				.build()))
			.thenReturn(DescribeStacksResponse.builder()
				.stacks(Stack.builder()
					.tags(
						Tag.builder()
							.key("key1")
							.value("value1")
							.build(),
						Tag.builder()
							.key("key2")
							.value("value2")
							.build()
						)
					.build())
				.build());

		StackResourceUserTagsFactoryBean factoryBean = new StackResourceUserTagsFactoryBean(cloudFormation,
				stackNameProvider);

		// Act
		factoryBean.afterPropertiesSet();
		Map<String, String> factoryBeanObject = factoryBean.getObject();

		// Assert
		assertThat(factoryBeanObject.get("key1")).isEqualTo("value1");
		assertThat(factoryBeanObject.get("key2")).isEqualTo("value2");
	}

}
