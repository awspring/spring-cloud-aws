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

package org.springframework.cloud.aws.metric;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.writer.DefaultCounterService;
import org.springframework.boot.actuate.metrics.writer.DefaultGaugeService;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.actuate.metrics.BufferingCloudWatchMetricSender;
import org.springframework.cloud.aws.autoconfigure.cache.ElastiCacheAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Agim Emruli
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = MetricExporterTest.BootMetricExporterConfig.class,
        properties = {"cloud.aws.cloudwatch.namespace=test", "cloud.aws.cloudwatch.nextRunDelayMillis=10"})
public class MetricExporterTest {

    @Autowired
    private AmazonCloudWatch amazonCloudWatch;

    @Autowired
    private CounterService counterService;

    @Autowired
    private GaugeService gaugeService;

    @Autowired
    private BufferingCloudWatchMetricSender bufferingCloudWatchMetricSender;

    @Before
    public void startMetriCSenderIfNecessary() throws Exception {
        if (!this.bufferingCloudWatchMetricSender.isRunning()) {
            this.bufferingCloudWatchMetricSender.start();
        }
    }

    @Test
    public void resetIncrementDecrementMetrics() throws Exception {
        this.counterService.reset("metricExporterTest");
        this.counterService.increment("metricExporterTest");
        this.counterService.increment("metricExporterTest");
        this.counterService.decrement("metricExporterTest");
        this.counterService.increment("metricExporterTest");
        this.counterService.increment("metricExporterTest");
        this.counterService.increment("metricExporterTest");
        this.counterService.increment("metricExporterTest");

        Thread.sleep(this.bufferingCloudWatchMetricSender.getFixedDelayBetweenRuns());
        this.bufferingCloudWatchMetricSender.stop();

        ListMetricsResult listMetricsResult = this.amazonCloudWatch.listMetrics(new ListMetricsRequest().withNamespace("test").withMetricName("counter.metricExporterTest"));
        Assert.assertEquals(1, listMetricsResult.getMetrics().size());
    }

    @Test
    public void submitMetricsToGaugeService() throws Exception {
        this.gaugeService.submit("gaugeService", 23);
        this.gaugeService.submit("gaugeService", 24);
        this.gaugeService.submit("gaugeService", 22);
        this.gaugeService.submit("gaugeService", 19);

        Thread.sleep(this.bufferingCloudWatchMetricSender.getFixedDelayBetweenRuns());
        this.bufferingCloudWatchMetricSender.stop();

        ListMetricsResult listMetricsResult = this.amazonCloudWatch.listMetrics(new ListMetricsRequest().withNamespace("test").withMetricName("gaugeService"));
        Assert.assertEquals(1, listMetricsResult.getMetrics().size());
    }

    @SpringBootApplication(exclude = ElastiCacheAutoConfiguration.class)
    @PropertySource({"classpath:Integration-test-config.properties", "file://${els.config.dir}/access.properties"})
    static class BootMetricExporterConfig {

        @Bean
        public CounterService counterService(MetricWriter metricWriter) {
            return new DefaultCounterService(metricWriter);
        }

        @Bean
        public GaugeService gaugeService(MetricWriter metricWriter) {
            return new DefaultGaugeService(metricWriter);
        }
    }
}
