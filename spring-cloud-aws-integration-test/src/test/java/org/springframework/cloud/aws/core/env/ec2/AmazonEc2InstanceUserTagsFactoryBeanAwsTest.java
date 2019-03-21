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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.cloud.aws.support.TestStackInstanceIdService;
import org.springframework.context.ConfigurableApplicationContext;
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
	private ConfigurableApplicationContext context;

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

		Assert.assertEquals("tagv1",
				this.context.getBeanFactory().getBeanExpressionResolver().evaluate(
						"#{instanceData['tag1']}",
						new BeanExpressionContext(this.context.getBeanFactory(), null)));
		Assert.assertEquals("tagv2",
				this.context.getBeanFactory().getBeanExpressionResolver().evaluate(
						"#{instanceData['tag2']}",
						new BeanExpressionContext(this.context.getBeanFactory(), null)));
		Assert.assertEquals("tagv3",
				this.context.getBeanFactory().getBeanExpressionResolver().evaluate(
						"#{instanceData['tag3']}",
						new BeanExpressionContext(this.context.getBeanFactory(), null)));
		Assert.assertEquals("tagv4",
				this.context.getBeanFactory().getBeanExpressionResolver().evaluate(
						"#{instanceData['tag4']}",
						new BeanExpressionContext(this.context.getBeanFactory(), null)));
	}

}
