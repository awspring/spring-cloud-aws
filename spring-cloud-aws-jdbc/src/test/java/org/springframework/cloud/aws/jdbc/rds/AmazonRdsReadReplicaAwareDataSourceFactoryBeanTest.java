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

import java.sql.Connection;

import javax.sql.DataSource;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import org.junit.Test;

import org.springframework.cloud.aws.jdbc.datasource.DataSourceFactory;
import org.springframework.cloud.aws.jdbc.datasource.DataSourceInformation;
import org.springframework.cloud.aws.jdbc.datasource.ReadOnlyRoutingDataSource;
import org.springframework.cloud.aws.jdbc.datasource.support.DatabaseType;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Agim Emruli
 */
public class AmazonRdsReadReplicaAwareDataSourceFactoryBeanTest {

	@Test
	public void afterPropertiesSet_instanceWithoutReadReplica_createsNoDataSourceRouter()
			throws Exception {
		// Arrange
		AmazonRDS amazonRDS = mock(AmazonRDS.class);
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);

		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest()
				.withDBInstanceIdentifier("test"))).thenReturn(
						new DescribeDBInstancesResult().withDBInstances(new DBInstance()
								.withDBInstanceStatus("available").withDBName("test")
								.withDBInstanceIdentifier("test").withEngine("mysql")
								.withMasterUsername("admin").withEndpoint(new Endpoint()
										.withAddress("localhost").withPort(3306))));

		AmazonRdsReadReplicaAwareDataSourceFactoryBean factoryBean = new AmazonRdsReadReplicaAwareDataSourceFactoryBean(
				amazonRDS, "test", "secret");
		factoryBean.setDataSourceFactory(dataSourceFactory);
		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret")))
						.thenReturn(mock(DataSource.class));

		// Act
		factoryBean.afterPropertiesSet();

		// Assert
		DataSource datasource = factoryBean.getObject();
		assertThat(datasource).isNotNull();

		verify(dataSourceFactory, times(1)).createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret"));
	}

	@Test
	public void afterPropertiesSet_instanceWithReadReplica_createsDataSourceRouter()
			throws Exception {
		// Arrange
		AmazonRDS amazonRDS = mock(AmazonRDS.class);
		DataSourceFactory dataSourceFactory = mock(DataSourceFactory.class);

		when(amazonRDS.describeDBInstances(
				new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
						.thenReturn(new DescribeDBInstancesResult().withDBInstances(
								new DBInstance().withDBInstanceStatus("available")
										.withDBName("test")
										.withDBInstanceIdentifier("test")
										.withEngine("mysql").withMasterUsername("admin")
										.withEndpoint(new Endpoint()
												.withAddress("localhost").withPort(3306))
										.withReadReplicaDBInstanceIdentifiers("read1",
												"read2")));

		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest()
				.withDBInstanceIdentifier("read1"))).thenReturn(
						new DescribeDBInstancesResult().withDBInstances(new DBInstance()
								.withDBInstanceStatus("available").withDBName("read1")
								.withDBInstanceIdentifier("read1").withEngine("mysql")
								.withMasterUsername("admin").withEndpoint(new Endpoint()
										.withAddress("localhost").withPort(3306))));

		when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest()
				.withDBInstanceIdentifier("read2"))).thenReturn(
						new DescribeDBInstancesResult().withDBInstances(new DBInstance()
								.withDBInstanceStatus("available").withDBName("read2")
								.withDBInstanceIdentifier("read2").withEngine("mysql")
								.withMasterUsername("admin").withEndpoint(new Endpoint()
										.withAddress("localhost").withPort(3306))));

		DataSource createdDataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);

		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret")))
						.thenReturn(createdDataSource);
		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "read1", "admin", "secret")))
						.thenReturn(createdDataSource);
		when(dataSourceFactory.createDataSource(new DataSourceInformation(
				DatabaseType.MYSQL, "localhost", 3306, "read2", "admin", "secret")))
						.thenReturn(createdDataSource);
		when(createdDataSource.getConnection()).thenReturn(connection);

		AmazonRdsReadReplicaAwareDataSourceFactoryBean factoryBean = new AmazonRdsReadReplicaAwareDataSourceFactoryBean(
				amazonRDS, "test", "secret");
		factoryBean.setDataSourceFactory(dataSourceFactory);

		// Act
		factoryBean.afterPropertiesSet();

		// Assert
		DataSource datasource = factoryBean.getObject();
		assertThat(datasource).isNotNull();
		assertThat(datasource instanceof LazyConnectionDataSourceProxy).isTrue();

		ReadOnlyRoutingDataSource source = (ReadOnlyRoutingDataSource) ((LazyConnectionDataSourceProxy) datasource)
				.getTargetDataSource();
		assertThat(source.getDataSources().size()).isEqualTo(3);
	}

}
