/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.jdbc.rds;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import org.elasticspring.jdbc.datasource.DataSourceFactory;
import org.elasticspring.jdbc.datasource.DataSourceInformation;
import org.elasticspring.jdbc.datasource.DynamicDataSource;
import org.elasticspring.jdbc.datasource.ReadOnlyRoutingDataSource;
import org.elasticspring.jdbc.datasource.support.DatabaseType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author Agim Emruli
 */
public class AmazonRdsReadReplicaAwareDataSourceFactoryBeanTest {

	@Test
	public void afterPropertiesSet_instanceWithoutReadReplica_createsNoDataSourceRouter() throws Exception {
		//Arrange
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

		AmazonRdsReadReplicaAwareDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsReadReplicaAwareDataSourceFactoryBean(amazonRDS, "test", "secret");
		amazonRdsDataSourceFactoryBean.setDataSourceFactory(dataSourceFactory);
		amazonRdsDataSourceFactoryBean.setTaskExecutor(new SyncTaskExecutor());

		//Act
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();

		//Assert
		DataSource datasource = amazonRdsDataSourceFactoryBean.getObject();
		Assert.assertNotNull(datasource);
		Assert.assertTrue(datasource instanceof DynamicDataSource);

		Mockito.verify(dataSourceFactory, Mockito.times(1)).createDataSource(new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret"));
	}

	@Test
	public void afterPropertiesSet_instanceWithReadReplica_createsDataSourceRouter() throws Exception {
		//Arrange
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
										).withReadReplicaDBInstanceIdentifiers("read1", "read2")
						)
		);

		Mockito.when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("read1"))).thenReturn(
				new DescribeDBInstancesResult().
						withDBInstances(new DBInstance().
										withDBInstanceStatus("available").
										withDBName("read1").
										withDBInstanceIdentifier("read1").
										withEngine("mysql").
										withMasterUsername("admin").
										withEndpoint(new Endpoint().
														withAddress("localhost").
														withPort(3306)
										)
						)
		);

		Mockito.when(amazonRDS.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("read2"))).thenReturn(
				new DescribeDBInstancesResult().
						withDBInstances(new DBInstance().
										withDBInstanceStatus("available").
										withDBName("read2").
										withDBInstanceIdentifier("read2").
										withEngine("mysql").
										withMasterUsername("admin").
										withEndpoint(new Endpoint().
														withAddress("localhost").
														withPort(3306)
										)
						)
		);

		DataSource createdDataSource = Mockito.mock(DataSource.class);
		Connection connection = Mockito.mock(Connection.class);

		Mockito.when(dataSourceFactory.createDataSource(new DataSourceInformation(DatabaseType.MYSQL, "localhost", 3306, "test", "admin", "secret"))).thenReturn(createdDataSource);
		Mockito.when(createdDataSource.getConnection()).thenReturn(connection);

		AmazonRdsReadReplicaAwareDataSourceFactoryBean amazonRdsDataSourceFactoryBean = new AmazonRdsReadReplicaAwareDataSourceFactoryBean(amazonRDS, "test", "secret");
		amazonRdsDataSourceFactoryBean.setDataSourceFactory(dataSourceFactory);
		amazonRdsDataSourceFactoryBean.setTaskExecutor(new SyncTaskExecutor());

		//Act
		amazonRdsDataSourceFactoryBean.afterPropertiesSet();

		//Assert
		DataSource datasource = amazonRdsDataSourceFactoryBean.getObject();
		Assert.assertNotNull(datasource);
		Assert.assertTrue(datasource instanceof LazyConnectionDataSourceProxy);

		ReadOnlyRoutingDataSource source = (ReadOnlyRoutingDataSource) ((LazyConnectionDataSourceProxy) datasource).getTargetDataSource();
		Assert.assertEquals(3, source.getDataSources().size());
	}
}
