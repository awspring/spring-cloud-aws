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

package org.elasticspring.jdbc.config.xml;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.elasticspring.context.config.xml.GlobalBeanDefinitionUtils;
import org.elasticspring.context.config.xml.support.AmazonWebserviceClientConfigurationUtils;
import org.elasticspring.context.credentials.CredentialsProviderFactoryBean;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

/**
 * Tests for the {@link org.elasticspring.jdbc.config.xml.AmazonRdsBeanDefinitionParser} bean definition parser
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonRdsBeanDefinitionParserTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testParseMinimalConfiguration() throws Exception {

		//Using a bean factory to disable eager creation of singletons
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Register a mock object which will be used to replay service calls
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonRDS.class);
		beanFactory.registerBeanDefinition(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonRDSClient.class.getName()), beanDefinitionBuilder.getBeanDefinition());

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml", getClass()));

		//Get the created mock object from the bean factory, datasource has not ben initialized yet
		AmazonRDS client = beanFactory.getBean(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonRDSClient.class.getName()), AmazonRDS.class);

		//Replay invocation that will be called during datasource creation
		Mockito.when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test"))).thenReturn(
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

		//Get bean so it will be initialized
		beanFactory.getBean(DataSource.class);
	}

	@Test
	public void testNoCredentialsDefined() throws Exception {
		this.expectedException.expect(BeanCreationException.class);
		this.expectedException.expectMessage(CredentialsProviderFactoryBean.CREDENTIALS_PROVIDER_BEAN_NAME);

		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-noCredentials.xml", getClass());
	}

	@Test
	public void testFullConfiguration() throws Exception {

		//Using a bean factory to disable eager creation of singletons
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Register a mock object which will be used to replay service calls
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonRDS.class);
		beanFactory.registerBeanDefinition(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonRDSClient.class.getName()), beanDefinitionBuilder.getBeanDefinition());

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-fullConfiguration.xml", getClass()));

		//Get the created mock object from the bean factory, datasource has not ben initialized yet
		AmazonRDS client = beanFactory.getBean(AmazonWebserviceClientConfigurationUtils.getBeanName(AmazonRDSClient.class.getName()), AmazonRDS.class);

		//Replay invocation that will be called during datasource creation
		Mockito.when(client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier("test"))).thenReturn(
				new DescribeDBInstancesResult().
						withDBInstances(new DBInstance().
								withDBInstanceStatus("available").
								withDBName("test").
								withDBInstanceIdentifier("test").
								withEngine("mysql").
								withEndpoint(new Endpoint().
										withAddress("localhost").
										withPort(3306)
								)
						)
		);


		BeanDefinition definition = beanFactory.getBeanDefinition("dataSource");
		Assert.assertEquals(GlobalBeanDefinitionUtils.RESOURCE_ID_RESOLVER_BEAN_NAME,((RuntimeBeanReference) definition.getConstructorArgumentValues().getArgumentValue(1,String.class).getValue()).getBeanName());
		Assert.assertEquals("test", definition.getConstructorArgumentValues().getArgumentValue(2, String.class).getValue());
		Assert.assertEquals("password", definition.getConstructorArgumentValues().getArgumentValue(3, String.class).getValue());
		Assert.assertEquals("myUser", definition.getPropertyValues().getPropertyValue("username").getValue());

		DataSource dataSource = beanFactory.getBean(DataSource.class);
		Assert.assertNotNull(dataSource);
	}

	@Test
	public void testParsePoolAttributes() throws Exception {

		//Using a bean factory to disable eager creation of singletons
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-poolAttributes.xml", getClass()));

		BeanDefinition definition = beanFactory.getBeanDefinition("dataSource");
		BeanDefinition dataSourceFactory = (BeanDefinition) definition.getPropertyValues().getPropertyValue("dataSourceFactory").getValue();

		Assert.assertEquals("foo=bar", dataSourceFactory.getPropertyValues().getPropertyValue("connectionProperties").getValue());
		Assert.assertEquals(Boolean.TRUE.toString(), dataSourceFactory.getPropertyValues().getPropertyValue("defaultAutoCommit").getValue());
		Assert.assertEquals("mySchema", dataSourceFactory.getPropertyValues().getPropertyValue("defaultCatalog").getValue());
		Assert.assertEquals(Boolean.TRUE.toString(), dataSourceFactory.getPropertyValues().getPropertyValue("defaultReadOnly").getValue());
		Assert.assertEquals("2", dataSourceFactory.getPropertyValues().getPropertyValue("defaultTransactionIsolation").getValue());
		Assert.assertEquals("10", dataSourceFactory.getPropertyValues().getPropertyValue("initialSize").getValue());
		Assert.assertEquals("SET CURRENT SCHEMA", dataSourceFactory.getPropertyValues().getPropertyValue("initSQL").getValue());
		Assert.assertEquals(Boolean.TRUE.toString(), dataSourceFactory.getPropertyValues().getPropertyValue("logAbandoned").getValue());
		Assert.assertEquals("10", dataSourceFactory.getPropertyValues().getPropertyValue("maxActive").getValue());
		Assert.assertEquals("5", dataSourceFactory.getPropertyValues().getPropertyValue("maxIdle").getValue());
		Assert.assertEquals("10000", dataSourceFactory.getPropertyValues().getPropertyValue("maxWait").getValue());
		Assert.assertEquals("60000", dataSourceFactory.getPropertyValues().getPropertyValue("minEvictableIdleTimeMillis").getValue());
		Assert.assertEquals("20", dataSourceFactory.getPropertyValues().getPropertyValue("minIdle").getValue());
		Assert.assertEquals("61", dataSourceFactory.getPropertyValues().getPropertyValue("removeAbandonedTimeout").getValue());
		Assert.assertEquals(Boolean.TRUE.toString(), dataSourceFactory.getPropertyValues().getPropertyValue("testOnBorrow").getValue());
		Assert.assertEquals(Boolean.TRUE.toString(), dataSourceFactory.getPropertyValues().getPropertyValue("testOnReturn").getValue());
		Assert.assertEquals(Boolean.TRUE.toString(), dataSourceFactory.getPropertyValues().getPropertyValue("testWhileIdle").getValue());
		Assert.assertEquals("4000", dataSourceFactory.getPropertyValues().getPropertyValue("timeBetweenEvictionRunsMillis").getValue());
		Assert.assertEquals("SELECT 1", dataSourceFactory.getPropertyValues().getPropertyValue("validationQuery").getValue());
		Assert.assertEquals("myValidator", dataSourceFactory.getPropertyValues().getPropertyValue("validatorClassName").getValue());
	}

	@Test
	//As we provide default in the schema for better code completion we should check if they match to the underlying pool defaults
	public void testDefaultPoolAttributes() throws Exception {
		PoolProperties poolProperties = new PoolProperties();

		//Using a bean factory to disable eager creation of singletons
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-defaultPoolAttributes.xml", getClass()));

		BeanDefinition definition = beanFactory.getBeanDefinition("dataSource");
		BeanDefinition dataSourceFactory = (BeanDefinition) definition.getPropertyValues().getPropertyValue("dataSourceFactory").getValue();

		BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(poolProperties);

		for (PropertyValue propertyValue : dataSourceFactory.getPropertyValues().getPropertyValueList()) {
			Assert.assertEquals(beanWrapper.getPropertyValue(propertyValue.getName()).toString(), propertyValue.getValue());
		}
	}

	@Test
	public void testCustomRegion() throws Exception {

		//Using a bean factory to disable eager creation of singletons
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-customRegion.xml", getClass()));

		AmazonRDS amazonRDS = beanFactory.getBean(AmazonRDS.class);

		//have to use reflection utils
		Assert.assertEquals("https://rds.eu-west-1.amazonaws.com", ReflectionTestUtils.getField(amazonRDS, "endpoint").toString());

	}

	@Test
	public void testCustomRegionProvider() throws Exception {

		//Using a bean factory to disable eager creation of singletons
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-customRegionProvider.xml", getClass()));

		AmazonRDS amazonRDS = beanFactory.getBean(AmazonRDS.class);

		//have to use reflection utils
		Assert.assertEquals("https://rds.eu-west-1.amazonaws.com", ReflectionTestUtils.getField(amazonRDS, "endpoint").toString());

	}

	@Test
	public void testCustomRegionProviderAndRegion() throws Exception {

		this.expectedException.expect(BeanDefinitionParsingException.class);
		this.expectedException.expectMessage("not be used together");

		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-customRegionProviderAndRegion.xml", getClass());

	}
}