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

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for the {@link BufferingCloudWatchMetricSenderTest}.
 *
 * @author Simon Buettner
 */
public class BufferingCloudWatchMetricSenderTest {

    @Captor
    ArgumentCaptor<PutMetricDataRequest> putRequestCaptor;

    AmazonCloudWatchAsyncClient amazonCloudWatchAsyncClient;

    @Before
    public void init(){
        MockitoAnnotations.initMocks(this);
        amazonCloudWatchAsyncClient = mock(AmazonCloudWatchAsyncClient.class);
    }

    @Test
	@SuppressWarnings("unchecked")
    public void testSending() throws Exception {
        BufferingCloudWatchMetricSender sender = new BufferingCloudWatchMetricSender("test", 10, 0, amazonCloudWatchAsyncClient);
        MetricDatum metric = new MetricDatum().withMetricName("test");
        sender.send(metric);

        // Wait
        ScheduledExecutorService scheduledExecutorService = sender.getScheduledExecutorService();
        scheduledExecutorService.shutdown();
        scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS);

        verify(amazonCloudWatchAsyncClient).putMetricDataAsync(putRequestCaptor.capture(), (AsyncHandler) any());
        assertEquals(metric.getMetricName(), putRequestCaptor.getValue().getMetricData().get(0).getMetricName());
    }

    @Test
    public void testScheduling() throws Exception {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        BufferingCloudWatchMetricSender sender = new BufferingCloudWatchMetricSender("test", 10, 0, amazonCloudWatchAsyncClient, scheduledExecutorService);

        // First metric should start a collection run.
        sender.send(new MetricDatum().withMetricName("test"));
        assertEquals(1, sender.getMetricDataBuffer().size());
        verify(scheduledExecutorService, times(1)).schedule(sender.getNextRun(), sender.getNextRunDelayMillis(), TimeUnit.MILLISECONDS);

        // Since we dont run the runnable another one should not be submitted but the value should still be buffered
        sender.send(new MetricDatum().withMetricName("test"));
        assertEquals(2, sender.getMetricDataBuffer().size());
        verify(scheduledExecutorService, times(1)).schedule(sender.getNextRun(), sender.getNextRunDelayMillis(), TimeUnit.MILLISECONDS);

        // When we run it, the buffer should be empty and we will be able to accept new runnables
        sender.getNextRun().run();
        assertEquals(0, sender.getMetricDataBuffer().size());

        // Next run should start another run
        sender.send(new MetricDatum().withMetricName("test"));
        assertEquals(1, sender.getMetricDataBuffer().size());
        verify(scheduledExecutorService, times(2)).schedule(sender.getNextRun(), sender.getNextRunDelayMillis(), TimeUnit.MILLISECONDS);
    }

    @Test
	@SuppressWarnings("unchecked")
	public void testBufferLimit() throws Exception {
        int maxBuffer = 40;
        int metrics = 50;

        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        BufferingCloudWatchMetricSender sender = new BufferingCloudWatchMetricSender("test", maxBuffer, 0, amazonCloudWatchAsyncClient, scheduledExecutorService);

        // Add
        for (int i = 0; i < metrics; i++) {
            sender.send(new MetricDatum().withMetricName("test"));
        }

        assertEquals("The internal buffer should never exceed maxBuffer", maxBuffer, sender.getMetricDataBuffer().size());
        verify(scheduledExecutorService, times(1)).schedule(sender.getNextRun(), sender.getNextRunDelayMillis(), TimeUnit.MILLISECONDS);

        sender.getNextRun().run();

        verify(amazonCloudWatchAsyncClient, times(2)).putMetricDataAsync(putRequestCaptor.capture(), (AsyncHandler) any());
        assertEquals("There should be two requests", 2, putRequestCaptor.getAllValues().size());
        assertEquals("40 metrics should be placed in two requests", maxBuffer, putRequestCaptor.getAllValues().get(0).getMetricData().size() + putRequestCaptor.getAllValues().get(1).getMetricData().size());
    }

}
