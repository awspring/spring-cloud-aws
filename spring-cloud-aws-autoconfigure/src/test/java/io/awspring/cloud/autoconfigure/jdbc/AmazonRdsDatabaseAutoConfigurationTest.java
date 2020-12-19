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

package io.awspring.cloud.autoconfigure.jdbc;

import java.net.URI;

import javax.sql.DataSource;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import io.awspring.cloud.jdbc.config.annotation.RdsInstanceConfigurer;
import io.awspring.cloud.jdbc.datasource.TomcatJdbcDataSourceFactory;
import io.awspring.cloud.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import io.awspring.cloud.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static io.awspring.cloud.core.config.AmazonWebserviceClientConfigurationUtils.GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AmazonRdsDatabaseAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(AmazonRdsDatabaseAutoConfiguration.class));

	@Test
	void registersRdsInstanceConfigurer() {
		this.contextRunner.withUserConfiguration(ApplicationConfigurationWithoutReadReplica.class)
				.withUserConfiguration(CustomRdsInstanceConfigurer.class)
				.withPropertyValues("cloud.aws.rds.instances[0].dbInstanceIdentifier:test",
						"cloud.aws.rds.instances[0].password:secret")
				.run(context -> {
					// Assert
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isNotNull();
					assertThat(context.getBean(AmazonRdsDataSourceFactoryBean.class)).isNotNull();

					assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
					assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource).getValidationQuery())
							.isEqualTo("SELECT 1 FROM TEST");
					assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource).getInitialSize()).isEqualTo(0);
				});
	}

	@Test
	void configureBean_withDefaultClientSpecifiedAndNoReadReplica_configuresFactoryBeanWithoutReadReplica() {
		this.contextRunner.withUserConfiguration(ApplicationConfigurationWithoutReadReplica.class)
				.withPropertyValues("cloud.aws.rds.instances[0].dbInstanceIdentifier:test",
						"cloud.aws.rds.instances[0].password:secret")
				.run(context -> {
					assertThat(context.getBean(DataSource.class)).isNotNull();
					assertThat(context.getBean(AmazonRdsDataSourceFactoryBean.class)).isNotNull();
				});
	}

	@Test
	void configureBean_withCustomDataBaseName_configuresFactoryBeanWithCustomDatabaseName() {
		this.contextRunner.withUserConfiguration(ApplicationConfigurationWithoutReadReplica.class)
				.withPropertyValues("cloud.aws.rds.instances[0].dbInstanceIdentifier:test",
						"cloud.aws.rds.instances[0].password:secret", "cloud.aws.rds.instances[0].databaseName:fooDb")
				.run(context -> {
					DataSource dataSource = context.getBean(DataSource.class);
					assertThat(dataSource).isNotNull();
					assertThat(context.getBean(AmazonRdsDataSourceFactoryBean.class)).isNotNull();

					assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
					assertThat(((org.apache.tomcat.jdbc.pool.DataSource) dataSource).getUrl().endsWith("fooDb"))
							.isTrue();
				});
	}

	@Test
	void configureBean_withDefaultClientSpecifiedAndNoReadReplicaAndMultipleDatabases_configuresBothDatabases() {
		this.contextRunner.withUserConfiguration(ApplicationConfigurationWithMultipleDatabases.class)
				.withPropertyValues("cloud.aws.rds.instances[0].dbInstanceIdentifier:test",
						"cloud.aws.rds.instances[0].password:secret",
						"cloud.aws.rds.instances[1].dbInstanceIdentifier:anotherOne",
						"cloud.aws.rds.instances[1].password:verySecret")
				.run(context -> {
					assertThat(context.getBean("test", DataSource.class)).isNotNull();
					assertThat(context.getBean("&test", AmazonRdsDataSourceFactoryBean.class)).isNotNull();

					assertThat(context.getBean("anotherOne", DataSource.class)).isNotNull();
					assertThat(context.getBean("&anotherOne", AmazonRdsDataSourceFactoryBean.class)).isNotNull();
				});
	}

	@Test
	void configureBean_withDefaultClientSpecifiedAndReadReplica_configuresFactoryBeanWithReadReplicaEnabled() {
		this.contextRunner.withUserConfiguration(ApplicationConfigurationWithReadReplica.class)
				.withPropertyValues("cloud.aws.rds.instances[0].dbInstanceIdentifier:test",
						"cloud.aws.rds.instances[0].password:secret",
						"cloud.aws.rds.instances[0].readReplicaSupport:true")
				.run(context -> {
					assertThat(context.getBean(DataSource.class)).isNotNull();
					assertThat(context.getBean(AmazonRdsReadReplicaAwareDataSourceFactoryBean.class)).isNotNull();
				});
	}

	@Test
	void rdsIsDisabled() {
		this.contextRunner.withPropertyValues("cloud.aws.rds.enabled:false")
				.run(context -> assertThat(context).doesNotHaveBean(DataSource.class));
	}

	@Test
	public void configureBean_withoutDbInstanceIdentifierSpecified_doNotConfigureFactoryBean() {
		this.contextRunner.withPropertyValues("cloud.aws.rds.instances.instances[0].password:secret").run((context) -> {
			assertThat(context).doesNotHaveBean(DataSource.class);
			assertThat(context).doesNotHaveBean(AmazonRdsDataSourceFactoryBean.class);
		});
	}

	@Test
	void configureBean_withoutPasswordSpecified_doNotConfigureFactoryBean() {
		this.contextRunner.withPropertyValues("cloud.aws.rds.instances.instances[0].dbInstanceIdentifier:test")
				.run((context) -> {
					assertThat(context).doesNotHaveBean(DataSource.class);
					assertThat(context).doesNotHaveBean(AmazonRdsDataSourceFactoryBean.class);
				});
	}

	@Test
	void enableRdsWithSpecificRegion() {
		this.contextRunner.withPropertyValues("cloud.aws.rds.region:us-east-1").run(context -> {
			AmazonRDSClient client = context.getBean(AmazonRDSClient.class);

			Object region = ReflectionTestUtils.getField(client, "signingRegion");
			assertThat(region).isEqualTo(Regions.US_EAST_1.getName());
		});
	}

	@Test
	void enableRdsWithCustomEndpoint() {
		this.contextRunner.withPropertyValues("cloud.aws.rds.endpoint:http://localhost:8090").run(context -> {
			AmazonRDSClient client = context.getBean(AmazonRDSClient.class);

			Object endpoint = ReflectionTestUtils.getField(client, "endpoint");
			assertThat(endpoint).isEqualTo(URI.create("http://localhost:8090"));

			Boolean isEndpointOverridden = (Boolean) ReflectionTestUtils.getField(client, "isEndpointOverridden");
			assertThat(isEndpointOverridden).isTrue();
		});
	}

	@Test
	void configuration_withGlobalClientConfiguration_shouldUseItForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithGlobalClientConfiguration.class).run((context) -> {
			AmazonRDSClient client = context.getBean(AmazonRDSClient.class);

			// Assert
			ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
					"clientConfiguration");
			assertThat(clientConfiguration.getProxyHost()).isEqualTo("global");
		});
	}

	@Test
	void configuration_withSqsClientConfiguration_shouldUseItForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithRdsClientConfiguration.class).run((context) -> {
			AmazonRDSClient client = context.getBean(AmazonRDSClient.class);

			// Assert
			ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
					"clientConfiguration");
			assertThat(clientConfiguration.getProxyHost()).isEqualTo("rds");
		});
	}

	@Test
	void configuration_withGlobalAndSqsClientConfigurations_shouldUseSqsConfigurationForClient() {
		// Arrange & Act
		this.contextRunner.withUserConfiguration(ConfigurationWithGlobalAndRdsClientConfiguration.class)
				.run((context) -> {
					AmazonRDSClient client = context.getBean(AmazonRDSClient.class);

					// Assert
					ClientConfiguration clientConfiguration = (ClientConfiguration) ReflectionTestUtils.getField(client,
							"clientConfiguration");
					assertThat(clientConfiguration.getProxyHost()).isEqualTo("rds");
				});
	}

	static class ApplicationConfigurationWithoutReadReplica {

		@Bean
		AmazonRDSClient amazonRDS() {
			AmazonRDSClient client = Mockito.mock(AmazonRDSClient.class);
			when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
					.thenReturn(new DescribeDBInstancesResult()
							.withDBInstances(new DBInstance().withDBInstanceStatus("available").withDBName("test")
									.withDBInstanceIdentifier("test").withEngine("mysql").withMasterUsername("admin")
									.withEndpoint(new Endpoint().withAddress("localhost").withPort(3306))
									.withReadReplicaDBInstanceIdentifiers("read1")));
			return client;
		}

	}

	static class ApplicationConfigurationWithMultipleDatabases {

		@Bean
		AmazonRDS amazonRDS() {
			AmazonRDSClient client = Mockito.mock(AmazonRDSClient.class);
			when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
					.thenReturn(new DescribeDBInstancesResult()
							.withDBInstances(new DBInstance().withDBInstanceStatus("available").withDBName("test")
									.withDBInstanceIdentifier("test").withEngine("mysql").withMasterUsername("admin")
									.withEndpoint(new Endpoint().withAddress("localhost").withPort(3306))));
			when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("anotherOne")))
					.thenReturn(new DescribeDBInstancesResult().withDBInstances(new DBInstance()
							.withDBInstanceStatus("available").withDBName("test").withDBInstanceIdentifier("anotherOne")
							.withEngine("mysql").withMasterUsername("admin")
							.withEndpoint(new Endpoint().withAddress("localhost").withPort(3306))));
			return client;
		}

	}

	static class ApplicationConfigurationWithReadReplica {

		@Bean
		AmazonRDS amazonRDS() {
			AmazonRDSClient client = Mockito.mock(AmazonRDSClient.class);
			when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
					.thenReturn(new DescribeDBInstancesResult()
							.withDBInstances(new DBInstance().withDBInstanceStatus("available").withDBName("test")
									.withDBInstanceIdentifier("test").withEngine("mysql").withMasterUsername("admin")
									.withEndpoint(new Endpoint().withAddress("localhost").withPort(3306))
									.withReadReplicaDBInstanceIdentifiers("read1")));
			when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("read1")))
					.thenReturn(new DescribeDBInstancesResult()
							.withDBInstances(new DBInstance().withDBInstanceStatus("available").withDBName("read1")
									.withDBInstanceIdentifier("read1").withEngine("mysql").withMasterUsername("admin")
									.withEndpoint(new Endpoint().withAddress("localhost").withPort(3306))));
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

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalClientConfiguration {

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithRdsClientConfiguration {

		@Bean
		ClientConfiguration rdsClientConfiguration() {
			return new ClientConfiguration().withProxyHost("rds");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationWithGlobalAndRdsClientConfiguration {

		@Bean
		ClientConfiguration rdsClientConfiguration() {
			return new ClientConfiguration().withProxyHost("rds");
		}

		@Bean(name = GLOBAL_CLIENT_CONFIGURATION_BEAN_NAME)
		ClientConfiguration globalClientConfiguration() {
			return new ClientConfiguration().withProxyHost("global");
		}

	}

}
