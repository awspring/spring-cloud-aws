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

package org.springframework.cloud.aws.jdbc.rds;

import javax.sql.DataSource;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.jdbc.datasource.DataSourceFactory;
import org.springframework.cloud.aws.jdbc.datasource.DataSourceInformation;
import org.springframework.cloud.aws.jdbc.datasource.support.DatabaseType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AmazonRdsDataSourceFactoryBean}.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonRdsDataSourceFactoryBeanTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void afterPropertiesSet_noInstanceFound_reportsIllegalStateException()
			throws Exception {
		// Arrange
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("No database instance with id:'test'");

		AmazonRDS amazonRDS = mock(AmazonRDS.class);
		when(amazonRDS.describeDBInstances(
				new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
						.thenThrow(new DBInstanceNotFoundException("foo"));

		AmazonRdsDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(
				amazonRDS, "test", "foo");

		// Act
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();

		// Assert
	}

	@Test
	public void newInstance_withResourceIdResolver_createsInstanceWithResolvedName()
			throws Exception {
		// Arrange
		AmazonRDS amazonRDS = mock(AmazonRDS.class);
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
		ResourceIdResolver resourceIdResolver = mock(ResourceIdResolver.class);
		DataSource dataSource = mock(DataSource.class);

		when(resourceIdResolver.resolveToPhysicalResourceId("test")).thenReturn("bar");

		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest()
				.withDBInstanceIdentifier("bar"))).thenReturn(
						new DescribeDBInstancesResult().withDBInstances(new DBInstance()
								.withDBInstanceStatus("available").withDBName("test")
								.withDBInstanceIdentifier("bar").withEngine("mysql")
								.withMasterUsername("admin").withEndpoint(new Endpoint()
										.withAddress("localhost").withPort(3306))));

		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret")))
						.thenReturn(dataSource);

		AmazonRdsDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(
				amazonRDS, "test", "secret");
		amazonRdsDataSourceFactoryBean.setDataSourceFactory(dataSourceFactory);
		amazonRdsDataSourceFactoryBean.setResourceIdResolver(resourceIdResolver);

		// Act
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();

		// Assert
		assertThat(amazonRdsDataSourceFactoryBean.getObject()).isNotNull();

		verify(dataSourceFactory, times(1)).createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret"));
	}

	@Test
	public void afterPropertiesSet_noUserNameSet_createsInstanceWithUserNameFromMetaData()
			throws Exception {
		// Arrange
		AmazonRDS amazonRDS = mock(AmazonRDS.class);
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
		DataSource dataSource = mock(DataSource.class);

		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest()
				.withDBInstanceIdentifier("test"))).thenReturn(
						new DescribeDBInstancesResult().withDBInstances(new DBInstance()
								.withDBInstanceStatus("available").withDBName("test")
								.withDBInstanceIdentifier("test").withEngine("mysql")
								.withMasterUsername("admin").withEndpoint(new Endpoint()
										.withAddress("localhost").withPort(3306))));

		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret")))
						.thenReturn(dataSource);

		AmazonRdsDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(
				amazonRDS, "test", "secret");
		amazonRdsDataSourceFactoryBean.setDataSourceFactory(dataSourceFactory);

		// Act
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();

		// Assert
		DataSource datasource = amazonRdsDataSourceFactoryBean.getObject();
		assertThat(datasource).isNotNull();

		verify(dataSourceFactory, times(1)).createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret"));
	}

	@Test
	public void destroyInstance_shutdownInitiated_destroysDynamicDataSource()
			throws Exception {
		AmazonRDS amazonRDS = mock(AmazonRDS.class);
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);
		DataSource dataSource = mock(DataSource.class);

		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest()
				.withDBInstanceIdentifier("test"))).thenReturn(
						new DescribeDBInstancesResult().withDBInstances(new DBInstance()
								.withDBInstanceStatus("available").withDBName("test")
								.withDBInstanceIdentifier("test").withEngine("mysql")
								.withMasterUsername("admin").withEndpoint(new Endpoint()
										.withAddress("localhost").withPort(3306))));

		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret")))
						.thenReturn(dataSource);

		AmazonRdsDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(
				amazonRDS, "test", "secret");
		amazonRdsDataSourceFactoryBean.setDataSourceFactory(dataSourceFactory);
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();

		amazonRdsDataSourceFactoryBean.getObject();

		amazonRdsDataSourceFactoryBean.destroy();

		verify(dataSourceFactory, times(1)).closeDataSource(dataSource);
	}

	@Test
	public void afterPropertiesSet_customUserNameSet_createsInstanceWithCustomUserNameAndIgnoresMetaDataUserName()
			throws Exception {
		AmazonRDS amazonRDS = mock(AmazonRDS.class);
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);

		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest()
				.withDBInstanceIdentifier("test"))).thenReturn(
						new DescribeDBInstancesResult().withDBInstances(new DBInstance()
								.withDBInstanceStatus("available").withDBName("test")
								.withDBInstanceIdentifier("test").withEngine("mysql")
								.withMasterUsername("admin").withEndpoint(new Endpoint()
										.withAddress("localhost").withPort(3306))));

		AmazonRdsDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsDataSourceFactoryBean(
				amazonRDS, "test", "secret");
		amazonRdsDataSourceFactoryBean.setUsername("superAdmin");
		amazonRdsDataSourceFactoryBean.setDataSourceFactory(dataSourceFactory);
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();
		amazonRdsDataSourceFactoryBean.getObject();

		verify(dataSourceFactory, times(1)).createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "superAdmin", "secret"));
	}

}
