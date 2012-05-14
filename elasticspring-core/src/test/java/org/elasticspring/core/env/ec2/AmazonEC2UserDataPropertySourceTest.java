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
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeResult;
import com.amazonaws.services.ec2.model.InstanceAttribute;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.Charset;

/**
 *
 */
public class AmazonEC2UserDataPropertySourceTest {

	@Test
	public void testResolveUserDataProperties() throws Exception {
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		InstanceIdProvider instanceIdProvider = Mockito.mock(InstanceIdProvider.class);

		AmazonEC2UserDataPropertySource amazonEC2UserDataPropertySource = new AmazonEC2UserDataPropertySource("test",amazonEC2);
		amazonEC2UserDataPropertySource.setInstanceIdProvider(instanceIdProvider);

		Mockito.when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		//Request
		DescribeInstanceAttributeRequest describeInstanceAttributeRequest = new DescribeInstanceAttributeRequest("1234567890", AmazonEC2UserDataPropertySource.USER_DATA_ATTRIBUTE_NAME);

		//Result
		String encodedUserData = Base64.encodeBase64String("keyA:valueA;keyB:valueB".getBytes("UTF-8"));
		DescribeInstanceAttributeResult describeInstanceAttributeResult = new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute().withUserData(encodedUserData));

		Mockito.when(amazonEC2.describeInstanceAttribute(describeInstanceAttributeRequest)).thenReturn(describeInstanceAttributeResult);

		Assert.assertEquals("valueA", amazonEC2UserDataPropertySource.getProperty("keyA"));
		Assert.assertEquals("valueB", amazonEC2UserDataPropertySource.getProperty("keyB"));
	}

	@Test
	public void testResolveUserWithCustomSeparatorsAndCharset() throws Exception {
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		InstanceIdProvider instanceIdProvider = Mockito.mock(InstanceIdProvider.class);
		AmazonEC2UserDataPropertySource amazonEC2UserDataPropertySource = new AmazonEC2UserDataPropertySource("test",amazonEC2);
		amazonEC2UserDataPropertySource.setInstanceIdProvider(instanceIdProvider);

		amazonEC2UserDataPropertySource.setUserDataAttributeEncoding("ISO-8859-1");
		amazonEC2UserDataPropertySource.setUserDataAttributeSeparator(",");
		amazonEC2UserDataPropertySource.setUserDataValueSeparator("=");

		Mockito.when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		DescribeInstanceAttributeRequest describeInstanceAttributeRequest = new DescribeInstanceAttributeRequest("1234567890", AmazonEC2UserDataPropertySource.USER_DATA_ATTRIBUTE_NAME);
		String encodedUserData = Base64.encodeBase64String("keyA=valueA,keyB=valueÖ".getBytes(Charset.forName("ISO-8859-1")));
		DescribeInstanceAttributeResult describeInstanceAttributeResult = new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute().withUserData(encodedUserData));
		Mockito.when(amazonEC2.describeInstanceAttribute(describeInstanceAttributeRequest)).thenReturn(describeInstanceAttributeResult);

		Assert.assertEquals("valueA", amazonEC2UserDataPropertySource.getProperty("keyA"));
		Assert.assertEquals("valueÖ", amazonEC2UserDataPropertySource.getProperty("keyB"));
	}
}
