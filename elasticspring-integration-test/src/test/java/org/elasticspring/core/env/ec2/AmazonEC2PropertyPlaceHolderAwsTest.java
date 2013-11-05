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
import org.elasticspring.support.TestStackEnvironment;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("AmazonEC2PropertyPlaceHolderAwsTest-context.xml")
public class AmazonEC2PropertyPlaceHolderAwsTest {

	@Autowired
	private AmazonEC2 amazonEC2Client;

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@Test
	public void testGetUserProperties() throws Exception {
		AmazonEC2UserTagPropertySource amazonEC2PropertySource = new AmazonEC2UserTagPropertySource("userTagPropertySource", this.amazonEC2Client);

		Assert.assertEquals("tagv1", amazonEC2PropertySource.getProperty("tag1").toString());
		Assert.assertEquals("tagv2", amazonEC2PropertySource.getProperty("tag2").toString());
		Assert.assertEquals("tagv3", amazonEC2PropertySource.getProperty("tag3").toString());
		Assert.assertEquals("tagv4", amazonEC2PropertySource.getProperty("tag4").toString());
	}

}
