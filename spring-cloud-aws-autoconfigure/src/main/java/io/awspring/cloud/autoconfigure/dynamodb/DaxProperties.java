/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.autoconfigure.dynamodb;

import org.springframework.lang.Nullable;

/**
 * Properties used to configure {@link software.amazon.dax.ClusterDaxClient}
 *
 * @author Matej Nedić
 * @since 3.0.0
 */
public class DaxProperties {
	/**
	 * Timeout for idle connections with the DAX cluster.
	 */
	@Nullable
	private Integer idleTimeoutMillis;
	/**
	 * DAX cluster endpoint.
	 */
	@Nullable
	private String url;
	/**
	 * Connection time to live.
	 */
	@Nullable
	private Integer connectionTtlMillis;
	/**
	 * Connection timeout.
	 */
	@Nullable
	private Integer connectTimeoutMillis;
	/**
	 * Request timeout for connections with the DAX cluster.
	 */
	@Nullable
	private Integer requestTimeoutMillis;
	/**
	 * Number of times to retry writes, initial try is not counted.
	 */
	@Nullable
	private Integer writeRetries;
	/**
	 * Number of times to retry reads, initial try is not counted.
	 */
	@Nullable
	private Integer readRetries;
	/**
	 * Interval between polling of cluster members for membership changes.
	 */
	@Nullable
	private Integer clusterUpdateIntervalMillis;
	/**
	 * Timeout for endpoint refresh.
	 */
	@Nullable
	private Integer endpointRefreshTimeoutMillis;
	/**
	 * Maximum number of concurrent requests.
	 */
	@Nullable
	private Integer maxConcurrency;
	/**
	 * Maximum number of pending Connections to acquire.
	 */
	@Nullable
	private Integer maxPendingConnectionAcquires;
	/**
	 * Skips hostname verification in url.
	 */
	@Nullable
	private Boolean skipHostNameVerification;

	@Nullable
	public Integer getIdleTimeoutMillis() {
		return idleTimeoutMillis;
	}

	public void setIdleTimeoutMillis(@Nullable Integer idleTimeoutMillis) {
		this.idleTimeoutMillis = idleTimeoutMillis;
	}

	@Nullable
	public Integer getConnectionTtlMillis() {
		return connectionTtlMillis;
	}

	public void setConnectionTtlMillis(@Nullable Integer connectionTtlMillis) {
		this.connectionTtlMillis = connectionTtlMillis;
	}

	@Nullable
	public Integer getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public void setConnectTimeoutMillis(@Nullable Integer connectTimeoutMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
	}

	@Nullable
	public Integer getRequestTimeoutMillis() {
		return requestTimeoutMillis;
	}

	public void setRequestTimeoutMillis(@Nullable Integer requestTimeoutMillis) {
		this.requestTimeoutMillis = requestTimeoutMillis;
	}

	@Nullable
	public Integer getWriteRetries() {
		return writeRetries;
	}

	public void setWriteRetries(@Nullable Integer writeRetries) {
		this.writeRetries = writeRetries;
	}

	@Nullable
	public Integer getReadRetries() {
		return readRetries;
	}

	public void setReadRetries(@Nullable Integer readRetries) {
		this.readRetries = readRetries;
	}

	@Nullable
	public Integer getClusterUpdateIntervalMillis() {
		return clusterUpdateIntervalMillis;
	}

	public void setClusterUpdateIntervalMillis(@Nullable Integer clusterUpdateIntervalMillis) {
		this.clusterUpdateIntervalMillis = clusterUpdateIntervalMillis;
	}

	@Nullable
	public Integer getEndpointRefreshTimeoutMillis() {
		return endpointRefreshTimeoutMillis;
	}

	public void setEndpointRefreshTimeoutMillis(@Nullable Integer endpointRefreshTimeoutMillis) {
		this.endpointRefreshTimeoutMillis = endpointRefreshTimeoutMillis;
	}

	@Nullable
	public Integer getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(@Nullable Integer maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	@Nullable
	public Integer getMaxPendingConnectionAcquires() {
		return maxPendingConnectionAcquires;
	}

	public void setMaxPendingConnectionAcquires(@Nullable Integer maxPendingConnectionAcquires) {
		this.maxPendingConnectionAcquires = maxPendingConnectionAcquires;
	}

	@Nullable
	public Boolean getSkipHostNameVerification() {
		return skipHostNameVerification;
	}

	public void setSkipHostNameVerification(@Nullable Boolean skipHostNameVerification) {
		this.skipHostNameVerification = skipHostNameVerification;
	}

	@Nullable
	public String getUrl() {
		return url;
	}

	public void setUrl(@Nullable String url) {
		this.url = url;
	}
}
