/*
 * Copyright 2013-2020 the original author or authors.
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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.aws.jdbc.config.annotation.RdsInstanceConfigurer;
import org.springframework.cloud.aws.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AmazonRdsDatabaseAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(AmazonRdsDatabaseAutoConfiguration.class));

	@Test
	public void registersRdsInstanceConfigurer() {
		// Arrange
		this.contextRunner
				.withUserConfiguration(ApplicationConfigurationWithoutReadReplica.class)
				.withUserConfiguration(CustomRdsInstanceConfigurer.class)
				.withPropertyValues("cloud.aws.rds.test.password:secret").run(context -> {
					// Assert
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isNotNull();
					assertThat(context.getBean(AmazonRdsDataSourceFactoryBean.class))
							.isNotNull();

					assertThat(
							dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource)
									.isTrue();
					assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource)
							.getValidationQuery()).isEqualTo("SELECT 1 FROM TEST");
					assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource)
							.getInitialSize()).isEqualTo(0);
				});
	}

	@Test
	void configureBean_withDefaultClientSpecifiedAndNoReadReplica_configuresFactoryBeanWithoutReadReplica() {
		this.contextRunner
				.withUserConfiguration(ApplicationConfigurationWithoutReadReplica.class)
				.withPropertyValues("cloud.aws.rds.test.password:secret").run(context -> {
					assertThat(context.getBean(DataSource.class)).isNotNull();
					assertThat(context.getBean(AmazonRdsDataSourceFactoryBean.class))
							.isNotNull();
				});
	}

	@Test
	void configureBean_withCustomDataBaseName_configuresFactoryBeanWithCustomDatabaseName() {
		this.contextRunner
				.withUserConfiguration(ApplicationConfigurationWithoutReadReplica.class)
				.withPropertyValues("cloud.aws.rds.test.password:secret",
						"cloud.aws.rds.test.databaseName:fooDb")
				.run(context -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isNotNull();
					assertThat(context.getBean(AmazonRdsDataSourceFactoryBean.class))
							.isNotNull();

					assertThat(
							dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource)
									.isTrue();
					assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource)
							.getUrl().endsWith("fooDb")).isTrue();
				});
	}

	@Test
	void configureBean_withDefaultClientSpecifiedAndNoReadReplicaAndMultipleDatabases_configuresBothDatabases() {
		this.contextRunner
				.withUserConfiguration(
						ApplicationConfigurationWithMultipleDatabases.class)
				.withPropertyValues("cloud.aws.rds.test.password:secret",
						"cloud.aws.rds.anotherOne.password:verySecret")
				.run(context -> {
					assertThat(context.getBean("test", DataSource.class)).isNotNull();
					assertThat(context.getBean("&test",
							AmazonRdsDataSourceFactoryBean.class)).isNotNull();

					assertThat(context.getBean("anotherOne", DataSource.class))
							.isNotNull();
					assertThat(context.getBean("&anotherOne",
							AmazonRdsDataSourceFactoryBean.class)).isNotNull();
				});
	}

	@Test
	void configureBean_withDefaultClientSpecifiedAndReadReplica_configuresFactoryBeanWithReadReplicaEnabled() {
		this.contextRunner
				.withUserConfiguration(ApplicationConfigurationWithReadReplica.class)
				.withPropertyValues("cloud.aws.rds.test.password:secret",
						"cloud.aws.rds.test.readReplicaSupport:true")
				.run(context -> {
					assertThat(context.getBean(DataSource.class)).isNotNull();
					assertThat(context.getBean(
							AmazonRdsReadReplicaAwareDataSourceFactoryBean.class))
									.isNotNull();
				});
	}

	@Test
	void rdsIsDisabled() {
		this.contextRunner.withPropertyValues("cloud.aws.rds.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(DataSource.class));
	}

	static class ApplicationConfigurationWithoutReadReplica {

		@Bean
		AmazonRDSClient amazonRDS() {
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

	static class ApplicationConfigurationWithMultipleDatabases {

		@Bean
		AmazonRDS amazonRDS() {
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

	static class ApplicationConfigurationWithReadReplica {

		@Bean
		AmazonRDS amazonRDS() {
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

	public static class CustomRdsInstanceConfigurer {

		@Bean
		public RdsInstanceConfigurer instanceConfigurer() {
			return () -> {
				TomcatJdbcDataSourceFactory dataSourceFactory = new TomcatJdbcDataSourceFactory();
				dataSourceFactory.setInitialSize(0);
				dataSourceFactory.setValidationQuery("SELECT 1 FROM TEST");
				return dataSourceFactory;
			};
		}

	}

}
