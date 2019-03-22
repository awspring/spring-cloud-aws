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

import org.springframework.cloud.aws.jdbc.datasource.support.DatabaseType;
import org.springframework.util.Assert;

/**
 * Immutable parameter object that holds all information needed by the
 * {@link DataSourceFactory} implementation to create a datasource. The attributes inside
 * this class represents the minimal information for the DataSourceFactory to actually
 * create the underlying datasource.
 * <p>
 * This parameter object is used to allow a more flexible DataSourceFactory interface
 * </p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public final class DataSourceInformation {

	private final DatabaseType databaseType;

	private final String hostName;

	private final Integer port;

	private final String databaseName;

	private final String userName;

	private final String password;

	/**
	 * Main constructor to create this object. This constructor receives all information
	 * to fully construct the object and to allow the implementation to be immutable.
	 * @param databaseType - The database type used by the datasource to connect to. This
	 * information will be typically used to instantiate and use the particular driver
	 * class to connect to the database platform.
	 * @param hostName - The fully qualified hostname without any protocol or port
	 * information (e.g. myDbServer.domain.com, localhost, 192.168.23.1)
	 * @param port - The port used to connect to the particular database platform (eg.
	 * 3306 as a default for mysql)
	 * @param databaseName - The database name used to connect to the database. The
	 * meaning is database specific for (e.g for mysql the database name, for oracle the
	 * SID id)
	 * @param userName - The username used to connect to the database
	 * @param password - The password used to connect to the database
	 */
	public DataSourceInformation(DatabaseType databaseType, String hostName, Integer port,
			String databaseName, String userName, String password) {
		Assert.notNull(databaseType, "DatabaseType must not be null");
		Assert.notNull(hostName, "Hostname must not be null");
		Assert.notNull(port, "Port must not be null");
		Assert.notNull(userName, "UserName must not be null");
		Assert.notNull(password, "Password must not be null");

		this.databaseType = databaseType;
		this.hostName = hostName;
		this.port = port;
		this.databaseName = databaseName;
		this.userName = userName;
		this.password = password;
	}

	/**
	 * Returns the database type provided by this class. Represented by an enumeration
	 * based on the supported database platform in the AWS platform.
	 * @return - The databaseType - never null
	 */
	public DatabaseType getDatabaseType() {
		return this.databaseType;
	}

	/**
	 * Returns the host name which will be used to connect to the database. This is only
	 * the hostname without any further protocol information.
	 * @return - The hostname - never null
	 */
	public String getHostName() {
		return this.hostName;
	}

	/**
	 * Returns the port used to connect to the database.
	 * @return - The port - never null
	 */
	public Integer getPort() {
		return this.port;
	}

	/**
	 * The database name used to connect to the database. The information is
	 * {@link #databaseType} type specific.
	 * <ul>
	 * <li>MySQL - This is the database name to connect to</li>
	 * <li>Oracle - This is the system id (SID) to connect to</li>
	 * <li>SQLSERVER - This information is not used at all</li>
	 * </ul>
	 * @return - The database name that can be used to connect to the database.
	 */
	public String getDatabaseName() {
		return this.databaseName;
	}

	/**
	 * The username used to connect to the database. The user must have the right to
	 * connect to the database.
	 * @return - The username never null
	 */
	public String getUserName() {
		return this.userName;
	}

	/**
	 * The password used to connect to the database. The password might be empty but not
	 * null.
	 * @return - the password - never null
	 */
	public String getPassword() {
		return this.password;
	}

	@Override
	public int hashCode() {
		int result = this.databaseType.hashCode();
		result = 31 * result + this.hostName.hashCode();
		result = 31 * result + this.port.hashCode();
		result = 31 * result + this.databaseName.hashCode();
		result = 31 * result + this.userName.hashCode();
		result = 31 * result + this.password.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DataSourceInformation)) {
			return false;
		}

		DataSourceInformation that = (DataSourceInformation) obj;

		if (this.databaseType != that.getDatabaseType()) {
			return false;
		}
		if (!this.databaseName.equals(that.getDatabaseName())) {
			return false;
		}
		if (!this.hostName.equals(that.getHostName())) {
			return false;
		}
		if (!this.password.equals(that.getPassword())) {
			return false;
		}
		if (!this.port.equals(that.getPort())) {
			return false;
		}
		return this.userName.equals(that.getUserName());

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DataSourceInformation");
		sb.append("{databaseType=").append(this.databaseType);
		sb.append(", hostName='").append(this.hostName).append("'");
		sb.append(", port=").append(this.port);
		sb.append(", databaseName='").append(this.databaseName).append("'");
		sb.append(", userName='").append(this.userName).append("'");
		sb.append(", password='").append(this.password).append("'");
		sb.append("}");
		return sb.toString();
	}

}
