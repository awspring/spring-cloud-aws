/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc.rds;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rds.AmazonRDSAsync;
import com.amazonaws.services.rds.AmazonRDSAsyncClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 *
 */
public class AmazonRDSDataSourceFactoryBean extends AbstractFactoryBean<DataSource> {

	private final AmazonRDSAsync amazonRDS;
	private String databaseName;
	private String dbInstanceIdentifier;
	private Integer allocatedStorage;
	private String dbInstanceClass;
	private String engine;
	private String masterUserName;
	private String masterUserPassword;
	private List<String> dbSecurityGroups;
	private String availabilityZone;
	private String preferredMaintenanceWindow;
	private Integer backupRetentionPeriod;
	private String preferredBackupWindow;
	private Integer port;
	private Boolean multiAz;
	private String engineVersion;
	private Boolean autoMinorVersionUpgrade;
	private Boolean autoCreate;
	private DataSourceFactory dataSourceFactory;

	public void setAutoCreate(Boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	public void setAutoMinorVersionUpgrade(Boolean autoMinorVersionUpgrade) {
		this.autoMinorVersionUpgrade = autoMinorVersionUpgrade;
	}

	public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
		this.dataSourceFactory = dataSourceFactory;
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

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public void setDbInstanceClass(String dbInstanceClass) {
		this.dbInstanceClass = dbInstanceClass;
	}

	public void setDbInstanceIdentifier(String dbInstanceIdentifier) {
		this.dbInstanceIdentifier = dbInstanceIdentifier;
	}

	public void setDbSecurityGroups(List<String> dbSecurityGroups) {
		this.dbSecurityGroups = dbSecurityGroups;
	}

	public void setEngine(String engine) {
		this.engine = engine;
	}

	public void setEngineVersion(String engineVersion) {
		this.engineVersion = engineVersion;
	}

	public void setMasterUserName(String masterUserName) {
		this.masterUserName = masterUserName;
	}

	public void setMasterUserPassword(String masterUserPassword) {
		this.masterUserPassword = masterUserPassword;
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

	public void setAllocatedStorage(Integer allocatedStorage) {

		this.allocatedStorage = allocatedStorage;
	}

	public AmazonRDSDataSourceFactoryBean(String accessKey, String secretKey) {
		this.amazonRDS = new AmazonRDSAsyncClient(new BasicAWSCredentials(accessKey, secretKey));
	}


	@Override
	public Class<DataSource> getObjectType() {
		return DataSource.class;
	}

	@Override
	protected DataSource createInstance() throws Exception {

		DBInstance dbInstance = getDBInstance();

		if (dbInstance == null) {
			if (this.autoCreate) {
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
			} else {
				throw new IllegalStateException("No database instance with id:'" + this.dbInstanceIdentifier + "' found and autoCreate is off. " +
						"Please specify a valid db instance or enable auto creation");

			}
		}

		return new DynamicDataSource(dbInstance);
	}

	private DBInstance getDBInstance() {
		DescribeDBInstancesResult dbInstancesResult = this.amazonRDS.describeDBInstances();
		for (DBInstance candidateInstance : dbInstancesResult.getDBInstances()) {
			if (candidateInstance.getDBInstanceIdentifier().equals(this.dbInstanceIdentifier)) {
				return candidateInstance;
			}
		}
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.dbInstanceIdentifier);
		Assert.notNull(this.allocatedStorage);
		Assert.notNull(this.dbInstanceClass);
		Assert.notNull(this.engine);
		Assert.notNull(this.masterUserName);
		Assert.notNull(this.masterUserPassword);
		super.afterPropertiesSet();
	}

	@Override
	protected void destroyInstance(DataSource instance) throws Exception {
		if(instance instanceof DynamicDataSource){
			((DynamicDataSource) instance).destroyDataSource();
		}
		super.destroyInstance(instance);
	}

	private void sleepInBetweenStatusCheck() {
		try {
			Thread.sleep(5000L);
		} catch (InterruptedException e) {
			// Re-interrupt current thread, to allow other threads to react.
			Thread.currentThread().interrupt();
		}
	}

	private final class DynamicDataSource extends AbstractDataSource {

		private volatile DataSource dataSource;
		private final Object dataSourceMonitor = new Object();
		private DBInstance dbInstance;

		public DynamicDataSource(DBInstance dbInstance) {
			this.dbInstance = dbInstance;
		}

		public Connection getConnection() throws SQLException {
			if (this.dataSource == null) {
				initializeDataSource();
			}

			return this.dataSource.getConnection();
		}

		public Connection getConnection(String username, String password) throws SQLException {
			if (this.dataSource == null) {
				initializeDataSource();
			}

			return this.dataSource.getConnection(username, password);
		}

		private void initializeDataSource() {
			synchronized (this.dataSourceMonitor) {
				while (this.dbInstance == null || "creating".equals(this.dbInstance.getDBInstanceStatus())) {
					this.dbInstance = getDBInstance();
					if("creating".equals(this.dbInstance.getDBInstanceStatus())){
						sleepInBetweenStatusCheck();
					}
				}
				this.dataSource = AmazonRDSDataSourceFactoryBean.this.dataSourceFactory.createDataSource("com.mysql.jdbc.Driver", this.dbInstance.getEndpoint().getAddress(), dbInstance.getEndpoint().getPort(), AmazonRDSDataSourceFactoryBean.this.databaseName,
						AmazonRDSDataSourceFactoryBean.this.masterUserName, AmazonRDSDataSourceFactoryBean.this.masterUserPassword);
			}
		}

		public void destroyDataSource(){
			AmazonRDSDataSourceFactoryBean.this.dataSourceFactory.closeDataSource(this.dataSource);
		}
	}
}