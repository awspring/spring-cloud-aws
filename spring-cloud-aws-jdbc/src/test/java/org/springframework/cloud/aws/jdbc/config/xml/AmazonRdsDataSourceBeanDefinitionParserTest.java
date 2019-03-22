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

package org.springframework.cloud.aws.jdbc.config.xml;

import java.util.Date;
import java.util.Map;

import javax.sql.DataSource;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.GetUserResult;
import com.amazonaws.services.identitymanagement.model.User;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import com.amazonaws.services.rds.model.ListTagsForResourceRequest;
import com.amazonaws.services.rds.model.ListTagsForResourceResult;
import com.amazonaws.services.rds.model.Tag;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link AmazonRdsDataSourceBeanDefinitionParser} bean definition parser.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonRdsDataSourceBeanDefinitionParserTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void parseInternal_minimalConfiguration_createsBeanDefinitionWithoutReadReplicas()
			throws Exception {

		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonRDS.class);
		beanFactory.registerBeanDefinition(
				AmazonWebserviceClientConfigurationUtils
						.getBeanName(AmazonRDSClient.class.getName()),
				beanDefinitionBuilder.getBeanDefinition());

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-minimal.xml", getClass()));

		AmazonRDS client = beanFactory.getBean(AmazonWebserviceClientConfigurationUtils
				.getBeanName(AmazonRDSClient.class.getName()), AmazonRDS.class);

		when(client.describeDBInstances(
				new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
						.thenReturn(new DescribeDBInstancesResult().withDBInstances(
								new DBInstance().withDBInstanceStatus("available")
										.withDBName("test")
										.withDBInstanceIdentifier("test")
										.withEngine("mysql").withMasterUsername("admin")
										.withEndpoint(new Endpoint()
												.withAddress("localhost").withPort(3306))
										.withReadReplicaDBInstanceIdentifiers("read1")));

		// Act
		DataSource dataSource = beanFactory.getBean(DataSource.class);

		// Assert
		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
	}

	@Test
	public void parseInternal_readReplicaSupportEnabled_configuresReadReplicaEnabledFactoryBean()
			throws Exception {

		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonRDS.class);
		beanFactory.registerBeanDefinition(
				AmazonWebserviceClientConfigurationUtils
						.getBeanName(AmazonRDSClient.class.getName()),
				beanDefinitionBuilder.getBeanDefinition());

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-readReplicaEnabled.xml", getClass()));

		// Act
		BeanDefinition beanDefinition = beanFactory.getBeanDefinition("test");

		// Assert
		assertThat(beanDefinition.getBeanClassName()).isEqualTo(
				AmazonRdsReadReplicaAwareDataSourceFactoryBean.class.getName());
	}

	@Test
	public void parseInternal_noCredentialsDefined_returnsClientWithDefaultCredentialsProvider()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonRDS.class);
		beanFactory.registerBeanDefinition(
				AmazonWebserviceClientConfigurationUtils
						.getBeanName(AmazonRDSClient.class.getName()),
				beanDefinitionBuilder.getBeanDefinition());

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-noCredentials.xml", getClass()));

		AmazonRDS client = beanFactory.getBean(AmazonWebserviceClientConfigurationUtils
				.getBeanName(AmazonRDSClient.class.getName()), AmazonRDS.class);

		when(client.describeDBInstances(
				new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
						.thenReturn(new DescribeDBInstancesResult().withDBInstances(
								new DBInstance().withDBInstanceStatus("available")
										.withDBName("test")
										.withDBInstanceIdentifier("test")
										.withEngine("mysql").withMasterUsername("admin")
										.withEndpoint(new Endpoint()
												.withAddress("localhost").withPort(3306))
										.withReadReplicaDBInstanceIdentifiers("read1")));

		// Act
		DataSource dataSource = beanFactory.getBean(DataSource.class);

		// Assert
		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
	}

	@Test
	public void parseInternal_fullConfiguration_createsBeanDefinitionWithoutReadReplicas()
			throws Exception {

		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonRDS.class);
		beanFactory.registerBeanDefinition(
				AmazonWebserviceClientConfigurationUtils
						.getBeanName(AmazonRDSClient.class.getName()),
				beanDefinitionBuilder.getBeanDefinition());

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-fullConfiguration.xml", getClass()));

		AmazonRDS client = beanFactory.getBean(AmazonWebserviceClientConfigurationUtils
				.getBeanName(AmazonRDSClient.class.getName()), AmazonRDS.class);

		when(client.describeDBInstances(
				new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
						.thenReturn(
								new DescribeDBInstancesResult().withDBInstances(
										new DBInstance().withDBInstanceStatus("available")
												.withDBName("test")
												.withDBInstanceIdentifier("test")
												.withEngine("mysql")
												.withEndpoint(new Endpoint()
														.withAddress("localhost")
														.withPort(3306))));

		BeanDefinition definition = beanFactory.getBeanDefinition("test");
		assertThat(definition.getConstructorArgumentValues()
				.getArgumentValue(1, String.class).getValue()).isEqualTo("test");
		assertThat(definition.getConstructorArgumentValues()
				.getArgumentValue(2, String.class).getValue()).isEqualTo("password");
		assertThat(definition.getPropertyValues().getPropertyValue("username").getValue())
				.isEqualTo("myUser");
		assertThat(definition.getPropertyValues().getPropertyValue("databaseName")
				.getValue()).isEqualTo("fooDb");

		DataSource dataSource = beanFactory.getBean(DataSource.class);

		// Assert
		assertThat(dataSource).isNotNull();
		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
	}

	@Test
	public void parseInternal_dataSourceWithConfiguredPoolAttributes_poolAttributesConfigured()
			throws Exception {

		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-poolAttributes.xml", getClass()));

		BeanDefinition definition = beanFactory.getBeanDefinition("test");

		// Act
		BeanDefinition dataSourceFactory = (BeanDefinition) definition.getPropertyValues()
				.getPropertyValue("dataSourceFactory").getValue();

		// Assert
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("connectionProperties").getValue())
						.isEqualTo("foo=bar");
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("defaultAutoCommit").getValue())
						.isEqualTo(Boolean.TRUE.toString());
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("defaultCatalog").getValue()).isEqualTo("mySchema");
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("defaultReadOnly").getValue())
						.isEqualTo(Boolean.TRUE.toString());
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("defaultTransactionIsolation").getValue())
						.isEqualTo("2");
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("initialSize")
				.getValue()).isEqualTo("10");
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("initSQL")
				.getValue()).isEqualTo("SET CURRENT SCHEMA");
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("logAbandoned")
				.getValue()).isEqualTo(Boolean.TRUE.toString());
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("maxActive")
				.getValue()).isEqualTo("10");
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("maxIdle")
				.getValue()).isEqualTo("5");
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("maxWait")
				.getValue()).isEqualTo("10000");
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("minEvictableIdleTimeMillis").getValue())
						.isEqualTo("60000");
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("minIdle")
				.getValue()).isEqualTo("20");
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("removeAbandonedTimeout").getValue()).isEqualTo("61");
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("testOnBorrow")
				.getValue()).isEqualTo(Boolean.TRUE.toString());
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("testOnReturn")
				.getValue()).isEqualTo(Boolean.TRUE.toString());
		assertThat(dataSourceFactory.getPropertyValues().getPropertyValue("testWhileIdle")
				.getValue()).isEqualTo(Boolean.TRUE.toString());
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("timeBetweenEvictionRunsMillis").getValue())
						.isEqualTo("4000");
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("validationQuery").getValue()).isEqualTo("SELECT 1");
		assertThat(dataSourceFactory.getPropertyValues()
				.getPropertyValue("validatorClassName").getValue())
						.isEqualTo(SampleValidator.class.getName());
	}

	@Test
	// As we provide default in the schema for better code completion we should check if
	// they match to the underlying pool defaults
	public void parseInternal_defaultPoolAttribute_matchesPoolConfiguration()
			throws Exception {
		// Arrange
		PoolProperties poolProperties = new PoolProperties();

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-defaultPoolAttributes.xml", getClass()));

		// Act
		BeanDefinition definition = beanFactory.getBeanDefinition("test");
		BeanDefinition dataSourceFactory = (BeanDefinition) definition.getPropertyValues()
				.getPropertyValue("dataSourceFactory").getValue();

		// Assert
		BeanWrapper beanWrapper = PropertyAccessorFactory
				.forBeanPropertyAccess(poolProperties);

		for (PropertyValue propertyValue : dataSourceFactory.getPropertyValues()
				.getPropertyValueList()) {
			assertThat(propertyValue.getValue()).isEqualTo(
					beanWrapper.getPropertyValue(propertyValue.getName()).toString());
		}
	}

	@Test
	public void parseInternal_customRegionConfigured_amazonRdsClientWithCustomRegionConfigured()
			throws Exception {

		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-customRegion.xml", getClass()));

		// Act
		AmazonRDS amazonRDS = beanFactory.getBean(AmazonRDS.class);

		// Assert
		// have to use reflection utils
		assertThat(ReflectionTestUtils.getField(amazonRDS, "endpoint").toString())
				.isEqualTo("https://rds.eu-west-1.amazonaws.com");
	}

	@Test
	public void parseInternal_custRegionProviderConf_amazRdsClientWithCustomRegionConfThatIsReturnedFromRegionProvider()
			throws Exception {

		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-customRegionProvider.xml", getClass()));

		// Act
		AmazonRDS amazonRDS = beanFactory.getBean(AmazonRDS.class);

		// Assert
		// have to use reflection utils
		assertThat(ReflectionTestUtils.getField(amazonRDS, "endpoint").toString())
				.isEqualTo("https://rds.eu-west-1.amazonaws.com");

	}

	@Test
	public void parseInternal_customRegionProviderAndRegionConfigured_reportsError()
			throws Exception {
		// Arrange
		this.expectedException.expect(BeanDefinitionStoreException.class);

		// Act
		// noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(
				getClass().getSimpleName() + "-customRegionProviderAndRegion.xml",
				getClass());

		// Assert
	}

	@Test
	public void parseInternal_userTagsDefined_createsUserTagBeanDefinition()
			throws Exception {

		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonRDS.class);
		beanFactory.registerBeanDefinition(
				AmazonWebserviceClientConfigurationUtils
						.getBeanName(AmazonRDSClient.class.getName()),
				beanDefinitionBuilder.getBeanDefinition());

		BeanDefinitionBuilder identityBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(Mockito.class);
		identityBuilder.setFactoryMethod("mock");
		identityBuilder.addConstructorArgValue(AmazonIdentityManagement.class);
		beanFactory.registerBeanDefinition(
				AmazonWebserviceClientConfigurationUtils
						.getBeanName(AmazonIdentityManagement.class.getName()),
				identityBuilder.getBeanDefinition());

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-userTags.xml", getClass()));

		AmazonRDS client = beanFactory.getBean(AmazonWebserviceClientConfigurationUtils
				.getBeanName(AmazonRDSClient.class.getName()), AmazonRDS.class);
		AmazonIdentityManagement amazonIdentityManagement = beanFactory.getBean(
				AmazonWebserviceClientConfigurationUtils
						.getBeanName(AmazonIdentityManagement.class.getName()),
				AmazonIdentityManagement.class);

		when(amazonIdentityManagement.getUser()).thenReturn(
				new GetUserResult().withUser(new User("/", "aemruli", "123456789012",
						"arn:aws:iam::1234567890:user/aemruli", new Date())));
		when(client.listTagsForResource(new ListTagsForResourceRequest()
				.withResourceName("arn:aws:rds:us-west-2:1234567890:db:test")))
						.thenReturn(new ListTagsForResourceResult().withTagList(
								new Tag().withKey("key1").withValue("value2")));

		// Act
		Map<?, ?> dsTags = beanFactory.getBean("dsTags", Map.class);

		// Assert
		assertThat(dsTags.get("key1")).isEqualTo("value2");
	}

	@Test
	public void parseInternal_customRdsInstance_createsRdsBeanAndUserTagsWithCustomRdsInstance()
			throws Exception {

		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(
				beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-customRdsInstance.xml", getClass()));

		AmazonRDS clientMock = beanFactory.getBean("amazonRds", AmazonRDS.class);

		when(clientMock.describeDBInstances(
				new DescribeDBInstancesRequest().withDBInstanceIdentifier("test")))
						.thenReturn(new DescribeDBInstancesResult().withDBInstances(
								new DBInstance().withDBInstanceStatus("available")
										.withDBName("test")
										.withDBInstanceIdentifier("test")
										.withEngine("mysql").withMasterUsername("admin")
										.withEndpoint(new Endpoint()
												.withAddress("localhost").withPort(3306))
										.withReadReplicaDBInstanceIdentifiers("read1")));

		AmazonIdentityManagement amazonIdentityManagement = beanFactory
				.getBean("myIdentityService", AmazonIdentityManagement.class);

		when(amazonIdentityManagement.getUser()).thenReturn(
				new GetUserResult().withUser(new User("/", "aemruli", "123456789012",
						"arn:aws:iam::1234567890:user/aemruli", new Date())));
		when(clientMock.listTagsForResource(new ListTagsForResourceRequest()
				.withResourceName("arn:aws:rds:us-west-2:1234567890:db:test")))
						.thenReturn(new ListTagsForResourceResult().withTagList(
								new Tag().withKey("key1").withValue("value2")));

		// Act
		Map<?, ?> dsTags = beanFactory.getBean("dsTags", Map.class);
		DataSource dataSource = beanFactory.getBean(DataSource.class);

		// Assert
		assertThat(dsTags.get("key1")).isEqualTo("value2");
		assertThat(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource).isTrue();
	}

}
