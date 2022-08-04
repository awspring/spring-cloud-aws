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

import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.dax.ClusterDaxClient;
import software.amazon.dax.Configuration;

/**
 * Helper class for testing if configured client has given values.
 *
 * @author Matej Nedic
 * @since 3.0.0
 */
class ConfiguredDaxClient {

	private final Configuration configuration;

	public ConfiguredDaxClient(ClusterDaxClient clusterDaxClient) {
		this.configuration = ((software.amazon.dax.Configuration) ReflectionTestUtils.getField(
				ReflectionTestUtils.getField(ReflectionTestUtils.getField(clusterDaxClient, "client"), "cluster"),
				"configuration"));
	}

	public String getUrl() {
		return configuration.url();
	}

	public int getWriteRetries() {
		return configuration.writeRetries();
	}

	public int getReadRetries() {
		return configuration.readRetries();
	}

	public int getConnectTimeoutMillis() {
		return configuration.connectTimeoutMillis();
	}

	public int getRequestTimeoutMillis() {
		return configuration.requestTimeoutMillis();
	}

	public int getClusterUpdateIntervalMillis() {
		return configuration.clusterUpdateIntervalMillis();
	}

	public int getEndpointRefreshTimeoutMillis() {
		return configuration.endpointRefreshTimeoutMillis();
	}

	public int getConnectionTtlMillis() {
		return configuration.connectionTtlMillis();
	}

	public int getIdleTimeoutMillis() {
		return configuration.idleTimeoutMillis();
	}

	public int getMaxConcurrency() {
		return configuration.maxConcurrency();
	}

	public int getMaxPendingConnectionAcquires() {
		return configuration.maxPendingConnectionAcquires();
	}

	public boolean getSkipHostNameVerification() {
		return configuration.skipHostNameVerification();
	}
}
