/*
 *
 *  * Copyright 2010-2012 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.elasticspring.jdbc.rds;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rds.AmazonRDSAsync;
import com.amazonaws.services.rds.AmazonRDSAsyncClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DeleteDBInstanceRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class AmazonRDSDataSourceFactoryBean extends AbstractFactoryBean<DataSource> {

	private final AmazonRDSAsync amazonRDS;
	private DataSourceFactory dataSourceFactory;

	private boolean autoCreate;
	private String dbInstanceIdentifier;
	private String databaseName;
	private String masterUserName;
	private String masterUserPassword;

	private String dbInstanceClass;
	private String engine;
	private String engineVersion;
	private Integer allocatedStorage;

	private List<String> dbSecurityGroups;
	private String availabilityZone;
	private String preferredMaintenanceWindow;
	private Integer backupRetentionPeriod;
	private String preferredBackupWindow;
	private Integer port;
	private Boolean multiAz;
	private Boolean autoMinorVersionUpgrade;

	private boolean automaticallyCreated;

	public AmazonRDSDataSourceFactoryBean(String accessKey, String secretKey) {
		this.amazonRDS = new AmazonRDSAsyncClient(new BasicAWSCredentials(accessKey, secretKey));
	}

	public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		this.dataSourceFactory = dataSourceFactory;
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	public void setDbInstanceIdentifier(String dbInstanceIdentifier) {
		this.dbInstanceIdentifier = dbInstanceIdentifier;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public void setMasterUserName(String masterUserName) {
		this.masterUserName = masterUserName;
	}

	public void setMasterUserPassword(String masterUserPassword) {
		this.masterUserPassword = masterUserPassword;
	}

	public void setDbInstanceClass(String dbInstanceClass) {
		this.dbInstanceClass = dbInstanceClass;
	}

	public void setEngine(String engine) {
		this.engine = engine;
	}

	public void setEngineVersion(String engineVersion) {
		this.engineVersion = engineVersion;
	}

	public void setAutoMinorVersionUpgrade(Boolean autoMinorVersionUpgrade) {
		this.autoMinorVersionUpgrade = autoMinorVersionUpgrade;
	}

	public void setAllocatedStorage(Integer allocatedStorage) {
		this.allocatedStorage = allocatedStorage;
	}

	public void setMultiAz(Boolean multiAz) {
		this.multiAz = multiAz;
	}

	public void setAvailabilityZone(String availabilityZone) {
		this.availabilityZone = availabilityZone;
	}

	public void setBackupRetentionPeriod(Integer backupRetentionPeriod) {
		this.backupRetentionPeriod = backupRetentionPeriod;
	}

	public void setDbSecurityGroups(List<String> dbSecurityGroups) {
		this.dbSecurityGroups = dbSecurityGroups;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public void setPreferredBackupWindow(String preferredBackupWindow) {
		this.preferredBackupWindow = preferredBackupWindow;
	}

	public void setPreferredMaintenanceWindow(String preferredMaintenanceWindow) {
		this.preferredMaintenanceWindow = preferredMaintenanceWindow;
	}

	@Override
	public Class<DataSource> getObjectType() {
		return DataSource.class;
	}

	@Override
	protected DataSource createInstance() throws Exception {

		DBInstance dbInstance = getDBInstance(this.dbInstanceIdentifier);

		if (dbInstance == null) {
			if (this.autoCreate) {
				createDBInstance();
				this.automaticallyCreated = true;
			} else {
				throw new IllegalStateException(MessageFormat.format("No database instance with id:''{0}'' found and autoCreate is off. Please specify a valid db instance or enable auto creation",
						this.dbInstanceIdentifier));
			}
		}

		return new DynamicDataSource();
	}

	protected void createDBInstance() {
		CreateDBInstanceRequest createDBInstanceRequest = new CreateDBInstanceRequest(this.dbInstanceIdentifier, this.allocatedStorage, this.dbInstanceClass, this.engine, this.masterUserName, this.masterUserPassword).withDBName(this.databaseName);
		if (this.dbSecurityGroups != null) {
			createDBInstanceRequest.withDBSecurityGroups(this.dbSecurityGroups);
		}
		if (this.availabilityZone != null) {
			createDBInstanceRequest.withAvailabilityZone(this.availabilityZone);
		}
		if (this.preferredMaintenanceWindow != null) {
			createDBInstanceRequest.withPreferredMaintenanceWindow(this.preferredMaintenanceWindow);
		}
		if (this.backupRetentionPeriod != null) {
			createDBInstanceRequest.withBackupRetentionPeriod(this.backupRetentionPeriod);
		}
		if (this.preferredBackupWindow != null) {
			createDBInstanceRequest.withPreferredBackupWindow(this.preferredBackupWindow);
		}
		if (this.port != null) {
			createDBInstanceRequest.withPort(this.port);
		}
		if (this.multiAz != null) {
			createDBInstanceRequest.withMultiAZ(this.multiAz);
		}
		if (this.engineVersion != null) {
			createDBInstanceRequest.withEngineVersion(this.engineVersion);
		}
		if (this.autoMinorVersionUpgrade != null) {
			createDBInstanceRequest.withAutoMinorVersionUpgrade(this.autoMinorVersionUpgrade);
		}
		this.amazonRDS.createDBInstance(createDBInstanceRequest);
	}

	private DBInstance getDBInstance(String instanceName) {
		DescribeDBInstancesResult dbInstancesResult;
		try {
			dbInstancesResult = this.amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(instanceName));
		} catch (DBInstanceNotFoundException e) {
			return null;
		}

		return dbInstancesResult.getDBInstances().get(0);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.dbInstanceIdentifier);
		Assert.notNull(this.databaseName);
		Assert.notNull(this.masterUserName);
		Assert.notNull(this.masterUserPassword);

		if (this.autoCreate) {
			Assert.notNull(this.allocatedStorage);
			Assert.notNull(this.dbInstanceClass);
			Assert.notNull(this.engine);
		}

		super.afterPropertiesSet();
	}

	@Override
	protected void destroyInstance(DataSource instance) throws Exception {
		if (instance instanceof DynamicDataSource) {
			((DynamicDataSource) instance).destroyDataSource();
		}

		if (this.automaticallyCreated) {
			this.amazonRDS.deleteDBInstance(new DeleteDBInstanceRequest(this.dbInstanceIdentifier).withSkipFinalSnapshot(true));
		}
		super.destroyInstance(instance);
	}

	private final class DynamicDataSource extends AbstractDataSource {

		private final Object dataSourceMonitor = new Object();

		private final List<String> shutDownStates = Arrays.asList("available", "failed", "storage-full");

		private DBInstance dbInstance;

		private volatile DataSource dataSource;

		@Override
		public Connection getConnection() throws SQLException {
			if (this.dataSource == null) {
				initializeDataSource();
			}

			return this.dataSource.getConnection();
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			if (this.dataSource == null) {
				initializeDataSource();
			}

			return this.dataSource.getConnection(username, password);
		}

		private void initializeDataSource() {
			synchronized (this.dataSourceMonitor) {
				while (this.dbInstance == null || "creating".equals(this.dbInstance.getDBInstanceStatus())) {
					this.dbInstance = getDBInstance(AmazonRDSDataSourceFactoryBean.this.dbInstanceIdentifier);
					if ("creating".equals(this.dbInstance.getDBInstanceStatus())) {
						sleepInBetweenStatusCheck();
					}
				}
				this.dataSource = AmazonRDSDataSourceFactoryBean.this.dataSourceFactory.createDataSource("com.mysql.jdbc.Driver",
						this.dbInstance.getEndpoint().getAddress(),
						this.dbInstance.getEndpoint().getPort(),
						AmazonRDSDataSourceFactoryBean.this.databaseName,
						AmazonRDSDataSourceFactoryBean.this.masterUserName,
						AmazonRDSDataSourceFactoryBean.this.masterUserPassword);
			}
		}

		public void destroyDataSource() {
			synchronized (this.dataSourceMonitor) {
				this.dbInstance = getDBInstance(this.dbInstance.getDBInstanceIdentifier());
				while (this.dbInstance == null || this.shutDownStates.contains(this.dbInstance.getDBInstanceStatus())) {
					this.dbInstance = getDBInstance(AmazonRDSDataSourceFactoryBean.this.dbInstanceIdentifier);
				}
				AmazonRDSDataSourceFactoryBean.this.dataSourceFactory.closeDataSource(this.dataSource);
			}
		}

		private void sleepInBetweenStatusCheck() {
			try {
				Thread.sleep(5000L);
			} catch (InterruptedException e) {
				// Re-interrupt current thread, to allow other threads to react.
				Thread.currentThread().interrupt();
			}
		}
	}
}