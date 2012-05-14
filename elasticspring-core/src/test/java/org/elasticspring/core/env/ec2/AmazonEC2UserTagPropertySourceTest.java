/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.core.env.ec2;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 */
public class AmazonEC2UserTagPropertySourceTest {

	@Test
	public void testResolveUserTagProperties() throws Exception {
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		InstanceIdProvider instanceIdProvider = Mockito.mock(InstanceIdProvider.class);
		AmazonEC2UserTagPropertySource amazonEC2UserTagPropertySource = new AmazonEC2UserTagPropertySource("test", amazonEC2);
		amazonEC2UserTagPropertySource.setInstanceIdProvider(instanceIdProvider);

		Mockito.when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds("1234567890");

		Instance targetInstance = new Instance().withTags(new Tag("keyA", "valueA"), new Tag("keyB", "valueB")).withInstanceId("1234567890");
		Instance anotherInstanceInSameReservation = new Instance().withTags(new Tag("keyC", "valueC")).withInstanceId("0987654321");
		Reservation reservation = new Reservation().withInstances(targetInstance, anotherInstanceInSameReservation);
		DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult().withReservations(reservation);

		Mockito.when(amazonEC2.describeInstances(describeInstancesRequest)).thenReturn(describeInstancesResult);

		Assert.assertEquals("valueA", amazonEC2UserTagPropertySource.getProperty("keyA"));
		Assert.assertEquals("valueB", amazonEC2UserTagPropertySource.getProperty("keyB"));
		Assert.assertNull(amazonEC2UserTagPropertySource.getProperty("keyC"));
	}

}
