/*
 * Copyright 2013-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.awspring.cloud.autoconfigure.core;

import io.awspring.cloud.autoconfigure.AwsClientProperties;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.metrics.MetricLevel;

/**
 * Properties related to publishing the metrics of the AWS SDK clients to CloudWatch. The endpoint and region properties
 * apply to the CloudWatch client of the metric publisher.
 *
 * @author Sharang Gupta
 * @since 4.3.0
 */
@ConfigurationProperties(prefix = ClientMetricsProperties.PREFIX)
public class ClientMetricsProperties extends AwsClientProperties {

	/**
	 * The prefix used for the properties of the AWS SDK client metrics.
	 */
	public static final String PREFIX = "spring.cloud.aws.client.metrics";

	/**
	 * Enables publishing the metrics of the AWS SDK clients to CloudWatch when
	 * software.amazon.awssdk:cloudwatch-metric-publisher is on the classpath.
	 */
	private boolean enabled = true;

	/**
	 * CloudWatch namespace of the metrics. The AWS SDK default is "AwsSdk/JavaSdk2".
	 */
	@Nullable
	private String namespace;

	/**
	 * How often the metrics are uploaded to CloudWatch. The AWS SDK default is one minute.
	 */
	@Nullable
	private Duration uploadFrequency;

	/**
	 * Maximum number of PutMetricData calls per upload. The AWS SDK default is 10.
	 */
	@Nullable
	private Integer maximumCallsPerUpload;

	/**
	 * Level of the metrics to publish. The AWS SDK default is INFO.
	 */
	@Nullable
	private MetricLevel metricLevel;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Nullable
	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(@Nullable String namespace) {
		this.namespace = namespace;
	}

	@Nullable
	public Duration getUploadFrequency() {
		return this.uploadFrequency;
	}

	public void setUploadFrequency(@Nullable Duration uploadFrequency) {
		this.uploadFrequency = uploadFrequency;
	}

	@Nullable
	public Integer getMaximumCallsPerUpload() {
		return this.maximumCallsPerUpload;
	}

	public void setMaximumCallsPerUpload(@Nullable Integer maximumCallsPerUpload) {
		this.maximumCallsPerUpload = maximumCallsPerUpload;
	}

	@Nullable
	public MetricLevel getMetricLevel() {
		return this.metricLevel;
	}

	public void setMetricLevel(@Nullable MetricLevel metricLevel) {
		this.metricLevel = metricLevel;
	}

}
