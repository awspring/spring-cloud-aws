/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.autoconfigure.kinesis;

import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import io.awspring.cloud.autoconfigure.AwsClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import static io.awspring.cloud.autoconfigure.kinesis.KinesisProperties.PREFIX;


/**
 * Properties related to KinesisProducer
 *
 * @author Matej Nedic
 * @since 4.0.0
 */
@ConfigurationProperties(prefix = PREFIX)
public class KinesisProducerProperties extends AwsClientProperties {

	/**
	 * The prefix used for AWS Kinesis configuration.
	 */
	public static final String PREFIX = "spring.cloud.aws.kinesis.producer";

	/**
	 * Whether aggregation of user records is enabled.
	 */
	private Boolean aggregationEnabled;

	/**
	 * Maximum number of user records to aggregate. Must be between 1 and Long.MAX_VALUE.
	 */
	private Long aggregationMaxCount;

	/**
	 * Maximum size in bytes of aggregated records. Must be between 64 and 1048576.
	 */
	private Long aggregationMaxSize;

	/**
	 * CloudWatch endpoint. Must match the regex: ^([A-Za-z0-9-\\.]+)?$
	 */
	private String cloudwatchEndpoint;

	/**
	 * CloudWatch port. Must be between 1 and 65535.
	 */
	private Long cloudwatchPort;

	/**
	 * Maximum number of records to collect before sending a batch. Must be between 1 and 500.
	 */
	private Long collectionMaxCount;

	/**
	 * Maximum size in bytes of collected records before sending. Must be between 52224 and Long.MAX_VALUE.
	 */
	private Long collectionMaxSize;

	/**
	 * Connection timeout in milliseconds. Must be between 100 and 300000.
	 */
	private Long connectTimeout;

	/**
	 * Delay in milliseconds for credentials refresh. Must be between 1 and 300000.
	 */
	private Long credentialsRefreshDelay;

	/**
	 * Whether core dumps are enabled.
	 */
	private Boolean enableCoreDumps;

	/**
	 * Whether to fail if throttled by Kinesis.
	 */
	private Boolean failIfThrottled;

	/**
	 * Log level for the producer. Allowed values: trace, debug, info, warning, error.
	 */
	private String logLevel;

	/**
	 * Maximum number of connections. Must be between 1 and 256.
	 */
	private Long maxConnections;

	/**
	 * Metrics granularity. Allowed values: global, stream, shard.
	 */
	private String metricsGranularity;

	/**
	 * Metrics level. Allowed values: none, summary, detailed.
	 */
	private String metricsLevel;

	/**
	 * Metrics namespace. Must match the regex: (?!AWS/).{1,255}
	 */
	private String metricsNamespace;

	/**
	 * Delay in milliseconds for uploading metrics. Must be between 1 and 60000.
	 */
	private Long metricsUploadDelay;

	/**
	 * Minimum number of connections. Must be between 1 and 16.
	 */
	private Long minConnections;

	/**
	 * Native executable path.
	 */
	private String nativeExecutable;

	/**
	 * Rate limit for records per second. Must be between 1 and Long.MAX_VALUE.
	 */
	private Long rateLimit;

	/**
	 * Maximum buffered time for records in milliseconds. Must be between 0 and Long.MAX_VALUE.
	 */
	private Long recordMaxBufferedTime;

	/**
	 * Time to live for records in milliseconds. Must be between 100 and Long.MAX_VALUE.
	 */
	private Long recordTtl;

	/**
	 * Request timeout in milliseconds. Must be between 100 and 600000.
	 */
	private Long requestTimeout;

	/**
	 * Temporary directory path for the producer.
	 */
	private String tempDirectory;

	/**
	 * Whether to verify SSL certificates.
	 */
	private Boolean verifyCertificate;

	/**
	 * Proxy host.
	 */
	private String proxyHost;

	/**
	 * Proxy port. Must be between 1 and 65535.
	 */
	private Long proxyPort;

	/**
	 * Proxy username.
	 */
	private String proxyUserName;

	/**
	 * Proxy password.
	 */
	private String proxyPassword;

	/**
	 * STS endpoint. Must match the regex: ^([A-Za-z0-9-\\.]+)?$
	 */
	private String stsEndpoint;

	/**
	 * STS port. Must be between 1 and 65535.
	 */
	private Long stsPort;

	/**
	 * Threading model for the producer.
	 */
	private KinesisProducerConfiguration.ThreadingModel threadingModel;

	/**
	 * Thread pool size. Must be greater than or equal to 0.
	 */
	private Integer threadPoolSize;

	/**
	 * Timeout in milliseconds for user records. Must be greater than or equal to 0.
	 */
	private Long userRecordTimeoutInMillis;

	public Boolean getAggregationEnabled() {
		return aggregationEnabled;
	}

	public void setAggregationEnabled(Boolean aggregationEnabled) {
		this.aggregationEnabled = aggregationEnabled;
	}

	public Long getAggregationMaxCount() {
		return aggregationMaxCount;
	}

	public void setAggregationMaxCount(Long aggregationMaxCount) {
		this.aggregationMaxCount = aggregationMaxCount;
	}

	public Long getAggregationMaxSize() {
		return aggregationMaxSize;
	}

	public void setAggregationMaxSize(Long aggregationMaxSize) {
		this.aggregationMaxSize = aggregationMaxSize;
	}

	public String getCloudwatchEndpoint() {
		return cloudwatchEndpoint;
	}

	public void setCloudwatchEndpoint(String cloudwatchEndpoint) {
		this.cloudwatchEndpoint = cloudwatchEndpoint;
	}

	public Long getCloudwatchPort() {
		return cloudwatchPort;
	}

	public void setCloudwatchPort(Long cloudwatchPort) {
		this.cloudwatchPort = cloudwatchPort;
	}

	public Long getCollectionMaxCount() {
		return collectionMaxCount;
	}

	public void setCollectionMaxCount(Long collectionMaxCount) {
		this.collectionMaxCount = collectionMaxCount;
	}

	public Long getCollectionMaxSize() {
		return collectionMaxSize;
	}

	public void setCollectionMaxSize(Long collectionMaxSize) {
		this.collectionMaxSize = collectionMaxSize;
	}

	public Long getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Long getCredentialsRefreshDelay() {
		return credentialsRefreshDelay;
	}

	public void setCredentialsRefreshDelay(Long credentialsRefreshDelay) {
		this.credentialsRefreshDelay = credentialsRefreshDelay;
	}

	public Boolean getEnableCoreDumps() {
		return enableCoreDumps;
	}

	public void setEnableCoreDumps(Boolean enableCoreDumps) {
		this.enableCoreDumps = enableCoreDumps;
	}

	public Boolean getFailIfThrottled() {
		return failIfThrottled;
	}

	public void setFailIfThrottled(Boolean failIfThrottled) {
		this.failIfThrottled = failIfThrottled;
	}

	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	public Long getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(Long maxConnections) {
		this.maxConnections = maxConnections;
	}

	public String getMetricsGranularity() {
		return metricsGranularity;
	}

	public void setMetricsGranularity(String metricsGranularity) {
		this.metricsGranularity = metricsGranularity;
	}

	public String getMetricsLevel() {
		return metricsLevel;
	}

	public void setMetricsLevel(String metricsLevel) {
		this.metricsLevel = metricsLevel;
	}

	public String getMetricsNamespace() {
		return metricsNamespace;
	}

	public void setMetricsNamespace(String metricsNamespace) {
		this.metricsNamespace = metricsNamespace;
	}

	public Long getMetricsUploadDelay() {
		return metricsUploadDelay;
	}

	public void setMetricsUploadDelay(Long metricsUploadDelay) {
		this.metricsUploadDelay = metricsUploadDelay;
	}

	public Long getMinConnections() {
		return minConnections;
	}

	public void setMinConnections(Long minConnections) {
		this.minConnections = minConnections;
	}

	public String getNativeExecutable() {
		return nativeExecutable;
	}

	public void setNativeExecutable(String nativeExecutable) {
		this.nativeExecutable = nativeExecutable;
	}

	public Long getRateLimit() {
		return rateLimit;
	}

	public void setRateLimit(Long rateLimit) {
		this.rateLimit = rateLimit;
	}

	public Long getRecordMaxBufferedTime() {
		return recordMaxBufferedTime;
	}

	public void setRecordMaxBufferedTime(Long recordMaxBufferedTime) {
		this.recordMaxBufferedTime = recordMaxBufferedTime;
	}

	public Long getRecordTtl() {
		return recordTtl;
	}

	public void setRecordTtl(Long recordTtl) {
		this.recordTtl = recordTtl;
	}

	public Long getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(Long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public String getTempDirectory() {
		return tempDirectory;
	}

	public void setTempDirectory(String tempDirectory) {
		this.tempDirectory = tempDirectory;
	}

	public Boolean getVerifyCertificate() {
		return verifyCertificate;
	}

	public void setVerifyCertificate(Boolean verifyCertificate) {
		this.verifyCertificate = verifyCertificate;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public Long getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(Long proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUserName() {
		return proxyUserName;
	}

	public void setProxyUserName(String proxyUserName) {
		this.proxyUserName = proxyUserName;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	public String getStsEndpoint() {
		return stsEndpoint;
	}

	public void setStsEndpoint(String stsEndpoint) {
		this.stsEndpoint = stsEndpoint;
	}

	public Long getStsPort() {
		return stsPort;
	}

	public void setStsPort(Long stsPort) {
		this.stsPort = stsPort;
	}

	public KinesisProducerConfiguration.ThreadingModel getThreadingModel() {
		return threadingModel;
	}

	public void setThreadingModel(KinesisProducerConfiguration.ThreadingModel threadingModel) {
		this.threadingModel = threadingModel;
	}

	public Integer getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(Integer threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	public Long getUserRecordTimeoutInMillis() {
		return userRecordTimeoutInMillis;
	}

	public void setUserRecordTimeoutInMillis(Long userRecordTimeoutInMillis) {
		this.userRecordTimeoutInMillis = userRecordTimeoutInMillis;
	}
}
