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

package org.elasticspring.beans.factory.config.ec2;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceAttributeResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceAttribute;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.nio.charset.Charset;
import java.util.Properties;

/**
 *
 */
public class AmazonEC2PropertyPlaceHolderTest {

	@Test
	public void testResolveUserDataProperties() throws Exception {
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		InstanceIdProvider instanceIdProvider = Mockito.mock(InstanceIdProvider.class);
		AmazonEC2PropertyPlaceHolder amazonEC2PropertyPlaceHolder = getAmazonEC2PropertyPlaceHolder("secret", "access", amazonEC2, instanceIdProvider);
		amazonEC2PropertyPlaceHolder.setResolveUserTagsForInstance(false);

		Mockito.when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		DescribeInstanceAttributeRequest describeInstanceAttributeRequest = new DescribeInstanceAttributeRequest("1234567890",AmazonEC2PropertyPlaceHolder.USER_DATA_ATTRIBUTE_NAME);
		String encodedUserData = Base64.encodeBase64String("keyA:valueA;keyB:valueB".getBytes("UTF-8"));

		DescribeInstanceAttributeResult describeInstanceAttributeResult = new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute().withUserData(encodedUserData));
		Mockito.when(amazonEC2.describeInstanceAttribute(Matchers.refEq(describeInstanceAttributeRequest))).thenReturn(describeInstanceAttributeResult);
		amazonEC2PropertyPlaceHolder.afterPropertiesSet();

		Assert.assertEquals("valueA",amazonEC2PropertyPlaceHolder.resolvePlaceholder("keyA",null));
		Assert.assertEquals("valueB",amazonEC2PropertyPlaceHolder.resolvePlaceholder("keyB",null));
	}

	@Test
	public void testResolveUserWithCustomSeparatorsAndCharset() throws Exception {
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		InstanceIdProvider instanceIdProvider = Mockito.mock(InstanceIdProvider.class);
		AmazonEC2PropertyPlaceHolder amazonEC2PropertyPlaceHolder = getAmazonEC2PropertyPlaceHolder("secret", "access", amazonEC2, instanceIdProvider);
		amazonEC2PropertyPlaceHolder.setResolveUserTagsForInstance(false);
		amazonEC2PropertyPlaceHolder.setUserDataAttributeEncoding("ISO-8859-1");
		amazonEC2PropertyPlaceHolder.setUserDataAttributeSeparator(",");
		amazonEC2PropertyPlaceHolder.setValueSeparator("=");
		
		Mockito.when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		DescribeInstanceAttributeRequest describeInstanceAttributeRequest = new DescribeInstanceAttributeRequest("1234567890",AmazonEC2PropertyPlaceHolder.USER_DATA_ATTRIBUTE_NAME);
		String encodedUserData = Base64.encodeBase64String("keyA=valueA,keyB=valueÖ".getBytes(Charset.forName("ISO-8859-1")));
		DescribeInstanceAttributeResult describeInstanceAttributeResult = new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute().withUserData(encodedUserData));
		Mockito.when(amazonEC2.describeInstanceAttribute(Matchers.refEq(describeInstanceAttributeRequest))).thenReturn(describeInstanceAttributeResult);
		amazonEC2PropertyPlaceHolder.afterPropertiesSet();

		Assert.assertEquals("valueA",amazonEC2PropertyPlaceHolder.resolvePlaceholder("keyA",null));
		Assert.assertEquals("valueÖ",amazonEC2PropertyPlaceHolder.resolvePlaceholder("keyB",null));
	}

	@Test
	public void testResolveUserWithOutUserDataDefinedForInstance() throws Exception {
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		InstanceIdProvider instanceIdProvider = Mockito.mock(InstanceIdProvider.class);
		AmazonEC2PropertyPlaceHolder amazonEC2PropertyPlaceHolder = getAmazonEC2PropertyPlaceHolder("secret", "access", amazonEC2, instanceIdProvider);
		amazonEC2PropertyPlaceHolder.setResolveUserTagsForInstance(false);
		amazonEC2PropertyPlaceHolder.setUserDataAttributeEncoding("ISO-8859-1");
		amazonEC2PropertyPlaceHolder.setUserDataAttributeSeparator(",");
		amazonEC2PropertyPlaceHolder.setValueSeparator("=");

		Mockito.when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		DescribeInstanceAttributeRequest describeInstanceAttributeRequest = new DescribeInstanceAttributeRequest("1234567890",AmazonEC2PropertyPlaceHolder.USER_DATA_ATTRIBUTE_NAME);
		DescribeInstanceAttributeResult describeInstanceAttributeResult = new DescribeInstanceAttributeResult().withInstanceAttribute(new InstanceAttribute().withUserData(null));
		Mockito.when(amazonEC2.describeInstanceAttribute(Matchers.refEq(describeInstanceAttributeRequest))).thenReturn(describeInstanceAttributeResult);
		amazonEC2PropertyPlaceHolder.afterPropertiesSet();

		Assert.assertNull(amazonEC2PropertyPlaceHolder.resolvePlaceholder("keyA",new Properties()));
	}


	@Test
	public void testResolveUserTagProperties() throws Exception {
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		InstanceIdProvider instanceIdProvider = Mockito.mock(InstanceIdProvider.class);
		AmazonEC2PropertyPlaceHolder amazonEC2PropertyPlaceHolder = getAmazonEC2PropertyPlaceHolder("secret", "access", amazonEC2, instanceIdProvider);
		amazonEC2PropertyPlaceHolder.setResolveUserDataForInstance(false);

		Mockito.when(instanceIdProvider.getCurrentInstanceId()).thenReturn("1234567890");

		DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds("1234567890");

		Instance targetInstance = new Instance().withTags(new Tag("keyA","valueA"), new Tag("keyB","valueB")).withInstanceId("1234567890");
		Instance anotherInstanceInSameReservation = new Instance().withTags(new Tag("keyC","valueC")).withInstanceId("0987654321");
		Reservation reservation = new Reservation().withInstances(targetInstance,anotherInstanceInSameReservation);
		DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult().withReservations(reservation);

		Mockito.when(amazonEC2.describeInstances(Matchers.refEq(describeInstancesRequest))).thenReturn(describeInstancesResult);
		amazonEC2PropertyPlaceHolder.afterPropertiesSet();

		Assert.assertEquals("valueA",amazonEC2PropertyPlaceHolder.resolvePlaceholder("keyA",null));
		Assert.assertEquals("valueB",amazonEC2PropertyPlaceHolder.resolvePlaceholder("keyB",null));
		Assert.assertNull(amazonEC2PropertyPlaceHolder.resolvePlaceholder("keyC", new Properties()));
	}


	@Test
	public void testFallBackToDefaultProperties() throws Exception {
		AmazonEC2 amazonEC2 = Mockito.mock(AmazonEC2.class);
		InstanceIdProvider instanceIdProvider = Mockito.mock(InstanceIdProvider.class);
		AmazonEC2PropertyPlaceHolder amazonEC2PropertyPlaceHolder = getAmazonEC2PropertyPlaceHolder("secret", "access", amazonEC2, instanceIdProvider);
		amazonEC2PropertyPlaceHolder.setResolveUserDataForInstance(false);
		amazonEC2PropertyPlaceHolder.setResolveUserTagsForInstance(false);

		Properties properties = new Properties();
		properties.put("key","value");

		Assert.assertEquals("value",amazonEC2PropertyPlaceHolder.resolvePlaceholder("key",properties));
	}

	private AmazonEC2PropertyPlaceHolder getAmazonEC2PropertyPlaceHolder(String accessKey, String secretKey, final AmazonEC2 amazonEC2,
																		 InstanceIdProvider instanceIdProvider) {
		return new AmazonEC2PropertyPlaceHolder(accessKey, secretKey, instanceIdProvider) {

			public AmazonEC2 getAmazonEC2() {
				return amazonEC2;
			}
		};
	}
}
