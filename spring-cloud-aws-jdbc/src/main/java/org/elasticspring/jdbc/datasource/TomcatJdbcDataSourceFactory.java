/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.jdbc.datasource;

import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.Validator;
import org.elasticspring.jdbc.datasource.support.DatabasePlatformSupport;
import org.elasticspring.jdbc.datasource.support.StaticDatabasePlatformSupport;
import org.springframework.beans.BeanUtils;
import org.springframework.core.Constants;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

/**
 * A Tomcat JDBC Pool {@link DataSourceFactory} implementation that creates a JDBC pool backed datasource. Allows the
 * configuration of all configuration properties except username, password, url and driver class name because they are
 * passed in while actually creating the datasource. All other properties can be modified by calling the
 * respective setter methods. This class uses a {@link DatabasePlatformSupport} implementation to actually retrieve the
 * driver class name and url in order to create the datasource.
 * <p>All properties are derived from {@link PoolConfiguration} of the Tomcat JDBC Pool class.</p>
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class TomcatJdbcDataSourceFactory implements DataSourceFactory, PoolConfiguration {

	private static final String PREFIX_ISOLATION = "TRANSACTION_";

	private final PoolProperties defaultPoolConfiguration = new PoolProperties();
	private DatabasePlatformSupport databasePlatformSupport = new StaticDatabasePlatformSupport();

	@Override
	public void setAbandonWhenPercentageFull(int percentage) {
		this.defaultPoolConfiguration.setAbandonWhenPercentageFull(percentage);
	}

	@Override
	public void setTestOnReturn(boolean testOnReturn) {
		this.defaultPoolConfiguration.setTestOnReturn(testOnReturn);
	}

	@Override
	public void setMaxAge(long maxAge) {
		this.defaultPoolConfiguration.setMaxAge(maxAge);
	}

	@Override
	public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
		this.defaultPoolConfiguration.setRemoveAbandonedTimeout(removeAbandonedTimeout);
	}

	@Override
	public int getAbandonWhenPercentageFull() {
		return this.defaultPoolConfiguration.getAbandonWhenPercentageFull();
	}

	@Override
	public void setUsername(String username) {
		throw new UnsupportedOperationException("Username will be set at runtime!");
	}

	@Override
	public void setValidator(Validator validator) {
		this.defaultPoolConfiguration.setValidator(validator);
	}

	@Override
	public PoolProperties.InterceptorDefinition[] getJdbcInterceptorsAsArray() {
		return this.defaultPoolConfiguration.getJdbcInterceptorsAsArray();
	}

	@Override
	public String getName() {
		return this.defaultPoolConfiguration.getName();
	}

	@Override
	public Boolean isDefaultAutoCommit() {
		return this.defaultPoolConfiguration.isDefaultAutoCommit();
	}

	@Override
	public void setMaxActive(int maxActive) {
		this.defaultPoolConfiguration.setMaxActive(maxActive);
	}

	@Override
	public void setValidatorClassName(String className) {
		this.defaultPoolConfiguration.setValidatorClassName(className);
	}

	@Override
	public String getDefaultCatalog() {
		return this.defaultPoolConfiguration.getDefaultCatalog();
	}

	@Override
	public int getTimeBetweenEvictionRunsMillis() {
		return this.defaultPoolConfiguration.getTimeBetweenEvictionRunsMillis();
	}

	@Override
	public boolean getUseDisposableConnectionFacade() {
		return this.defaultPoolConfiguration.getUseDisposableConnectionFacade();
	}

	@Override
	public int getMinIdle() {
		return this.defaultPoolConfiguration.getMinIdle();
	}

	@Override
	public boolean isPoolSweeperEnabled() {
		return this.defaultPoolConfiguration.isPoolSweeperEnabled();
	}

	@Override
	public boolean isUseEquals() {
		return this.defaultPoolConfiguration.isUseEquals();
	}

	@Override
	public void setSuspectTimeout(int seconds) {
		this.defaultPoolConfiguration.setSuspectTimeout(seconds);
	}

	@Override
	public void setAlternateUsernameAllowed(boolean alternateUsernameAllowed) {
		this.defaultPoolConfiguration.setAlternateUsernameAllowed(alternateUsernameAllowed);
	}

	@Override
	public void setDefaultReadOnly(Boolean defaultReadOnly) {
		this.defaultPoolConfiguration.setDefaultReadOnly(defaultReadOnly);
	}

	@Override
	public void setJdbcInterceptors(String jdbcInterceptors) {
		this.defaultPoolConfiguration.setJdbcInterceptors(jdbcInterceptors);
	}

	@Override
	public int getMaxActive() {
		return this.defaultPoolConfiguration.getMaxActive();
	}

	@Override
	public void setTestWhileIdle(boolean testWhileIdle) {
		this.defaultPoolConfiguration.setTestWhileIdle(testWhileIdle);
	}

	@Override
	public void setMaxWait(int maxWait) {
		this.defaultPoolConfiguration.setMaxWait(maxWait);
	}

	@Override
	public Properties getDbProperties() {
		return this.defaultPoolConfiguration.getDbProperties();
	}

	@Override
	public int getRemoveAbandonedTimeout() {
		return this.defaultPoolConfiguration.getRemoveAbandonedTimeout();
	}

	@Override
	public void setUseLock(boolean useLock) {
		this.defaultPoolConfiguration.setUseLock(useLock);
	}

	@Override
	public boolean isJmxEnabled() {
		return this.defaultPoolConfiguration.isJmxEnabled();
	}

	@Override
	public void setPassword(String password) {
		throw new UnsupportedOperationException("Password will be set at runtime!");
	}

	@Override
	public boolean isTestOnBorrow() {
		return this.defaultPoolConfiguration.isTestOnBorrow();
	}

	@Override
	public void setDataSourceJNDI(String jndiDS) {
		this.defaultPoolConfiguration.setDataSourceJNDI(jndiDS);
	}

	@Override
	public Boolean getDefaultAutoCommit() {
		return this.defaultPoolConfiguration.getDefaultAutoCommit();
	}

	@Override
	public int getNumTestsPerEvictionRun() {
		return this.defaultPoolConfiguration.getNumTestsPerEvictionRun();
	}

	@Override
	public void setTestOnBorrow(boolean testOnBorrow) {
		this.defaultPoolConfiguration.setTestOnBorrow(testOnBorrow);
	}

	@Override
	public void setJmxEnabled(boolean jmxEnabled) {
		this.defaultPoolConfiguration.setJmxEnabled(jmxEnabled);
	}

	@Override
	public int getMinEvictableIdleTimeMillis() {
		return this.defaultPoolConfiguration.getMinEvictableIdleTimeMillis();
	}

	@Override
	public String getPoolName() {
		return this.defaultPoolConfiguration.getPoolName();
	}

	@Override
	public boolean isLogAbandoned() {
		return this.defaultPoolConfiguration.isLogAbandoned();
	}

	@Override
	public void setInitSQL(String initSQL) {
		this.defaultPoolConfiguration.setInitSQL(initSQL);
	}

	@Override
	public boolean isRemoveAbandoned() {
		return this.defaultPoolConfiguration.isRemoveAbandoned();
	}

	@Override
	public boolean isAccessToUnderlyingConnectionAllowed() {
		return this.defaultPoolConfiguration.isAccessToUnderlyingConnectionAllowed();
	}

	@Override
	public Validator getValidator() {
		return this.defaultPoolConfiguration.getValidator();
	}

	@Override
	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
		this.defaultPoolConfiguration.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
	}

	@Override
	public boolean isFairQueue() {
		return this.defaultPoolConfiguration.isFairQueue();
	}

	@Override
	public void setRemoveAbandoned(boolean removeAbandoned) {
		this.defaultPoolConfiguration.setRemoveAbandoned(removeAbandoned);
	}

	@Override
	public void setTestOnConnect(boolean testOnConnect) {
		this.defaultPoolConfiguration.setTestOnConnect(testOnConnect);
	}

	@Override
	public Object getDataSource() {
		return this.defaultPoolConfiguration.getDataSource();
	}

	@Override
	public void setInitialSize(int initialSize) {
		this.defaultPoolConfiguration.setInitialSize(initialSize);
	}

	@Override
	public boolean getPropagateInterruptState() {
		return this.defaultPoolConfiguration.getPropagateInterruptState();
	}

	@Override
	public void setLogValidationErrors(boolean logValidationErrors) {
		this.defaultPoolConfiguration.setLogValidationErrors(logValidationErrors);
	}

	@Override
	public boolean getRollbackOnReturn() {
		return this.defaultPoolConfiguration.getRollbackOnReturn();
	}

	@Override
	public void setTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
		this.defaultPoolConfiguration.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
	}

	@Override
	public long getMaxAge() {
		return this.defaultPoolConfiguration.getMaxAge();
	}

	@Override
	public Boolean isDefaultReadOnly() {
		return this.defaultPoolConfiguration.isDefaultReadOnly();
	}

	@Override
	public String getConnectionProperties() {
		return this.defaultPoolConfiguration.getConnectionProperties();
	}

	@Override
	public void setDbProperties(Properties dbProperties) {
		this.defaultPoolConfiguration.setDbProperties(dbProperties);
	}

	@Override
	public void setDefaultAutoCommit(Boolean defaultAutoCommit) {
		this.defaultPoolConfiguration.setDefaultAutoCommit(defaultAutoCommit);
	}

	@Override
	public Boolean getDefaultReadOnly() {
		return this.defaultPoolConfiguration.getDefaultReadOnly();
	}

	@Override
	public void setUseDisposableConnectionFacade(boolean useDisposableConnectionFacade) {
		this.defaultPoolConfiguration.setUseDisposableConnectionFacade(useDisposableConnectionFacade);
	}

	@Override
	public boolean isTestWhileIdle() {
		return this.defaultPoolConfiguration.isTestWhileIdle();
	}

	@Override
	public void setMinIdle(int minIdle) {
		this.defaultPoolConfiguration.setMinIdle(minIdle);
	}

	@Override
	public void setDefaultCatalog(String defaultCatalog) {
		this.defaultPoolConfiguration.setDefaultCatalog(defaultCatalog);
	}

	@Override
	public boolean getLogValidationErrors() {
		return this.defaultPoolConfiguration.getLogValidationErrors();
	}

	@Override
	public void setDriverClassName(String driverClassName) {
		throw new UnsupportedOperationException("Will be set at runtime!");
	}

	@Override
	public void setUseEquals(boolean useEquals) {
		this.defaultPoolConfiguration.setUseEquals(useEquals);
	}

	@Override
	public String getDataSourceJNDI() {
		return this.defaultPoolConfiguration.getDataSourceJNDI();
	}

	@Override
	public void setRollbackOnReturn(boolean rollbackOnReturn) {
		this.defaultPoolConfiguration.setRollbackOnReturn(rollbackOnReturn);
	}

	@Override
	public String getValidationQuery() {
		return this.defaultPoolConfiguration.getValidationQuery();
	}

	@Override
	public void setLogAbandoned(boolean logAbandoned) {
		this.defaultPoolConfiguration.setLogAbandoned(logAbandoned);
	}

	@Override
	public void setName(String name) {
		this.defaultPoolConfiguration.setName(name);
	}

	@Override
	public void setUrl(String url) {
		throw new UnsupportedOperationException("Will be set at runtime!");
	}

	@Override
	public void setDataSource(Object ds) {
		this.defaultPoolConfiguration.setDataSource(ds);
	}

	@Override
	public void setCommitOnReturn(boolean commitOnReturn) {
		this.defaultPoolConfiguration.setCommitOnReturn(commitOnReturn);
	}

	@Override
	public long getValidationInterval() {
		return this.defaultPoolConfiguration.getValidationInterval();
	}

	@Override
	public boolean isAlternateUsernameAllowed() {
		return this.defaultPoolConfiguration.isAlternateUsernameAllowed();
	}

	@Override
	public void setPropagateInterruptState(boolean propagateInterruptState) {
		this.defaultPoolConfiguration.setPropagateInterruptState(propagateInterruptState);
	}

	@Override
	public String getPassword() {
		return this.defaultPoolConfiguration.getPassword();
	}

	@Override
	public String getUrl() {
		return this.defaultPoolConfiguration.getUrl();
	}

	@Override
	public int getInitialSize() {
		return this.defaultPoolConfiguration.getInitialSize();
	}

	@Override
	public String getDriverClassName() {
		return this.defaultPoolConfiguration.getDriverClassName();
	}

	@Override
	public String getValidatorClassName() {
		return this.defaultPoolConfiguration.getValidatorClassName();
	}

	@Override
	public int getMaxWait() {
		return this.defaultPoolConfiguration.getMaxWait();
	}

	@Override
	public void setValidationInterval(long validationInterval) {
		this.defaultPoolConfiguration.setValidationInterval(validationInterval);
	}

	@Override
	public int getDefaultTransactionIsolation() {
		return this.defaultPoolConfiguration.getDefaultTransactionIsolation();
	}

	@Override
	public boolean isTestOnConnect() {
		return this.defaultPoolConfiguration.isTestOnConnect();
	}

	@Override
	public String getUsername() {
		return this.defaultPoolConfiguration.getUsername();
	}

	@Override
	public int getSuspectTimeout() {
		return this.defaultPoolConfiguration.getSuspectTimeout();
	}

	@Override
	public void setMaxIdle(int maxIdle) {
		this.defaultPoolConfiguration.setMaxIdle(maxIdle);
	}

	@Override
	public boolean getUseLock() {
		return this.defaultPoolConfiguration.getUseLock();
	}

	@Override
	public String getJdbcInterceptors() {
		return this.defaultPoolConfiguration.getJdbcInterceptors();
	}

	@Override
	public String getInitSQL() {
		return this.defaultPoolConfiguration.getInitSQL();
	}

	@Override
	public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
		this.defaultPoolConfiguration.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
	}

	@Override
	public void setFairQueue(boolean fairQueue) {
		this.defaultPoolConfiguration.setFairQueue(fairQueue);
	}

	@Override
	public void setConnectionProperties(String connectionProperties) {
		this.defaultPoolConfiguration.setConnectionProperties(connectionProperties);
	}

	@Override
	public boolean isTestOnReturn() {
		return this.defaultPoolConfiguration.isTestOnReturn();
	}

	@Override
	public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
		this.defaultPoolConfiguration.setDefaultTransactionIsolation(defaultTransactionIsolation);
	}

	public void setDefaultTransactionIsolationName(String constantName) {
		if (constantName == null) {
			throw new IllegalArgumentException("Isolation name must not be null");
		}
		Constants constants = new Constants(Connection.class);
		setDefaultTransactionIsolation(constants.asNumber(PREFIX_ISOLATION + constantName).intValue());
	}

	@Override
	public boolean getCommitOnReturn() {
		return this.defaultPoolConfiguration.getCommitOnReturn();
	}

	@Override
	public int getMaxIdle() {
		return this.defaultPoolConfiguration.getMaxIdle();
	}

	@Override
	public void setAccessToUnderlyingConnectionAllowed(boolean accessToUnderlyingConnectionAllowed) {
		this.defaultPoolConfiguration.setAccessToUnderlyingConnectionAllowed(accessToUnderlyingConnectionAllowed);
	}

	@Override
	public void setValidationQuery(String validationQuery) {
		this.defaultPoolConfiguration.setValidationQuery(validationQuery);
	}

	public void setDatabasePlatformSupport(DatabasePlatformSupport databasePlatformSupport) {
		this.databasePlatformSupport = databasePlatformSupport;
	}

	@Override
	public org.apache.tomcat.jdbc.pool.DataSource createDataSource(DataSourceInformation dataSourceInformation) {
		//create a method scoped instance
		PoolConfiguration configurationToUse = new PoolProperties();

		//copy all general properties
		BeanUtils.copyProperties(this.defaultPoolConfiguration, configurationToUse);

		configurationToUse.setDriverClassName(this.databasePlatformSupport.getDriverClassNameForDatabase(dataSourceInformation.getDatabaseType()));
		configurationToUse.setUrl(this.databasePlatformSupport.getDatabaseUrlForDatabase(dataSourceInformation.getDatabaseType(),
				dataSourceInformation.getHostName(), dataSourceInformation.getPort(), dataSourceInformation.getDatabaseName()));
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