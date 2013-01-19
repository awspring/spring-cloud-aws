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

package org.elasticspring.jdbc.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import org.elasticspring.jdbc.datasource.DataSourceFactory;
import org.elasticspring.jdbc.datasource.DataSourceInformation;
import org.elasticspring.jdbc.datasource.support.DatabaseType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.core.task.SyncTaskExecutor;

import javax.sql.DataSource;

/**
 * Unit test for {@link AmazonRdsDataSourceFactoryBean}
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonRdsDataSourceFactoryBeanTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();


	@Test
	public void testDataBaseInstanceNotFound() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("No database instance with id:'test'");

		AmazonRDS amazonRDS = Mockito.mock(AmazonRDS.class);
		Mockito.when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test"))).thenThrow(new DBInstanceNotFoundException("foo"));

		AmazonRdsDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(amazonRDS, "test", "foo");
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();
		amazonRdsDataSourceFactoryBean.getObject();
	}

	@Test
	public void testUserNameNotSet() throws Exception {
		AmazonRDS amazonRDS = Mockito.mock(AmazonRDS.class);
		DataSourceFactory dataSourceFactory = Mockito.mock(DataSourceFactory.class);

		Mockito.when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test"))).thenReturn(
				new DescribeDBInstancesResult().
						withDBInstances(new DBInstance().
								withDBInstanceStatus("available").
								withDBName("test").
								withDBInstanceIdentifier("test").
								withEngine("mysql").
								withMasterUsername("admin").
								withEndpoint(new Endpoint().
										withAddress("localhost").
										withPort(3306)
								)
						)
		);

		AmazonRdsDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(amazonRDS, "test", "secret");
		amazonRdsDataSourceFactoryBean.setDataSourceFactory(dataSourceFactory);
		amazonRdsDataSourceFactoryBean.setTaskExecutor(new SyncTaskExecutor());
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();
		amazonRdsDataSourceFactoryBean.getObject();


		Mockito.verify(dataSourceFactory, Mockito.times(1)).createDataSource(new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret"));
	}

	@Test
	public void testDestroyInstance() throws Exception {
		AmazonRDS amazonRDS = Mockito.mock(AmazonRDS.class);
		DataSourceFactory dataSourceFactory = Mockito.mock(DataSourceFactory.class);
		DataSource dataSource = Mockito.mock(DataSource.class);

		Mockito.when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test"))).thenReturn(
				new DescribeDBInstancesResult().
						withDBInstances(new DBInstance().
								withDBInstanceStatus("available").
								withDBName("test").
								withDBInstanceIdentifier("test").
								withEngine("mysql").
								withMasterUsername("admin").
								withEndpoint(new Endpoint().
										withAddress("localhost").
										withPort(3306)
								)
						)
		);

		Mockito.when(dataSourceFactory.createDataSource(new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret"))).thenReturn(dataSource);

		AmazonRdsDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(amazonRDS, "test", "secret");
		amazonRdsDataSourceFactoryBean.setDataSourceFactory(dataSourceFactory);
		amazonRdsDataSourceFactoryBean.setTaskExecutor(new SyncTaskExecutor());
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();

		amazonRdsDataSourceFactoryBean.getObject();

		amazonRdsDataSourceFactoryBean.destroy();

		Mockito.verify(dataSourceFactory, Mockito.times(1)).closeDataSource(dataSource);
	}

	@Test
	public void testUserNameSet() throws Exception {
		AmazonRDS amazonRDS = Mockito.mock(AmazonRDS.class);
		DataSourceFactory dataSourceFactory = Mockito.mock(DataSourceFactory.class);

		Mockito.when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test"))).thenReturn(
				new DescribeDBInstancesResult().
						withDBInstances(new DBInstance().
								withDBInstanceStatus("available").
								withDBName("test").
								withDBInstanceIdentifier("test").
								withEngine("mysql").
								withMasterUsername("admin").
								withEndpoint(new Endpoint().
										withAddress("localhost").
										withPort(3306)
								)
						)
		);

		AmazonRdsDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(amazonRDS, "test", "secret");
		amazonRdsDataSourceFactoryBean.setUsername("superAdmin");
		amazonRdsDataSourceFactoryBean.setDataSourceFactory(dataSourceFactory);
		amazonRdsDataSourceFactoryBean.setTaskExecutor(new SyncTaskExecutor());
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();
		amazonRdsDataSourceFactoryBean.getObject();


		Mockito.verify(dataSourceFactory, Mockito.times(1)).createDataSource(new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "test", "superAdmin", "secret"));
	}

	@Test
	public void testAmazonRdsInstanceStatus() throws Exception {
		AmazonRDS amazonRDS = Mockito.mock(AmazonRDS.class);
		AmazonRdsDataSourceFactoryBean.AmazonRdsInstanceStatus amazonRdsInstanceStatus = new AmazonRdsDataSourceFactoryBean.AmazonRdsInstanceStatus(amazonRDS, "test");

		Mockito.when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test"))).thenReturn(
				new DescribeDBInstancesResult().
						withDBInstances(new DBInstance().withDBInstanceStatus("available")), new DescribeDBInstancesResult().withDBInstances(new DBInstance().withDBInstanceStatus("rebooting")));

		Assert.assertTrue(amazonRdsInstanceStatus.isDataSourceAvailable());
		Assert.assertFalse(amazonRdsInstanceStatus.isDataSourceAvailable());
	}
}
