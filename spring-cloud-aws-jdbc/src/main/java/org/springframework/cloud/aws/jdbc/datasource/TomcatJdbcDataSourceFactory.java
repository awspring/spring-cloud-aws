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

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import org.springframework.beans.BeanUtils;
import org.springframework.cloud.aws.jdbc.datasource.support.DatabasePlatformSupport;
import org.springframework.cloud.aws.jdbc.datasource.support.StaticDatabasePlatformSupport;
import org.springframework.core.Constants;

/**
 * A Tomcat JDBC Pool {@link DataSourceFactory} implementation that creates a JDBC pool
 * backed datasource. Allows the configuration of all configuration properties except
 * username, password, url and driver class name because they are passed in while actually
 * creating the datasource. All other properties can be modified by calling the respective
 * setter methods. This class uses a {@link DatabasePlatformSupport} implementation to
 * actually retrieve the driver class name and url in order to create the datasource.
 * <p>
 * All properties are derived from {@link PoolConfiguration} of the Tomcat JDBC Pool
 * class.
 * </p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class TomcatJdbcDataSourceFactory extends PoolProperties
		implements DataSourceFactory {

	private static final String PREFIX_ISOLATION = "TRANSACTION_";

	private DatabasePlatformSupport databasePlatformSupport = new StaticDatabasePlatformSupport();

	@Override
	public void setUsername(String username) {
		throw new UnsupportedOperationException("Username will be set at runtime!");
	}

	@Override
	public void setPassword(String password) {
		throw new UnsupportedOperationException("Password will be set at runtime!");
	}

	@Override
	public void setDriverClassName(String driverClassName) {
		throw new UnsupportedOperationException("Will be set at runtime!");
	}

	@Override
	public void setUrl(String url) {
		throw new UnsupportedOperationException("Will be set at runtime!");
	}

	public void setDefaultTransactionIsolationName(String constantName) {
		if (constantName == null) {
			throw new IllegalArgumentException("Isolation name must not be null");
		}
		Constants constants = new Constants(Connection.class);
		setDefaultTransactionIsolation(
				constants.asNumber(PREFIX_ISOLATION + constantName).intValue());
	}

	public void setDatabasePlatformSupport(
			DatabasePlatformSupport databasePlatformSupport) {
		this.databasePlatformSupport = databasePlatformSupport;
	}

	@Override
	public org.apache.tomcat.jdbc.pool.DataSource createDataSource(
			DataSourceInformation dataSourceInformation) {
		// create a method scoped instance
		PoolConfiguration configurationToUse = new PoolProperties();

		// copy all general properties
		BeanUtils.copyProperties(this, configurationToUse);

		configurationToUse.setDriverClassName(this.databasePlatformSupport
				.getDriverClassNameForDatabase(dataSourceInformation.getDatabaseType()));
		configurationToUse.setUrl(this.databasePlatformSupport.getDatabaseUrlForDatabase(
				dataSourceInformation.getDatabaseType(),
				dataSourceInformation.getHostName(), dataSourceInformation.getPort(),
				dataSourceInformation.getDatabaseName()));
		configurationToUse.setUsername(dataSourceInformation.getUserName());
		configurationToUse.setPassword(dataSourceInformation.getPassword());

		return new org.apache.tomcat.jdbc.pool.DataSource(configurationToUse);
	}

	@Override
	public void closeDataSource(DataSource dataSource) {
		if (dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
			((org.apache.tomcat.jdbc.pool.DataSource) dataSource).close();
		}
	}

}
