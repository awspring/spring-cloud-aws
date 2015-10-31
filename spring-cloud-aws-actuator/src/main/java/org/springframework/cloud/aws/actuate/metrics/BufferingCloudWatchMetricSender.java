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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

/**
 * A {@link CloudWatchMetricSender} implementation that internally uses a queue
 * to buffer {@link MetricDatum} values.
 *
 * @author Simon Buettner
 * @author Agim Emruli
 * @since 1.1
 */
public class BufferingCloudWatchMetricSender implements CloudWatchMetricSender, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(CloudWatchMetricWriter.class);

	/**
	 * Maximum number of {@link MetricDatum} values per request
	 * limit of CloudWatch.
	 */
	private static final int MAX_METRIC_DATA_PER_REQUEST = 20;

	private final String namespace;

	private final int maxBuffer;

	private final long fixedDelayBetweenRuns;

	private final AmazonCloudWatchAsync amazonCloudWatchAsync;

	private final LinkedBlockingQueue<MetricDatum> metricDataBuffer;
	private final ScheduledFuture<?> scheduledFuture;

	public BufferingCloudWatchMetricSender(String namespace, int maxBuffer, long fixedDelayBetweenRuns, AmazonCloudWatchAsync amazonCloudWatchAsync) {
		Assert.hasText(namespace);
		this.namespace = namespace.trim();
		this.maxBuffer = maxBuffer;
		this.fixedDelayBetweenRuns = fixedDelayBetweenRuns;
		this.amazonCloudWatchAsync = amazonCloudWatchAsync;
		this.metricDataBuffer = new LinkedBlockingQueue<>(this.maxBuffer);
		TaskScheduler taskScheduler = new ConcurrentTaskScheduler();
		this.scheduledFuture = taskScheduler.scheduleWithFixedDelay(new CloudWatchMetricSenderRunnable(), this.fixedDelayBetweenRuns);
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
	public void destroy() throws Exception {
		this.scheduledFuture.cancel(true);
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
			BufferingCloudWatchMetricSender.this.amazonCloudWatchAsync.putMetricDataAsync(putMetricDataRequest, new AsyncHandler<PutMetricDataRequest, Void>() {

				@Override
				public void onError(Exception exception) {
					LOGGER.error("Error sending metric data.", exception);
				}

				@Override
				public void onSuccess(PutMetricDataRequest request, Void result) {
					LOGGER.debug("Published metric with namespace:{}", request.getNamespace());
				}
			});
		}
	}
}
