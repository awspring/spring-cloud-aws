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
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link CloudWatchMetricSender} implementation that internally uses a queue
 * to buffer {@link MetricDatum} values.
 *
 * @author Simon Buettner
 * @author Agim Emruli
 * @since 1.1
 */
public class BufferingCloudWatchMetricSender implements CloudWatchMetricSender, InitializingBean, DisposableBean, SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudWatchMetricWriter.class);

    /**
     * Maximum number of {@link MetricDatum} values per request
     * limit of CloudWatch.
     */
    private static final int MAX_METRIC_DATA_PER_REQUEST = 20;
    private static final int FLUSH_TIMEOUT = 1000;

    private final String namespace;

    private final int maxBuffer;

    private final long fixedDelayBetweenRuns;

    private final AmazonCloudWatchAsync amazonCloudWatchAsync;

    private final LinkedBlockingQueue<MetricDatum> metricDataBuffer;
    private ScheduledFuture<?> scheduledFuture;

    private ThreadPoolTaskScheduler taskScheduler;

    public BufferingCloudWatchMetricSender(String namespace, int maxBuffer, long fixedDelayBetweenRuns, AmazonCloudWatchAsync amazonCloudWatchAsync) {
        Assert.hasText(namespace, "Namespace must not be null");
        this.namespace = namespace.trim();
        this.maxBuffer = maxBuffer;
        this.fixedDelayBetweenRuns = fixedDelayBetweenRuns;
        this.amazonCloudWatchAsync = amazonCloudWatchAsync;
        this.metricDataBuffer = new LinkedBlockingQueue<>(this.maxBuffer);
    }

    @Override
    public void send(MetricDatum metricDatum) {
        try {
            this.metricDataBuffer.put(metricDatum);
        } catch (InterruptedException e) {
            LOGGER.error("Error adding metric to queue", e);
            Thread.currentThread().interrupt();
        }
    }

    public String getNamespace() {
        return this.namespace;
    }

    public int getMaxBuffer() {
        return this.maxBuffer;
    }

    public long getFixedDelayBetweenRuns() {
        return this.fixedDelayBetweenRuns;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.afterPropertiesSet();
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void destroy() throws Exception {
        this.taskScheduler.destroy();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        this.stop();
        callback.run();
    }

    @Override
    public void start() {
        this.scheduledFuture = this.taskScheduler.scheduleWithFixedDelay(new CloudWatchMetricSenderRunnable(), this.fixedDelayBetweenRuns);
    }

    @Override
    public void stop() {
        if (!this.scheduledFuture.isCancelled()) {
            this.scheduledFuture.cancel(false);
        }
        flushMetrics();
    }

    private void flushMetrics() {
        Future<?> future = this.taskScheduler.submit(new CloudWatchMetricSenderRunnable());
        try {
            future.get(FLUSH_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Error flushing metrics", e);
        }
    }

    @Override
    public boolean isRunning() {
        return this.scheduledFuture != null && !this.scheduledFuture.isCancelled() && !this.scheduledFuture.isDone();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private class CloudWatchMetricSenderRunnable implements Runnable {

        @Override
        public void run() {
            try {
                while (!BufferingCloudWatchMetricSender.this.metricDataBuffer.isEmpty()) {
                    Collection<MetricDatum> metricData = collectNextMetricData();
                    if (!metricData.isEmpty()) {
                        sendToCloudWatch(metricData);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error executing metric collection run.", e);
            }
        }

        private Collection<MetricDatum> collectNextMetricData() {
            Collection<MetricDatum> metricData = new ArrayList<>(MAX_METRIC_DATA_PER_REQUEST);
            BufferingCloudWatchMetricSender.this.metricDataBuffer.drainTo(metricData, MAX_METRIC_DATA_PER_REQUEST);
            return metricData;
        }

        private void sendToCloudWatch(Collection<MetricDatum> metricData) {
            PutMetricDataRequest putMetricDataRequest = new PutMetricDataRequest()
                    .withNamespace(BufferingCloudWatchMetricSender.this.namespace)
                    .withMetricData(metricData);
            BufferingCloudWatchMetricSender.this.amazonCloudWatchAsync.putMetricDataAsync(putMetricDataRequest, new AsyncHandler<PutMetricDataRequest, PutMetricDataResult>() {

                @Override
                public void onError(Exception exception) {
                    LOGGER.error("Error sending metric data.", exception);
                }

                @Override
                public void onSuccess(PutMetricDataRequest request, PutMetricDataResult result) {
                    LOGGER.debug("Published metric with namespace:{}", request.getNamespace());
                }
            });
        }
    }
}
