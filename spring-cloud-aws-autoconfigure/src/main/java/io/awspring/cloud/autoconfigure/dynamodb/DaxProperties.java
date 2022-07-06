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

/**
 * Properties used to configure {@link software.amazon.dax.ClusterDaxClient}
 * @author Matej Nedić
 * @since 3.0.0x
 */
public class DaxProperties {
	/**
	 * Timeout for idle connections with the DAX cluster.
	 */
	private int idleTimeoutMillis = 30000;
	/**
	 * DAX cluster endpoint.
	 */
	private String url = "";
	private int connectionTtlMillis = 0;
	private int connectTimeoutMillis = 1000;
	/**
	 * Request timeout for connections with the DAX cluster.
	 */
	private int requestTimeoutMillis = 1000;
	/**
	 * Number of times to retry writes, initial try is not counted.
	 */
	private int writeRetries = 2;
	/**
	 * Number of times to retry reads, initial try is not counted.
	 */
	private int readRetries = 2;
	/**
	 * Interval between polling of cluster members for membership changes.
	 */
	private int clusterUpdateIntervalMillis = 4000;
	private int endpointRefreshTimeoutMillis = 6000;
	private int maxConcurrency = 1000;
	private int maxPendingConnectionAcquires = 10000;
	private boolean skipHostNameVerification;

	public int getIdleTimeoutMillis() {
		return idleTimeoutMillis;
	}

	public void setIdleTimeoutMillis(int idleTimeoutMillis) {
		this.idleTimeoutMillis = idleTimeoutMillis;
	}

	public int getConnectionTtlMillis() {
		return connectionTtlMillis;
	}

	public void setConnectionTtlMillis(int connectionTtlMillis) {
		this.connectionTtlMillis = connectionTtlMillis;
	}

	public int getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public void setConnectTimeoutMillis(int connectTimeoutMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
	}

	public int getRequestTimeoutMillis() {
		return requestTimeoutMillis;
	}

	public void setRequestTimeoutMillis(int requestTimeoutMillis) {
		this.requestTimeoutMillis = requestTimeoutMillis;
	}

	public int getWriteRetries() {
		return writeRetries;
	}

	public void setWriteRetries(int writeRetries) {
		this.writeRetries = writeRetries;
	}

	public int getReadRetries() {
		return readRetries;
	}

	public void setReadRetries(int readRetries) {
		this.readRetries = readRetries;
	}

	public int getClusterUpdateIntervalMillis() {
		return clusterUpdateIntervalMillis;
	}

	public void setClusterUpdateIntervalMillis(int clusterUpdateIntervalMillis) {
		this.clusterUpdateIntervalMillis = clusterUpdateIntervalMillis;
	}

	public int getEndpointRefreshTimeoutMillis() {
		return endpointRefreshTimeoutMillis;
	}

	public void setEndpointRefreshTimeoutMillis(int endpointRefreshTimeoutMillis) {
		this.endpointRefreshTimeoutMillis = endpointRefreshTimeoutMillis;
	}

	public int getMaxConcurrency() {
		return maxConcurrency;
	}

	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	public int getMaxPendingConnectionAcquires() {
		return maxPendingConnectionAcquires;
	}

	public void setMaxPendingConnectionAcquires(int maxPendingConnectionAcquires) {
		this.maxPendingConnectionAcquires = maxPendingConnectionAcquires;
	}

	public boolean isSkipHostNameVerification() {
		return skipHostNameVerification;
	}

	public void setSkipHostNameVerification(boolean skipHostNameVerification) {
		this.skipHostNameVerification = skipHostNameVerification;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
