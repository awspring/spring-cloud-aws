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

package org.springframework.cloud.aws.core.env.ec2;

import java.util.Collections;
import java.util.Map;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.TagDescription;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class AmazonEc2InstanceUserTagsFactoryBeanTest {

	@Test
	public void getObject_userTagDataAvailable_objectContainsAllAvailableKeys()
			throws Exception {
		// Arrange
		AmazonEC2 amazonEC2 = mock(AmazonEC2.class);

		InstanceIdProvider instanceIdProvider = mock(InstanceIdProvider.class);
		when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		DescribeTagsRequest describeTagsRequest = new DescribeTagsRequest().withFilters(
				new Filter("resource-id", Collections.singletonList("1234567890")),
				new Filter("resource-type", Collections.singletonList("instance")));

		DescribeTagsResult describeTagsResult = new DescribeTagsResult().withTags(
				new TagDescription().withKey("keyA")
						.withResourceType(ResourceType.Instance).withValue("valueA"),
				new TagDescription().withKey("keyB")
						.withResourceType(ResourceType.Instance).withValue("valueB"));

		when(amazonEC2.describeTags(describeTagsRequest)).thenReturn(describeTagsResult);

		AmazonEc2InstanceUserTagsFactoryBean amazonEc2InstanceUserTagsFactoryBean = new AmazonEc2InstanceUserTagsFactoryBean(
				amazonEC2, instanceIdProvider);

		// Act
		amazonEc2InstanceUserTagsFactoryBean.afterPropertiesSet();
		Map<String, String> resultMap = amazonEc2InstanceUserTagsFactoryBean.getObject();

		// Assert
		assertThat(resultMap.get("keyA")).isEqualTo("valueA");
		assertThat(resultMap.get("keyB")).isEqualTo("valueB");
		assertThat(resultMap.containsKey("keyC")).isFalse();
	}

}
