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

package org.springframework.cloud.aws.actuate.metrics;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link BufferingCloudWatchMetricSenderTest}.
 *
 * @author Simon Buettner
 * @author Agim Emruli
 */
public class BufferingCloudWatchMetricSenderTest {

    @Captor
    private ArgumentCaptor<PutMetricDataRequest> putRequestCaptor;

    @Mock
    private AmazonCloudWatchAsyncClient amazonCloudWatchAsyncClient;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void resetMocks() {
        Mockito.reset(this.amazonCloudWatchAsyncClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void send_withMetricData_sendsDataToCloudFormation() throws Exception {
        BufferingCloudWatchMetricSender sender = new BufferingCloudWatchMetricSender("test", 10, 1, this.amazonCloudWatchAsyncClient);
        MetricDatum metric = new MetricDatum().withMetricName("test");
        sender.afterPropertiesSet();
        sender.start();

        sender.send(metric);

        // Wait
        sender.stop();

        verify(this.amazonCloudWatchAsyncClient, times(1)).putMetricDataAsync(this.putRequestCaptor.capture(), (AsyncHandler) any());
        assertEquals(metric.getMetricName(), this.putRequestCaptor.getValue().getMetricData().get(0).getMetricName());

        sender.destroy();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBufferLimit() throws Exception {
        int maxBuffer = 40;

        BufferingCloudWatchMetricSender sender = new BufferingCloudWatchMetricSender("test", maxBuffer, 4, this.amazonCloudWatchAsyncClient);
        sender.afterPropertiesSet();
        sender.start();

        // Add
        int metrics = 50;
        for (int i = 0; i < metrics; i++) {
            sender.send(new MetricDatum().withMetricName("test"));
        }

        sender.stop();

        verify(this.amazonCloudWatchAsyncClient, times(3)).putMetricDataAsync(this.putRequestCaptor.capture(), (AsyncHandler) any());
        assertEquals("There should be three requests", 3, this.putRequestCaptor.getAllValues().size());
        assertEquals("50 metrics should be placed in three requests", maxBuffer, this.putRequestCaptor.getAllValues().get(0).getMetricData().size() + this.putRequestCaptor.getAllValues().get(1).getMetricData().size());

        sender.destroy();
    }

}
