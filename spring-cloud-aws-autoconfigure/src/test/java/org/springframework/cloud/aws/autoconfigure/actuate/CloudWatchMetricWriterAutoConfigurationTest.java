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

package org.springframework.cloud.aws.autoconfigure.actuate;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.aws.actuate.metrics.BufferingCloudWatchMetricSender;
import org.springframework.cloud.aws.actuate.metrics.CloudWatchMetricWriter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for the {@link CloudWatchMetricAutoConfiguration}.
 *
 * @author Simon Buettner
 */
public class CloudWatchMetricWriterAutoConfigurationTest {

    private MockEnvironment env;

    private AnnotationConfigApplicationContext context;

    @Before
    public void before() {
        this.env = new MockEnvironment();
        this.context = new AnnotationConfigApplicationContext();
        this.context.setEnvironment(this.env);
    }

    @Test
    public void testWithoutSettingAnyConfigProperties() {
        this.context.register(CloudWatchMetricAutoConfiguration.class);
        this.context.refresh();
        assertTrue(this.context.getBeansOfType(CloudWatchMetricWriter.class).isEmpty());
    }

    @Test
    public void testConfiguration() throws Exception {
        this.env.setProperty("cloud.aws.cloudwatch.namespace", "test");

        this.context.register(CloudWatchMetricAutoConfiguration.class);
        this.context.refresh();

        CloudWatchMetricWriter cloudWatchMetricWriter = this.context.getBean(CloudWatchMetricWriter.class);
        assertNotNull(cloudWatchMetricWriter);

        BufferingCloudWatchMetricSender cloudWatchMetricSender = this.context.getBean(BufferingCloudWatchMetricSender.class);
        assertNotNull(cloudWatchMetricSender);

        CloudWatchMetricProperties cloudWatchMetricProperties = this.context.getBean(CloudWatchMetricProperties.class);
        assertNotNull(cloudWatchMetricProperties);

        assertEquals(cloudWatchMetricSender.getNamespace(), cloudWatchMetricProperties.getNamespace());
        assertEquals(cloudWatchMetricSender.getMaxBuffer(), cloudWatchMetricProperties.getMaxBuffer());
        assertEquals(cloudWatchMetricSender.getFixedDelayBetweenRuns(), cloudWatchMetricProperties.getFixedDelayBetweenRuns());
    }

}
