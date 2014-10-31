/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.core.env.ec2;

import org.springframework.cloud.aws.support.TestStackInstanceIdService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AmazonEc2InstanceUserTagsFactoryBeanAwsTest {

	@Autowired
	private TestStackInstanceIdService testStackInstanceIdService;

	@Autowired
	private SimpleConfigurationBean simpleConfigurationBean;

	@Before
	public void enableInstanceIdMetadataService() {
		this.testStackInstanceIdService.enable();
	}

	@After
	public void disableInstanceIdMetadataService() {
		this.testStackInstanceIdService.disable();
	}

	@Test
	public void testGetUserProperties() throws Exception {
		Assert.assertEquals("tagv1", this.simpleConfigurationBean.getValue1());
		Assert.assertEquals("tagv2", this.simpleConfigurationBean.getValue2());
		Assert.assertEquals("tagv3", this.simpleConfigurationBean.getValue3());
		Assert.assertEquals("tagv4", this.simpleConfigurationBean.getValue4());
	}
}
