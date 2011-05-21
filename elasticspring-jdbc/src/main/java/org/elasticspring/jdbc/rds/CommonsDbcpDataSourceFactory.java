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

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 *
 */
public class CommonsDbcpDataSourceFactory implements DataSourceFactory, BeanClassLoaderAware {

	private ClassLoader beanClassLoader;
	private static final Logger LOGGER = LoggerFactory.getLogger(CommonsDbcpDataSourceFactory.class);


	public DataSource createDataSource(String dataSourceClass, String hostName, Integer port, String databaseName, String userName, String password) {
		BasicDataSource basicDataSource = new BasicDataSource();
		basicDataSource.setDriverClassName(dataSourceClass);
		basicDataSource.setUrl(new StringBuilder().append("jdbc:mysql://").append(hostName).append(":").append(port).append("/").append(databaseName).toString());
		basicDataSource.setDriverClassLoader(this.beanClassLoader);
		basicDataSource.setUsername(userName);
		basicDataSource.setPassword(password);
		return basicDataSource;
	}

	public void closeDataSource(DataSource dataSource) {
		if(dataSource instanceof BasicDataSource){
			try {
				((BasicDataSource) dataSource).close();
			} catch (SQLException e) {
				LOGGER.debug("Could not close JDBC Connection", e);
			}
		}
	}


	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}
}
