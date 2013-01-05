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

package org.elasticspring.jdbc.rds.config.xml;

import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;
import com.amazonaws.services.rds.model.Endpoint;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.elasticspring.context.config.xml.ContextNamespaceHandler;
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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;

/**
 * Tests for the {@link AmazonRdsBeanDefinitionParser} bean definition parser
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class AmazonRdsBeanDefinitionParserTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testParseMinimalConfiguration() throws Exception {

		//Using a bean factory to disable eager creation of singletons
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Register a mock object which will be used to replay service calls
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(Mockito.class);
		beanDefinitionBuilder.setFactoryMethod("mock");
		beanDefinitionBuilder.addConstructorArgValue(AmazonRDS.class);
		beanFactory.registerBeanDefinition(AmazonRdsBeanDefinitionParser.RDS_CLIENT_BEAN_NAME, beanDefinitionBuilder.getBeanDefinition());

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-minimal.xml", getClass()));

		//Get the created mock object from the bean factory, data source has not ben initialized yet
		AmazonRDS client = beanFactory.getBean(AmazonRdsBeanDefinitionParser.RDS_CLIENT_BEAN_NAME, AmazonRDS.class);

		//Replay invocation that will be called during data source creation
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

		//Get bean so it will be initialized
		beanFactory.getBean(DataSource.class);
	}

	@Test
	public void testNoCredentialsDefined() throws Exception {
		this.expectedException.expect(BeanCreationException.class);
		this.expectedException.expectMessage(ContextNamespaceHandler.DEFAULT_CREDENTIALS_PROVIDER_BEAN_NAME);

		//noinspection ResultOfObjectAllocationIgnored
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-noCredentials.xml", getClass());
	}

	@Test
	public void testFullConfiguration() throws Exception {

		//Using a bean factory to disable eager creation of singletons
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		//Load xml file
		XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
		xmlBeanDefinitionReader.loadBeanDefinitions(new ClassPathResource(getClass().getSimpleName() + "-fullConfiguration.xml", getClass()));

		BeanDefinition dataSource = beanFactory.getBeanDefinition("dataSource");

		Assert.assertEquals("test", dataSource.getConstructorArgumentValues().getArgumentValue(1, String.class).getValue());
		Assert.assertEquals("password", dataSource.getConstructorArgumentValues().getArgumentValue(2, String.class).getValue());
		Assert.assertEquals("myUser", dataSource.getPropertyValues().getPropertyValue("username").getValue());

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
		Assert.assertEquals("READ_COMMITTED", dataSourceFactory.getPropertyValues().getPropertyValue("defaultTransactionIsolation").getValue());
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
}