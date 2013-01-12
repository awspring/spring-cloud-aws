/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import org.elasticspring.jdbc.datasource.DataSourceFactory;
import org.elasticspring.jdbc.datasource.DataSourceInformation;
import org.elasticspring.jdbc.datasource.DynamicDataSource;
import org.elasticspring.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.elasticspring.jdbc.datasource.support.DatabaseType;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

/**
 * {@link org.springframework.beans.factory.FactoryBean} implementation that create a data source backed by an Amazon
 * Relational Database service instance. This factory bean retrieves all the meta data from the AWS RDS service in
 * order to create and configure a data source. THis class uses the {@link AmazonRDS} service to retrieve the meta data
 * and the {@link DataSourceFactory} to actually create the data source.
 * <p/>
 * The created data source of this implementation is a {@link DynamicDataSource} which allows the creation of a "proxy"
 * data source to allow this factory bean to complete. The DynamicDataSource class will uses the {@link
 * org.elasticspring.jdbc.datasource.DynamicDataSource.DataSourceStatus} implementation provides by this class to
 * actually check whenever this data source is available. If you want to make sure that the data source is available
 * before this class returns the object, then pass an {@link org.springframework.core.task.SyncTaskExecutor} which will
 * effectively wait till the data source is available before returning the object.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonRdsDataSourceFactoryBean extends AbstractFactoryBean<DataSource> {

	private final AmazonRDS amazonRDS;
	private final String dbInstanceIdentifier;
	private final String password;

	private DataSourceFactory dataSourceFactory = new TomcatJdbcDataSourceFactory();
	private String username;
	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	/**
	 * Constructor which retrieves all mandatory objects to allow the object to be constructed. This are the minimal
	 * configuration options which uses defaults or no values for all optional elements.
	 *
	 * @param amazonRDS
	 * 		- The amazonRDS instance used to connect to the service. This object will be used to actually retrieve the data
	 * 		source meta data from the Amazon RDS service.
	 * @param dbInstanceIdentifier
	 * 		- the unique data base instance identifier in the Amazon RDS service
	 * @param password
	 * 		- The password used to connect to the data source. For security reasons the password is not available in the meta
	 * 		data (in contrast to the user) so it must be provided in order to connect to the data base with JDBC.
	 */
	public AmazonRdsDataSourceFactoryBean(AmazonRDS amazonRDS, String dbInstanceIdentifier, String password) {
		this.amazonRDS = amazonRDS;
		this.dbInstanceIdentifier = dbInstanceIdentifier;
		this.password = password;
	}

	/**
	 * Allows to configure a different DataSourceFactory in order to use a different DataSource implementation. Uses the
	 * {@link TomcatJdbcDataSourceFactory} by default if not configured.
	 *
	 * @param dataSourceFactory
	 * 		- A fully configured DataSourceFactory instance, will be used by the DynamicDataSource to actually create the
	 * 		data
	 * 		source.
	 */
	public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		this.dataSourceFactory = dataSourceFactory;
	}

	/**
	 * Allows to set a different user then the master user name in order to connect to the data base. In contrast to the
	 * password, the master user name is available in the meta data to connect to the database so this username is only
	 * used when configured.
	 *
	 * @param username
	 * 		- The username to connect to the database, every value provided (even empty ones) are used to connect to the
	 * 		database.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Allows to configure a different TaskExecutor which will be passed to the DynamicDataSource in order to retrieve the
	 * DataSourceStatus status. Uses an {@link SimpleAsyncTaskExecutor} by default which will create a thread to retrieve
	 * the data source status.
	 *
	 * @param taskExecutor
	 * 		- A configured TaskExecutor implementation. May be a pooled one or even a managed one. See TaskExecutor
	 * 		implementations for further details.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	public Class<DataSource> getObjectType() {
		return DataSource.class;
	}

	@Override
	protected DataSource createInstance() throws Exception {

		DBInstance instance;
		try {
			DescribeDBInstancesResult describeDBInstancesResult = this.amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(this.dbInstanceIdentifier));
			instance = describeDBInstancesResult.getDBInstances().get(0);
		} catch (DBInstanceNotFoundException e) {
			throw new IllegalStateException(MessageFormat.format("No database instance with id:''{0}'' found. Please specify a valid db instance",
					this.dbInstanceIdentifier));
		}

		DynamicDataSource dynamicDataSource = new DynamicDataSource(fromRdsInstance(instance), this.dataSourceFactory, new AmazonRdsInstanceStatus(this.amazonRDS, instance.getDBInstanceIdentifier()), this.taskExecutor);
		dynamicDataSource.afterPropertiesSet();
		return dynamicDataSource;
	}

	private DataSourceInformation fromRdsInstance(DBInstance dbInstance) {
		return new DataSourceInformation(DatabaseType.valueOf(dbInstance.getEngine().toUpperCase()),
				dbInstance.getEndpoint().getAddress(), dbInstance.getEndpoint().getPort(), dbInstance.getDBName(),
				StringUtils.hasText(this.username) ? this.username : dbInstance.getMasterUsername(), this.password);
	}

	@Override
	protected void destroyInstance(DataSource instance) throws Exception {
		if (instance instanceof DynamicDataSource) {
			((DynamicDataSource) instance).destroyDataSource();
		}
	}

	/**
	 * SPI implementation of the {@link org.elasticspring.jdbc.datasource.DynamicDataSource.DataSourceStatus} interface.
	 * Check the data source status through the AWS RDS meta-data and returns true once the data source is available.
	 */
	static class AmazonRdsInstanceStatus implements DynamicDataSource.DataSourceStatus {

		private final AmazonRDS amazonRDS;
		private final String instanceIdentifier;
		private static final List<String> AVAILABLE_STATES = Arrays.asList("available");

		AmazonRdsInstanceStatus(AmazonRDS amazonRDS, String instanceIdentifier) {
			this.amazonRDS = amazonRDS;
			this.instanceIdentifier = instanceIdentifier;
		}

		@Override
		public boolean isDataSourceAvailable() {
			DescribeDBInstancesResult describeDBInstancesResult = this.amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(this.instanceIdentifier));
			DBInstance instance = describeDBInstancesResult.getDBInstances().get(0);
			return AVAILABLE_STATES.contains(instance.getDBInstanceStatus());
		}
	}
}