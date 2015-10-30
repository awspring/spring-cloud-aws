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
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsync;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link CloudWatchMetricSender} implementation that internally uses a queue
 * to buffer {@link MetricDatum} values. Each time a new values arrives a new {@link Runnable}
 * will be scheduled to send the metrics from the buffer if no {@link Runnable} has already been scheduled.
 * The schedule adds a little delay ({@link #nextRunDelayMillis}).
 * This way we try to increase the possibility to always send less requests with a higher payload.
 *
 * @author Simon Buettner
 */
public class BufferingCloudWatchMetricSender implements CloudWatchMetricSender {

    private static final Log logger = LogFactory.getLog(CloudWatchMetricWriter.class);

    /**
     * Maximum number of {@link MetricDatum} values per request
     * limit of CloudWatch.
     */
    private static final int MAX_METRIC_DATA_PER_REQUEST = 20;

   	private final AtomicBoolean senderRunning = new AtomicBoolean(false);

    private final ConcurrentLinkedQueue<MetricDatum> metricDataBuffer = new ConcurrentLinkedQueue<>();

    private final ScheduledExecutorService scheduledExecutorService;

    private final int maxBuffer;

    private final long nextRunDelayMillis;

    private final AmazonCloudWatchAsync amazonCloudWatchAsync;

    private final AsyncHandler<PutMetricDataRequest, Void> asyncHandler;

    private final String namespace;

    private final Runnable nextRun;

    public BufferingCloudWatchMetricSender(String namespace, int maxBuffer, long nextRunDelayMillis, AmazonCloudWatchAsync amazonCloudWatchAsync) {
        this(namespace, maxBuffer, nextRunDelayMillis, amazonCloudWatchAsync, Executors.newSingleThreadScheduledExecutor());
    }

    protected BufferingCloudWatchMetricSender(String namespace, int maxBuffer, long nextRunDelayMillis, final AmazonCloudWatchAsync amazonCloudWatchAsync, final ScheduledExecutorService scheduledExecutorService) {
        this.namespace = namespace.trim();
        this.maxBuffer = maxBuffer;
        this.nextRunDelayMillis = nextRunDelayMillis;
        this.amazonCloudWatchAsync = amazonCloudWatchAsync;
        this.scheduledExecutorService = scheduledExecutorService;

        // Create a single async handler which can be reused.
        this.asyncHandler = new AsyncHandler<PutMetricDataRequest, Void>() {
            @Override
            public void onError(Exception exception) {
                logger.error("Error sending metric data.", exception);
            }

            @Override
            public void onSuccess(PutMetricDataRequest request, Void aVoid) {

            }
        };

        // The next collect/send run.
        nextRun = new Runnable() {
            @Override
            public void run() {
				collectAndSend();
            }
        };
    }

    @Override
    public void send(MetricDatum metricDatum) {
        if(metricDataBuffer.size() < maxBuffer) {
            metricDataBuffer.add(metricDatum);
            scheduleNextRun();
        }
    }

    private void collectAndSend() {
		try {
			while(!metricDataBuffer.isEmpty()) {
				Collection<MetricDatum> metricData = collectNextMetricData();
				if(!metricData.isEmpty()) {
					sendToCloudWatch(metricData);
				}
			}
		}
		catch (Exception e) {
			logger.error("Error executing metric collection run.");
		}
		finally {
			senderRunning.set(false);
			if(!metricDataBuffer.isEmpty()) {
				scheduleNextRun();
			}
		}
    }

    private Collection<MetricDatum> collectNextMetricData() {
        Collection<MetricDatum> metricData = new ArrayList<>(MAX_METRIC_DATA_PER_REQUEST);
        while (metricData.size() < MAX_METRIC_DATA_PER_REQUEST) {
            MetricDatum metricDatum = metricDataBuffer.poll();
            if(metricDatum == null)  {
                break;
            }
            metricData.add(metricDatum);
        }
        return metricData;
    }

    private void sendToCloudWatch(Collection<MetricDatum> metricData) {
        PutMetricDataRequest putMetricDataRequest = new PutMetricDataRequest()
            .withNamespace(namespace)
            .withMetricData(metricData);
        amazonCloudWatchAsync.putMetricDataAsync(putMetricDataRequest, asyncHandler);
    }

    /**
     * Schedules the next sending run.
     */
    private void scheduleNextRun() {
        if(senderRunning.compareAndSet(false, true)) {
            scheduledExecutorService.schedule(nextRun, nextRunDelayMillis, TimeUnit.MILLISECONDS);
        }
    }

    protected ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    protected ConcurrentLinkedQueue<MetricDatum> getMetricDataBuffer() {
        return metricDataBuffer;
    }

    protected int getMaxBuffer() {
        return maxBuffer;
    }

    protected long getNextRunDelayMillis() {
        return nextRunDelayMillis;
    }

    protected String getNamespace() {
        return namespace;
    }

    protected Runnable getNextRun() {
        return nextRun;
    }
}
