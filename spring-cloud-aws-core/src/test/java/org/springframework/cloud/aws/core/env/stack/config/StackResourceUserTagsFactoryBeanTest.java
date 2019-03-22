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

package org.springframework.cloud.aws.core.env.stack.config;

import java.util.Map;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.Tag;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class StackResourceUserTagsFactoryBeanTest {

	@Test
	public void getObject_stackWithTagsDefined_createTagsMap() throws Exception {
		// Arrange
		AmazonCloudFormation cloudFormation = mock(AmazonCloudFormation.class);
		StackNameProvider stackNameProvider = mock(StackNameProvider.class);

		when(stackNameProvider.getStackName()).thenReturn("testStack");
		when(cloudFormation
				.describeStacks(new DescribeStacksRequest().withStackName("testStack")))
						.thenReturn(new DescribeStacksResult().withStacks(new Stack()
								.withTags(new Tag().withKey("key1").withValue("value1"),
										new Tag().withKey("key2").withValue("value2"))));

		StackResourceUserTagsFactoryBean factoryBean = new StackResourceUserTagsFactoryBean(
				cloudFormation, stackNameProvider);

		// Act
		factoryBean.afterPropertiesSet();
		Map<String, String> factoryBeanObject = factoryBean.getObject();

		// Assert
		assertThat(factoryBeanObject.get("key1")).isEqualTo("value1");
		assertThat(factoryBeanObject.get("key2")).isEqualTo("value2");
	}

}
