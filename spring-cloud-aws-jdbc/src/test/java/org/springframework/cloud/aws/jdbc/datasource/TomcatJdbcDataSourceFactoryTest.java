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

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.cloud.aws.jdbc.datasource.support.DatabaseType;
import org.springframework.cloud.aws.jdbc.datasource.support.MapBasedDatabasePlatformSupport;
import org.springframework.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Unit test class for {@link TomcatJdbcDataSourceFactory}.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class TomcatJdbcDataSourceFactoryTest {

	@Test
	public void testCreateWithDefaultSettings() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();

		DataSourceInformation dataSourceInformation = new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "user", "password");
		DataSource dataSource = tomcatJdbcDataSourceFactory
				.createDataSource(dataSourceInformation);
		assertThat(dataSource).isNotNull();

		assertThat(dataSource.getDriverClassName()).isEqualTo("com.mysql.jdbc.Driver");
		assertThat(dataSource.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/test");
		assertThat(dataSource.getUsername()).isEqualTo("user");
	}

	@Test
	public void testWithCustomDatabasePlatformSupport() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();

		tomcatJdbcDataSourceFactory
				.setDatabasePlatformSupport(new MapBasedDatabasePlatformSupport() {

					@Override
					protected Map<DatabaseType, String> getDriverClassNameMappings() {
						return Collections.singletonMap(DatabaseType.MYSQL,
								"com.mysql.driver");
					}

					@Override
					protected Map<DatabaseType, String> getSchemeNames() {
						return Collections.singletonMap(DatabaseType.MYSQL, "jdbc:sql");
					}

					@Override
					protected Map<DatabaseType, String> getAuthenticationInfo() {
						return Collections.emptyMap();
					}
				});

		DataSourceInformation dataSourceInformation = new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "user", "password");
		DataSource dataSource = tomcatJdbcDataSourceFactory
				.createDataSource(dataSourceInformation);
		assertThat(dataSource).isNotNull();

		assertThat(dataSource.getDriverClassName()).isEqualTo("com.mysql.driver");
		assertThat(dataSource.getUrl()).isEqualTo("jdbc:sql://localhost:3306/test");
		assertThat(dataSource.getUsername()).isEqualTo("user");
	}

	@Test
	public void testCloseDataSource() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();
		tomcatJdbcDataSourceFactory.setInitialSize(0);

		DataSourceInformation dataSourceInformation = new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "user", "password");
		DataSource dataSource = tomcatJdbcDataSourceFactory
				.createDataSource(dataSourceInformation);
		assertThat(dataSource).isNotNull();

		ConnectionPool pool = dataSource.createPool();
		assertThat(pool.isClosed()).isFalse();
		tomcatJdbcDataSourceFactory.closeDataSource(dataSource);
		assertThat(pool.isClosed()).isTrue();
	}

	@Test
	public void testSetDefaultIsolationLevelName() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();
		tomcatJdbcDataSourceFactory.setDefaultTransactionIsolationName("READ_COMMITTED");

		assertThat(tomcatJdbcDataSourceFactory.getDefaultTransactionIsolation())
				.isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
	}

	@Test
	public void testAllPropertiesSet() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();

		tomcatJdbcDataSourceFactory.setDbProperties(new Properties());
		tomcatJdbcDataSourceFactory.setDefaultAutoCommit(true);
		tomcatJdbcDataSourceFactory.setDefaultReadOnly(false);
		tomcatJdbcDataSourceFactory.setDefaultTransactionIsolation(
				TransactionDefinition.ISOLATION_READ_COMMITTED);
		tomcatJdbcDataSourceFactory.setDefaultCatalog("myCatalog");
		tomcatJdbcDataSourceFactory.setConnectionProperties("foo=bar");
		tomcatJdbcDataSourceFactory.setInitialSize(11);
		tomcatJdbcDataSourceFactory.setMaxActive(100);
		tomcatJdbcDataSourceFactory.setMaxIdle(110);
		tomcatJdbcDataSourceFactory.setMinIdle(10);
		tomcatJdbcDataSourceFactory.setMaxWait(23);
		tomcatJdbcDataSourceFactory.setValidationQuery("SELECT 1");
		tomcatJdbcDataSourceFactory.setTestOnBorrow(true);
		tomcatJdbcDataSourceFactory.setTestOnReturn(true);
		tomcatJdbcDataSourceFactory.setTestWhileIdle(true);
		tomcatJdbcDataSourceFactory.setTimeBetweenEvictionRunsMillis(100);
		tomcatJdbcDataSourceFactory.setNumTestsPerEvictionRun(100);
		tomcatJdbcDataSourceFactory.setMinEvictableIdleTimeMillis(1000);
		tomcatJdbcDataSourceFactory.setAccessToUnderlyingConnectionAllowed(false);
		tomcatJdbcDataSourceFactory.setRemoveAbandoned(true);
		tomcatJdbcDataSourceFactory.setLogAbandoned(true);

		tomcatJdbcDataSourceFactory.setValidationInterval(10000);
		tomcatJdbcDataSourceFactory.setJmxEnabled(true);
		tomcatJdbcDataSourceFactory.setInitSQL("SET SCHEMA");
		tomcatJdbcDataSourceFactory.setTestOnConnect(true);
		tomcatJdbcDataSourceFactory.setJdbcInterceptors("foo");
		tomcatJdbcDataSourceFactory.setFairQueue(false);
		tomcatJdbcDataSourceFactory.setUseEquals(false);
		tomcatJdbcDataSourceFactory.setAbandonWhenPercentageFull(80);
		tomcatJdbcDataSourceFactory.setMaxAge(100);
		tomcatJdbcDataSourceFactory.setUseLock(true);
		tomcatJdbcDataSourceFactory.setSuspectTimeout(200);
		tomcatJdbcDataSourceFactory.setDataSourceJNDI("foo");
		tomcatJdbcDataSourceFactory.setAlternateUsernameAllowed(true);
		tomcatJdbcDataSourceFactory.setCommitOnReturn(true);
		tomcatJdbcDataSourceFactory.setRollbackOnReturn(true);
		tomcatJdbcDataSourceFactory.setUseDisposableConnectionFacade(false);
		tomcatJdbcDataSourceFactory.setLogValidationErrors(true);
		tomcatJdbcDataSourceFactory.setPropagateInterruptState(true);

		DataSourceInformation dataSourceInformation = new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "user", "password");
		DataSource dataSource = tomcatJdbcDataSourceFactory
				.createDataSource(dataSourceInformation);

		BeanWrapper source = PropertyAccessorFactory
				.forBeanPropertyAccess(tomcatJdbcDataSourceFactory);
		BeanWrapper target = PropertyAccessorFactory
				.forBeanPropertyAccess(dataSource.getPoolProperties());
		List<String> ignoredProperties = Arrays.asList("driverClassName", "url",
				"username", "password");

		for (PropertyDescriptor propertyDescriptor : source.getPropertyDescriptors()) {
			if (propertyDescriptor.getWriteMethod() != null
					&& target.isReadableProperty(propertyDescriptor.getName())
					&& !ignoredProperties.contains(propertyDescriptor.getName())) {
				assertThat(target.getPropertyValue(propertyDescriptor.getName()))
						.isEqualTo(source.getPropertyValue(propertyDescriptor.getName()));
			}
		}

	}

	@Test // Test that the setters are not usable which will be configured at runtime
			// during datasource creation
	public void testInvalidPoolAttributes() throws Exception {

		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();

		try {
			tomcatJdbcDataSourceFactory.setDriverClassName("foo");
			fail("Expecting IllegalStateException");
		}
		catch (UnsupportedOperationException e) {
			assertThat(e.getMessage().contains("at runtime")).isTrue();
		}

		try {
			tomcatJdbcDataSourceFactory.setUrl("foo");
			fail("Expecting IllegalStateException");
		}
		catch (UnsupportedOperationException e) {
			assertThat(e.getMessage().contains("at runtime")).isTrue();
		}

		try {
			tomcatJdbcDataSourceFactory.setUsername("foo");
			fail("Expecting IllegalStateException");
		}
		catch (UnsupportedOperationException e) {
			assertThat(e.getMessage().contains("at runtime")).isTrue();
		}

		try {
			tomcatJdbcDataSourceFactory.setPassword("foo");
			fail("Expecting IllegalStateException");
		}
		catch (UnsupportedOperationException e) {
			assertThat(e.getMessage().contains("at runtime")).isTrue();
		}
	}

}
