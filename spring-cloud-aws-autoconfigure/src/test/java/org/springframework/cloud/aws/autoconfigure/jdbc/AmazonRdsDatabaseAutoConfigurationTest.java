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

package org.springframework.cloud.aws.autoconfigure.jdbc;

import javax.sql.DataSource;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class AmazonRdsDatabaseAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void configureBean_withDefaultClientSpecifiedAndNoReadReplica_configuresFactoryBeanWithoutReadReplica()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithoutReadReplica.class);
		this.context.register(AmazonRdsDatabaseAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.rds.test.password:secret").applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();
	}

	@Test
	public void configureBean_withCustomDataBaseName_configuresFactoryBeanWithCustomDatabaseName()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithoutReadReplica.class);
		this.context.register(AmazonRdsDatabaseAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.rds.test.password:secret",
				"cloud.aws.rds.test.databaseName:fooDb").applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(dataSource).isNotNull();
		assertThat(this.context.getBean(AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();

		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
		assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource).getUrl()
				.endsWith("fooDb")).isTrue();
	}

	@Test
	public void configureBean_withDefaultClientSpecifiedAndNoReadReplicaAndMultipleDatabases_configuresBothDatabases()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithMultipleDatabases.class);
		this.context.register(AmazonRdsDatabaseAutoConfiguration.class);
		TestPropertyValues
				.of("cloud.aws.rds.test.password:secret",
						"cloud.aws.rds.anotherOne.password:verySecret")
				.applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean("test", DataSource.class)).isNotNull();
		assertThat(this.context.getBean("&test", AmazonRdsDataSourceFactoryBean.class))
				.isNotNull();

		assertThat(this.context.getBean("anotherOne", DataSource.class)).isNotNull();
		assertThat(
				this.context.getBean("&anotherOne", AmazonRdsDataSourceFactoryBean.class))
						.isNotNull();
	}

	@Test
	public void configureBean_withDefaultClientSpecifiedAndReadReplica_configuresFactoryBeanWithReadReplicaEnabled()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithReadReplica.class);
		this.context.register(AmazonRdsDatabaseAutoConfiguration.class);
		TestPropertyValues
				.of("cloud.aws.rds.test.password:secret",
						"cloud.aws.rds.test.readReplicaSupport:true")
				.applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context
				.getBean(AmazonRdsReadReplicaAwareDataSourceFactoryBean.class))
						.isNotNull();
	}

	public static class ApplicationConfigurationWithoutReadReplica {

		@Bean
		public AmazonRDSClient amazonRDS() {
			AmazonRDSClient client = Mockito.mock(AmazonRDSClient.class);
			when(client.describeDBInstances(
					new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
							.thenReturn(new DescribeDBInstancesResult().withDBInstances(
									new DBInstance().withDBInstanceStatus("available")
											.withDBName("test")
											.withDBInstanceIdentifier("test")
											.withEngine("mysql")
											.withMasterUsername("admin")
											.withEndpoint(new Endpoint()
													.withAddress("localhost")
													.withPort(3306))
											.withReadReplicaDBInstanceIdentifiers(
													"read1")));
			return client;
		}

	}

	public static class ApplicationConfigurationWithMultipleDatabases {

		@Bean
		public AmazonRDS amazonRDS() {
			AmazonRDSClient client = Mockito.mock(AmazonRDSClient.class);
			when(client.describeDBInstances(
					new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
							.thenReturn(new DescribeDBInstancesResult().withDBInstances(
									new DBInstance().withDBInstanceStatus("available")
											.withDBName("test")
											.withDBInstanceIdentifier("test")
											.withEngine("mysql")
											.withMasterUsername("admin")
											.withEndpoint(new Endpoint()
													.withAddress("localhost")
													.withPort(3306))));
			when(client.describeDBInstances(new DescribeDBInstancesRequest()
					.withDBInstanceIdentifier("anotherOne")))
							.thenReturn(new DescribeDBInstancesResult().withDBInstances(
									new DBInstance().withDBInstanceStatus("available")
											.withDBName("test")
											.withDBInstanceIdentifier("anotherOne")
											.withEngine("mysql")
											.withMasterUsername("admin")
											.withEndpoint(new Endpoint()
													.withAddress("localhost")
													.withPort(3306))));
			return client;
		}

	}

	public static class ApplicationConfigurationWithReadReplica {

		@Bean
		public AmazonRDS amazonRDS() {
			AmazonRDSClient client = Mockito.mock(AmazonRDSClient.class);
			when(client.describeDBInstances(
					new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
							.thenReturn(new DescribeDBInstancesResult().withDBInstances(
									new DBInstance().withDBInstanceStatus("available")
											.withDBName("test")
											.withDBInstanceIdentifier("test")
											.withEngine("mysql")
											.withMasterUsername("admin")
											.withEndpoint(new Endpoint()
													.withAddress("localhost")
													.withPort(3306))
											.withReadReplicaDBInstanceIdentifiers(
													"read1")));
			when(client.describeDBInstances(
					new DescribeDBInstancesRequest().withDBInstanceIdentifier("read1")))
							.thenReturn(new DescribeDBInstancesResult().withDBInstances(
									new DBInstance().withDBInstanceStatus("available")
											.withDBName("read1")
											.withDBInstanceIdentifier("read1")
											.withEngine("mysql")
											.withMasterUsername("admin")
											.withEndpoint(new Endpoint()
													.withAddress("localhost")
													.withPort(3306))));
			return client;
		}

	}

}
