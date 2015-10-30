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

package org.springframework.cloud.aws.autoconfigure.metrics;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.*;

/**
 * Test for the {@link CloudWatchMetricAutoConfiguration}.
 *
 * @author Simon Buettner
 */
public class CloudWatchMetricWriterAutoConfigurationTest {

	MockEnvironment env;

	AnnotationConfigApplicationContext context;

	@Before
	public void before() {
		env = new MockEnvironment();
		context = new AnnotationConfigApplicationContext();
		context.setEnvironment(env);
	}

	@Test
	public void testWithoutSettingAnyConfigProperties() {
		context.register(CloudWatchMetricAutoConfiguration.class);
		context.refresh();
		assertTrue(context.getBeansOfType(CloudWatchMetricWriter.class).isEmpty());
	}

	@Test
	public void testConfiguration() throws Exception {
		env.setProperty("cloud.aws.cloudwatch.namespace", "test");

		context.register(CloudWatchMetricAutoConfiguration.class);
		context.refresh();

		CloudWatchMetricWriter cloudWatchMetricWriter = context.getBean(CloudWatchMetricWriter.class);
		assertNotNull(cloudWatchMetricWriter);

		BufferingCloudWatchMetricSender cloudWatchMetricSender = context.getBean(BufferingCloudWatchMetricSender.class);
		assertNotNull(cloudWatchMetricSender);

		CloudWatchMetricProperties cloudWatchMetricProperties = context.getBean(CloudWatchMetricProperties.class);
		assertNotNull(cloudWatchMetricProperties);

		assertEquals(cloudWatchMetricSender.getNamespace(), cloudWatchMetricProperties.getNamespace());
		assertEquals(cloudWatchMetricSender.getMaxBuffer(), cloudWatchMetricProperties.getMaxBuffer());
		assertEquals(cloudWatchMetricSender.getNextRunDelayMillis(), cloudWatchMetricProperties.getNextRunDelayMillis());
	}

}