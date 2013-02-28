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

package org.elasticspring.jdbc.datasource;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.elasticspring.jdbc.datasource.support.DatabaseType;
import org.elasticspring.jdbc.datasource.support.MapBasedDatabasePlatformSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.transaction.TransactionDefinition;

import java.beans.PropertyDescriptor;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Unit test class for {@link TomcatJdbcDataSourceFactory}
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class TomcatJdbcDataSourceFactoryTest {

	@Test
	public void testCreateWithDefaultSettings() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();

		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL,
				"localhost", 3306, "test", "user", "password");
		DataSource dataSource = tomcatJdbcDataSourceFactory.createDataSource(dataSourceInformation);
		Assert.assertNotNull(dataSource);

		Assert.assertEquals("com.mysql.jdbc.Driver", dataSource.getDriverClassName());
		Assert.assertEquals("jdbc:mysql://localhost:3306/test", dataSource.getUrl());
		Assert.assertEquals("user", dataSource.getUsername());
	}

	@Test
	public void testWithCustomDatabasePlatformSupport() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();

		tomcatJdbcDataSourceFactory.setDatabasePlatformSupport(new MapBasedDatabasePlatformSupport() {

			@Override
			protected Map<DatabaseType, String> getDriverClassNameMappings() {
				return Collections.singletonMap(DatabaseType.MYSQL, "com.mysql.driver");
			}

			@Override
			protected Map<DatabaseType, String> getSchemeNames() {
				return Collections.singletonMap(DatabaseType.MYSQL, "jdbc:sql");
			}
		});

		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL,
				"localhost", 3306, "test", "user", "password");
		DataSource dataSource = tomcatJdbcDataSourceFactory.createDataSource(dataSourceInformation);
		Assert.assertNotNull(dataSource);

		Assert.assertEquals("com.mysql.driver", dataSource.getDriverClassName());
		Assert.assertEquals("jdbc:sql://localhost:3306/test", dataSource.getUrl());
		Assert.assertEquals("user", dataSource.getUsername());
	}

	@Test
	public void testCloseDataSource() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();
		tomcatJdbcDataSourceFactory.setInitialSize(0);

		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL,
				"localhost", 3306, "test", "user", "password");
		DataSource dataSource = tomcatJdbcDataSourceFactory.createDataSource(dataSourceInformation);
		Assert.assertNotNull(dataSource);

		ConnectionPool pool = dataSource.createPool();
		Assert.assertFalse(pool.isClosed());
		tomcatJdbcDataSourceFactory.closeDataSource(dataSource);
		Assert.assertTrue(pool.isClosed());
	}

	@Test
	public void testSetDefaultIsolationLevelName() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();
		tomcatJdbcDataSourceFactory.setDefaultTransactionIsolationName("READ_COMMITTED");

		Assert.assertEquals(Connection.TRANSACTION_READ_COMMITTED, tomcatJdbcDataSourceFactory.getDefaultTransactionIsolation());
	}

	@Test
	public void testAllPropertiesSet() throws Exception {
		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();

		tomcatJdbcDataSourceFactory.setDbProperties(new Properties());
		tomcatJdbcDataSourceFactory.setDefaultAutoCommit(true);
		tomcatJdbcDataSourceFactory.setDefaultReadOnly(false);
		tomcatJdbcDataSourceFactory.setDefaultTransactionIsolation(TransactionDefinition.ISOLATION_READ_COMMITTED);
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
		tomcatJdbcDataSourceFactory.setDataSource(new DataSource());
		tomcatJdbcDataSourceFactory.setDataSourceJNDI("foo");
		tomcatJdbcDataSourceFactory.setAlternateUsernameAllowed(true);
		tomcatJdbcDataSourceFactory.setCommitOnReturn(true);
		tomcatJdbcDataSourceFactory.setRollbackOnReturn(true);
		tomcatJdbcDataSourceFactory.setUseDisposableConnectionFacade(false);
		tomcatJdbcDataSourceFactory.setLogValidationErrors(true);
		tomcatJdbcDataSourceFactory.setPropagateInterruptState(true);

		DataSourceInformation dataSourceInformation = new DataSourceInformation(DatabaseType.MYSQL,
				"localhost", 3306, "test", "user", "password");
		DataSource dataSource = tomcatJdbcDataSourceFactory.createDataSource(dataSourceInformation);

		BeanWrapper source = PropertyAccessorFactory.forBeanPropertyAccess(tomcatJdbcDataSourceFactory);
		BeanWrapper target = PropertyAccessorFactory.forBeanPropertyAccess(dataSource.getPoolProperties());
		List<String> ignoredProperties = Arrays.asList("driverClassName", "url", "username", "password");

		for (PropertyDescriptor propertyDescriptor : source.getPropertyDescriptors()) {
			if (propertyDescriptor.getWriteMethod() != null && target.isReadableProperty(propertyDescriptor.getName()) && !ignoredProperties.contains(propertyDescriptor.getName())) {
				Assert.assertEquals(source.getPropertyValue(propertyDescriptor.getName()), target.getPropertyValue(propertyDescriptor.getName()));
			}
		}

	}

	@Test //Test that the setters are not usable which will be configured at runtime during datasource creation
	public void testInvalidPoolAttributes() throws Exception {

		TomcatJdbcDataSourceFactory tomcatJdbcDataSourceFactory = new TomcatJdbcDataSourceFactory();

		try {
			tomcatJdbcDataSourceFactory.setDriverClassName("foo");
			Assert.fail("Expecting IllegalStateException");
		} catch (UnsupportedOperationException e) {
			Assert.assertTrue(e.getMessage().contains("at runtime"));
		}

		try {
			tomcatJdbcDataSourceFactory.setUrl("foo");
			Assert.fail("Expecting IllegalStateException");
		} catch (UnsupportedOperationException e) {
			Assert.assertTrue(e.getMessage().contains("at runtime"));
		}

		try {
			tomcatJdbcDataSourceFactory.setUsername("foo");
			Assert.fail("Expecting IllegalStateException");
		} catch (UnsupportedOperationException e) {
			Assert.assertTrue(e.getMessage().contains("at runtime"));
		}

		try {
			tomcatJdbcDataSourceFactory.setPassword("foo");
			Assert.fail("Expecting IllegalStateException");
		} catch (UnsupportedOperationException e) {
			Assert.assertTrue(e.getMessage().contains("at runtime"));
		}
	}
}