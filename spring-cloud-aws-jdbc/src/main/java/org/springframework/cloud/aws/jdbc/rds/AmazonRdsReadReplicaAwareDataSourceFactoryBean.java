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

package org.springframework.cloud.aws.jdbc.rds;

import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;

import org.springframework.cloud.aws.jdbc.datasource.ReadOnlyRoutingDataSource;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * {@link org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean} sub-class
 * that is capable to handle amazon rds read-replicas. This is especially useful in case
 * of read-heavy applications to leverage read-replica instance for all read-accesses.
 *
 * @author Agim Emruli
 */
public class AmazonRdsReadReplicaAwareDataSourceFactoryBean
		extends AmazonRdsDataSourceFactoryBean {

	/**
	 * Constructor which retrieves all mandatory objects to allow the object to be
	 * constructed. This are the minimal configuration options which uses defaults or no
	 * values for all optional elements.
	 * @param amazonRDS - The amazonRDS instance used to connect to the service. This
	 * object will be used to actually retrieve the datasource metadata from the Amazon
	 * RDS service.
	 * @param dbInstanceIdentifier - the unique database instance identifier in the Amazon
	 * RDS service
	 * @param password - The password used to connect to the datasource. For security
	 * reasons the password is not available in the
	 */
	public AmazonRdsReadReplicaAwareDataSourceFactoryBean(AmazonRDS amazonRDS,
			String dbInstanceIdentifier, String password) {
		super(amazonRDS, dbInstanceIdentifier, password);
	}

	/**
	 * Constructs a
	 * {@link org.springframework.cloud.aws.jdbc.datasource.ReadOnlyRoutingDataSource}
	 * data source that contains the regular data source as a default, and all
	 * read-replicas as additional data source. The
	 * {@link org.springframework.cloud.aws.jdbc.datasource.ReadOnlyRoutingDataSource} is
	 * additionally wrapped with a
	 * {@link org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy}, because
	 * the read-only flag is only available after the transactional context has been
	 * established. This is only the case if the physical connection is requested after
	 * the transaction start and not while starting a transaction.
	 * @return a ReadOnlyRoutingDataSource that is wrapped with a
	 * LazyConnectionDataSourceProxy
	 * @throws Exception if the underlying data source setup throws any exception
	 */
	@Override
	protected DataSource createInstance() throws Exception {
		DBInstance dbInstance = getDbInstance(getDbInstanceIdentifier());

		// If there is no read replica available, delegate to super class
		if (dbInstance.getReadReplicaDBInstanceIdentifiers().isEmpty()) {
			return super.createInstance();
		}

		HashMap<Object, Object> replicaMap = new HashMap<>(
				dbInstance.getReadReplicaDBInstanceIdentifiers().size());

		for (String replicaName : dbInstance.getReadReplicaDBInstanceIdentifiers()) {
			replicaMap.put(replicaName, createDataSourceInstance(replicaName));
		}

		// Create the data source
		ReadOnlyRoutingDataSource dataSource = new ReadOnlyRoutingDataSource();
		dataSource.setTargetDataSources(replicaMap);
		dataSource.setDefaultTargetDataSource(
				createDataSourceInstance(getDbInstanceIdentifier()));

		// Initialize the class
		dataSource.afterPropertiesSet();

		return new LazyConnectionDataSourceProxy(dataSource);
	}

	@Override
	protected void destroyInstance(DataSource instance) throws Exception {
		if (instance instanceof LazyConnectionDataSourceProxy) {
			DataSource targetDataSource = ((LazyConnectionDataSourceProxy) instance)
					.getTargetDataSource();
			if (targetDataSource instanceof ReadOnlyRoutingDataSource) {
				List<Object> dataSources = ((ReadOnlyRoutingDataSource) targetDataSource)
						.getDataSources();
				for (Object candidate : dataSources) {
					if (candidate instanceof DataSource) {
						super.destroyInstance(instance);
					}
				}
			}
		}
	}

}
