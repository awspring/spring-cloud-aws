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

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link CloudWatchMetricWriter}.
 *
 * @author Simon Buettner
 */
public class CloudWatchMetricWriterTest {

    @Test
    public void testMetricDatumCreation() {
        Metric<Number> simpleMetric = new Metric<Number>("test.x", 1);
        MetricDatum simpleMetricDatum = CloudWatchMetricWriter.createMetricDatumForUnknownValue(simpleMetric);
        assertEquals(simpleMetric.getName(), simpleMetricDatum.getMetricName());
        assertEquals(simpleMetric.getTimestamp(), simpleMetricDatum.getTimestamp());
        assertEquals(Double.valueOf(simpleMetric.getValue().doubleValue()), simpleMetricDatum.getValue());
        assertEquals(StandardUnit.Count.toString(), simpleMetricDatum.getUnit());
    }

    @Test
    public void testTimerMetricDatumCreation() {
        Metric<Number> timerMetric = new Metric<Number>("timer.x", 1);
        MetricDatum simpleMetricDatum = CloudWatchMetricWriter.createMetricDatumForUnknownValue(timerMetric);
        assertEquals(timerMetric.getName(), simpleMetricDatum.getMetricName());
        assertEquals(timerMetric.getTimestamp(), simpleMetricDatum.getTimestamp());
        assertEquals(Double.valueOf(timerMetric.getValue().doubleValue()), simpleMetricDatum.getValue());
        assertEquals(StandardUnit.Milliseconds.toString(), simpleMetricDatum.getUnit());
    }

    @Test
    public void testCounterMetricDatumCreation() {
        Delta<Integer> counterMetric = new Delta<>("test.x", 1);
        MetricDatum counterMetricDatum = CloudWatchMetricWriter.createMetricDatumForCounterValue(counterMetric);
        assertEquals(counterMetric.getName(), counterMetricDatum.getMetricName());
        assertEquals(counterMetric.getTimestamp(), counterMetricDatum.getTimestamp());
        assertEquals(Double.valueOf(counterMetric.getValue().doubleValue()), counterMetricDatum.getValue());
        assertEquals(StandardUnit.Count.toString(), counterMetricDatum.getUnit());
    }

    @Test
    public void testSenderCommunication() {
        CloudWatchMetricSender sender = mock(CloudWatchMetricSender.class);
        CloudWatchMetricWriter writer = new CloudWatchMetricWriter(sender);

        // Simple metric
        Metric<Number> simpleMetric = new Metric<Number>("test.x", 1);
        writer.set(simpleMetric);
        verify(sender).send(CloudWatchMetricWriter.createMetricDatumForUnknownValue(simpleMetric));

        // Timer metric
        Metric<Number> timerMetric = new Metric<Number>("timer.x", 1);
        writer.set(timerMetric);
        verify(sender).send(CloudWatchMetricWriter.createMetricDatumForUnknownValue(timerMetric));

        // Counter metric
        Delta<Integer> counterMetric = new Delta<>("test.c", 1);
        writer.increment(counterMetric);
        verify(sender).send(CloudWatchMetricWriter.createMetricDatumForCounterValue(counterMetric));
    }

}
