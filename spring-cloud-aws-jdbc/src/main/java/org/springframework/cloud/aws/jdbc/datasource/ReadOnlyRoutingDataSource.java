/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.jdbc.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource}
 * implementation that routes to different read only data source in a random fashion if
 * the current transaction is read-only. This is useful for database platforms that
 * support read-replicas (like MySQL) to scale up the access to the database for read-only
 * accesses.
 * <p>
 * <b>Note:</b> In order to use read-only replicas it is necessary to wrap this data
 * source with a {@link org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy}
 * to ensure that the connection is not fetched during transaction creation, but during
 * the first physical access. See the LazyConnectionDataSourceProxy documentation for more
 * details.
 * </p>
 *
 * @author Agim Emruli
 * @see org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
 */
public class ReadOnlyRoutingDataSource extends AbstractRoutingDataSource {

	private final List<Object> dataSources = new ArrayList<>();

	private List<Object> dataSourceKeys;

	private static int getRandom(int high) {
		// noinspection UnsecureRandomNumberGeneration
		return (int) (Math.random() * high);
	}

	@Override
	public void setTargetDataSources(Map<Object, Object> targetDataSources) {
		super.setTargetDataSources(targetDataSources);
		this.dataSourceKeys = new ArrayList<>(targetDataSources.keySet());
		this.dataSources.addAll(targetDataSources.values());
	}

	@Override
	public void setDefaultTargetDataSource(Object defaultTargetDataSource) {
		super.setDefaultTargetDataSource(defaultTargetDataSource);
		this.dataSources.add(defaultTargetDataSource);
	}

	@Override
	protected Object determineCurrentLookupKey() {
		if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()
				&& !this.dataSourceKeys.isEmpty()) {
			return this.dataSourceKeys.get(getRandom(this.dataSourceKeys.size()));
		}

		return null;
	}

	public List<Object> getDataSources() {
		return this.dataSources;
	}

}
