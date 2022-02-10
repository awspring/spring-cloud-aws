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

package io.awspring.cloud.v3.core.env.ec2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import io.awspring.cloud.v3.core.env.ec2.AmazonEc2InstanceUserTagsFactoryBean;
import io.awspring.cloud.v3.core.env.ec2.InstanceIdProvider;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.TagDescription;

/**
 * @author Agim Emruli
 */
class AmazonEc2InstanceUserTagsFactoryBeanTest {

	@Test
	void getObject_userTagDataAvailable_objectContainsAllAvailableKeys() throws Exception {
		// Arrange
		Ec2Client amazonEC2 = mock(Ec2Client.class);

		InstanceIdProvider instanceIdProvider = mock(InstanceIdProvider.class);
		when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		DescribeTagsRequest describeTagsRequest =
			DescribeTagsRequest.builder()
				.filters(
					Filter.builder()
						.name("resource-id")
						.values("1234567890")
						.build(),
					Filter.builder()
						.name("resource-type")
						.values("instance")
						.build())
				.build();

		DescribeTagsResponse describeTagsResponse = DescribeTagsResponse.builder()
			.tags(
				TagDescription.builder()
					.key("keyA")
					.resourceType(ResourceType.INSTANCE)
					.value("valueA")
				.build(),
				TagDescription.builder()
					.key("keyB")
					.resourceType(ResourceType.INSTANCE)
					.value("valueB")
					.build())
			.build();

		when(amazonEC2.describeTags(describeTagsRequest)).thenReturn(describeTagsResponse);

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
