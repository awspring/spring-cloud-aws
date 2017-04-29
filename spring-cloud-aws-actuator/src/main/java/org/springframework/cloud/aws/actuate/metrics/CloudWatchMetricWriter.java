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
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

/**
 * {@link MetricWriter} implementation that writes metrics to Amazon CloudWatch.
 *
 * @author Simon Buettner
 */
public class CloudWatchMetricWriter implements MetricWriter {

    private final CloudWatchMetricSender sender;

    public CloudWatchMetricWriter(CloudWatchMetricSender sender) {
        this.sender = sender;
    }

    @Override
    public void increment(Delta<?> delta) {
        this.sender.send(createMetricDatumForCounterValue(delta));
    }

    @Override
    public void set(Metric<?> value) {
        this.sender.send(createMetricDatumForUnknownValue(value));
    }

    @Override
    public void reset(String metricName) {
        this.sender.send(createEmptyMetricDatum(metricName));
    }

    protected static MetricDatum createEmptyMetricDatum(String metricName) {
        return new MetricDatum().withMetricName(metricName).withValue(0.0);
    }

    protected static MetricDatum createMetricDatumForCounterValue(Delta<?> delta) {
        return new MetricDatum()
                .withMetricName(delta.getName())
                .withTimestamp(delta.getTimestamp())
                .withValue(delta.getValue().doubleValue())
                .withUnit(StandardUnit.Count);
    }

    protected static MetricDatum createMetricDatumForUnknownValue(Metric<?> value) {
        String name = value.getName();
        MetricDatum metricDatum = new MetricDatum()
                .withMetricName(name)
                .withTimestamp(value.getTimestamp());

        if (name.contains("timer.") && !name.contains("gauge.") && !name.contains("counter.")) {
            // Duration
            metricDatum
                    .withValue(value.getValue().doubleValue())
                    .withUnit(StandardUnit.Milliseconds);
        } else {
            // Simple value
            metricDatum
                    .withValue(value.getValue().doubleValue())
                    .withUnit(StandardUnit.Count);
        }

        return metricDatum;
    }

}
