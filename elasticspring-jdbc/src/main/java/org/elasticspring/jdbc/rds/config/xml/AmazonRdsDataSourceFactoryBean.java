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

package org.elasticspring.jdbc.rds.config.xml;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import org.elasticspring.jdbc.datasource.DataSourceFactory;
import org.elasticspring.jdbc.datasource.DataSourceInformation;
import org.elasticspring.jdbc.datasource.DynamicDataSource;
import org.elasticspring.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class AmazonRdsDataSourceFactoryBean extends AbstractFactoryBean<DataSource> {

	private final AmazonRDS amazonRDS;

	private DataSourceFactory dataSourceFactory = new TomcatJdbcDataSourceFactory();

	private String dbInstanceIdentifier;
	private String password;
	private String username;
	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	public AmazonRdsDataSourceFactoryBean(AmazonRDS amazonRDS) {
		this.amazonRDS = amazonRDS;
	}

	public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		this.dataSourceFactory = dataSourceFactory;
	}

	public void setDbInstanceIdentifier(String dbInstanceIdentifier) {
		this.dbInstanceIdentifier = dbInstanceIdentifier;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

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

		return new DynamicDataSource(fromRdsInstance(instance), this.dataSourceFactory, new AmazonRdsInstanceStatus(this.amazonRDS, instance.getDBInstanceIdentifier()), this.taskExecutor);
	}

	private DataSourceInformation fromRdsInstance(DBInstance dbInstance) {
		return new DataSourceInformation(DataSourceInformation.DatabaseType.valueOf(dbInstance.getEngine().toUpperCase()),
				dbInstance.getEndpoint().getAddress(), dbInstance.getEndpoint().getPort(), dbInstance.getDBName(),
				StringUtils.hasText(this.username) ? this.username : dbInstance.getMasterUsername(), this.password);
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.dbInstanceIdentifier);
		Assert.notNull(this.password);

		super.afterPropertiesSet();
	}

	@Override
	protected void destroyInstance(DataSource instance) throws Exception {
		if (instance instanceof DynamicDataSource) {
			((DynamicDataSource) instance).destroyDataSource();
		}
		super.destroyInstance(instance);
	}

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