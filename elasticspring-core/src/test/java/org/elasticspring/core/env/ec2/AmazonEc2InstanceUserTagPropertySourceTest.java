/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.env.ec2;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.TagDescription;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

/**
 * @author Agim Emruli
 */
public class AmazonEc2InstanceUserTagPropertySourceTest {

	@Test
	public void getProperty_userTagDataAvailable_resolvesKeys() throws Exception {
		//Arrange
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);

		InstanceIdProvider instanceIdProvider = Mockito.mock(InstanceIdProvider.class);
		Mockito.when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		DescribeTagsRequest describeTagsRequest = new DescribeTagsRequest().withFilters(
				new Filter("resource-id", Collections.singletonList("1234567890")),
				new Filter("resource-type", Collections.singletonList("instance")));

		DescribeTagsResult describeTagsResult = new DescribeTagsResult().withTags(
				new TagDescription().withKey("keyA").withResourceType(ResourceType.Instance).withValue("valueA"),
				new TagDescription().withKey("keyB").withResourceType(ResourceType.Instance).withValue("valueB")
		);

		Mockito.when(amazonEC2.describeTags(describeTagsRequest)).thenReturn(describeTagsResult);

		//Act
		AmazonEc2InstanceUserTagPropertySource amazonEc2InstanceUserTagPropertySource = new AmazonEc2InstanceUserTagPropertySource("test", amazonEC2, instanceIdProvider);

		//Assert
		Assert.assertEquals("valueA", amazonEc2InstanceUserTagPropertySource.getProperty("keyA"));
		Assert.assertEquals("valueB", amazonEc2InstanceUserTagPropertySource.getProperty("keyB"));
		Assert.assertNull(amazonEc2InstanceUserTagPropertySource.getProperty("keyC"));
	}
}