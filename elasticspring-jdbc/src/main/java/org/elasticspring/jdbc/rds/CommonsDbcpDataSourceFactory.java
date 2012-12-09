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

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 *
 */
public class CommonsDbcpDataSourceFactory implements DataSourceFactory, BeanClassLoaderAware {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonsDbcpDataSourceFactory.class);

	private Boolean defaultAutoCommit;
	private Boolean defaultReadOnly;
	private Integer defaultTransactionIsolation;
	private String defaultCatalog;
	private Integer maxActive;
	private Integer maxIdle;
	private Integer minIdle;
	private Integer initialSize;
	private Long maxWait;
	private Boolean poolPreparedStatements;
	private Integer maxOpenPreparedStatements;
	private Boolean testOnBorrow;
	private Boolean testOnReturn;
	private Long timeBetweenEvictionRunsMillis;
	private Integer numTestsPerEvictionRun;
	private Long minEvictableIdleTimeMillis;
	private Boolean testWhileIdle;
	private String validationQuery;
	private Integer validationQueryTimeout;
	private List<String> connectionInitSqls;
	private Boolean accessToUnderlyingConnectionAllowed;
	private Properties connectionProperties;
	private ClassLoader beanClassLoader;


	public void setAccessToUnderlyingConnectionAllowed(Boolean accessToUnderlyingConnectionAllowed) {
		this.accessToUnderlyingConnectionAllowed = accessToUnderlyingConnectionAllowed;
	}

	public void setConnectionInitSqls(List<String> connectionInitSqls) {
		this.connectionInitSqls = connectionInitSqls;
	}

	public void setConnectionProperties(Properties connectionProperties) {
		this.connectionProperties = connectionProperties;
	}

	public void setDefaultAutoCommit(Boolean defaultAutoCommit) {
		this.defaultAutoCommit = defaultAutoCommit;
	}

	public void setDefaultCatalog(String defaultCatalog) {
		this.defaultCatalog = defaultCatalog;
	}

	public void setDefaultReadOnly(Boolean defaultReadOnly) {
		this.defaultReadOnly = defaultReadOnly;
	}

	public void setDefaultTransactionIsolation(Integer defaultTransactionIsolation) {
		this.defaultTransactionIsolation = defaultTransactionIsolation;
	}

	public void setInitialSize(Integer initialSize) {
		this.initialSize = initialSize;
	}

	public void setMaxActive(Integer maxActive) {
		this.maxActive = maxActive;
	}

	public void setMaxIdle(Integer maxIdle) {
		this.maxIdle = maxIdle;
	}

	public void setMaxOpenPreparedStatements(Integer maxOpenPreparedStatements) {
		this.maxOpenPreparedStatements = maxOpenPreparedStatements;
	}

	public void setMaxWait(Long maxWait) {
		this.maxWait = maxWait;
	}

	public void setMinEvictableIdleTimeMillis(Long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	public void setMinIdle(Integer minIdle) {
		this.minIdle = minIdle;
	}

	public void setNumTestsPerEvictionRun(Integer numTestsPerEvictionRun) {
		this.numTestsPerEvictionRun = numTestsPerEvictionRun;
	}

	public void setPoolPreparedStatements(Boolean poolPreparedStatements) {
		this.poolPreparedStatements = poolPreparedStatements;
	}

	public void setTestOnBorrow(Boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
	}

	public void setTestOnReturn(Boolean testOnReturn) {
		this.testOnReturn = testOnReturn;
	}

	public void setTestWhileIdle(Boolean testWhileIdle) {
		this.testWhileIdle = testWhileIdle;
	}

	public void setTimeBetweenEvictionRunsMillis(Long timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	public void setValidationQueryTimeout(Integer validationQueryTimeout) {
		this.validationQueryTimeout = validationQueryTimeout;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@SuppressWarnings("HardcodedFileSeparator")
	public DataSource createDataSource(String dataSourceClass, String hostName, Integer port, String databaseName, String userName, String password) {
		BasicDataSource basicDataSource = new BasicDataSource();
		basicDataSource.setDriverClassName(dataSourceClass);
		basicDataSource.setUrl(new StringBuilder().append("jdbc:mysql://").append(hostName).append(":").append(port).append("/").append(databaseName).toString());
		basicDataSource.setDriverClassLoader(this.beanClassLoader);
		basicDataSource.setUsername(userName);
		basicDataSource.setPassword(password);
		return applyDataSourceProperties(basicDataSource);
	}

	public DataSource applyDataSourceProperties(BasicDataSource dataSource) {
		if (this.defaultAutoCommit != null) {
			dataSource.setDefaultAutoCommit(this.defaultAutoCommit);
		}
		if (this.defaultReadOnly != null) {
			dataSource.setDefaultReadOnly(this.defaultReadOnly);
		}
		if (this.defaultTransactionIsolation != null) {
			dataSource.setDefaultTransactionIsolation(this.defaultTransactionIsolation);
		}
		if (this.defaultCatalog != null) {
			dataSource.setDefaultCatalog(this.defaultCatalog);
		}
		if (this.maxActive != null) {
			dataSource.setMaxActive(this.maxActive);
		}
		if (this.maxIdle != null) {
			dataSource.setMaxIdle(this.maxIdle);
		}
		if (this.minIdle != null) {
			dataSource.setMinIdle(this.minIdle);
		}
		if (this.initialSize != null) {
			dataSource.setInitialSize(this.initialSize);
		}
		if (this.maxWait != null) {
			dataSource.setMaxWait(this.maxWait);
		}
		if (this.poolPreparedStatements != null) {
			dataSource.setPoolPreparedStatements(this.poolPreparedStatements);
		}
		if (this.maxOpenPreparedStatements != null) {
			dataSource.setMaxOpenPreparedStatements(this.maxOpenPreparedStatements);
		}
		if (this.testOnBorrow != null) {
			dataSource.setTestOnBorrow(this.testOnBorrow);
		}
		if (this.testOnReturn != null) {
			dataSource.setTestOnReturn(this.testOnReturn);
		}
		if (this.timeBetweenEvictionRunsMillis != null) {
			dataSource.setTimeBetweenEvictionRunsMillis(this.timeBetweenEvictionRunsMillis);
		}
		if (this.numTestsPerEvictionRun != null) {
			dataSource.setNumTestsPerEvictionRun(this.numTestsPerEvictionRun);
		}
		if (this.minEvictableIdleTimeMillis != null) {
			dataSource.setMinEvictableIdleTimeMillis(this.minEvictableIdleTimeMillis);
		}
		if (this.testWhileIdle != null) {
			dataSource.setTestWhileIdle(this.testWhileIdle);
		}
		if (this.validationQuery != null) {
			dataSource.setValidationQuery(this.validationQuery);
		}
		if (this.validationQueryTimeout != null) {
			dataSource.setValidationQueryTimeout(this.validationQueryTimeout);
		}
		if (this.connectionInitSqls != null) {
			dataSource.setConnectionInitSqls(this.connectionInitSqls);
		}
		if (this.accessToUnderlyingConnectionAllowed != null) {
			dataSource.setAccessToUnderlyingConnectionAllowed(this.accessToUnderlyingConnectionAllowed);
		}
		if (this.connectionProperties != null) {
			for (String propertyName : this.connectionProperties.stringPropertyNames()) {
				dataSource.addConnectionProperty(propertyName, this.connectionProperties.getProperty(propertyName));
			}
		}

		return dataSource;
	}

	public void closeDataSource(DataSource dataSource) {
		if (dataSource instanceof BasicDataSource) {
			try {
				((BasicDataSource) dataSource).close();
			} catch (SQLException e) {
				LOGGER.debug("Could not close JDBC Connection", e);
			}
		}
	}
}
