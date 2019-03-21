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

package org.springframework.cloud.aws.context.config.xml;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cloud.aws.context.MetaDataServer;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils.getBeanName;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 */
public class StackConfigurationBeanDefinitionParserTest {

	// @checkstyle:off
	@Test
	public void parseInternal_stackConfigurationWithExternallyConfiguredCloudFormationClient_returnsConfiguredStackWithExternallyConfiguredClient()
			throws Exception {
		// @checkstyle:on
		// Arrange
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);

		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-withCustomCloudFormationClient.xml",
				getClass()));

		AmazonCloudFormation amazonCloudFormationMock = beanFactory
				.getBean(AmazonCloudFormation.class);
		when(amazonCloudFormationMock.listStackResources(
				new ListStackResourcesRequest().withStackName("test")))
						.thenReturn(new ListStackResourcesResult()
								.withStackResourceSummaries(new StackResourceSummary()));
		when(amazonCloudFormationMock
				.describeStacks(new DescribeStacksRequest().withStackName("test")))
						.thenReturn(new DescribeStacksResult().withStacks(new Stack()));

		// Act
		StackResourceRegistry stackResourceRegistry = beanFactory
				.getBean(StackResourceRegistry.class);

		// Assert
		assertThat(stackResourceRegistry).isNotNull();
		assertThat(beanFactory.containsBeanDefinition(
				getBeanName(AmazonCloudFormationClient.class.getName()))).isFalse();
		verify(amazonCloudFormationMock, times(1)).listStackResources(
				new ListStackResourcesRequest().withStackName("test"));
		beanFactory.getBean("customStackTags");
		verify(amazonCloudFormationMock, times(1))
				.describeStacks(new DescribeStacksRequest().withStackName("test"));
	}

	@Test
	public void parseInternal_withCustomRegion_shouldConfigureDefaultClientWithCustomRegion()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region.xml", getClass()));

		// Assert
		AmazonCloudFormationClient amazonCloudFormation = registry
				.getBean(AmazonCloudFormationClient.class);
		assertThat(
				ReflectionTestUtils.getField(amazonCloudFormation, "endpoint").toString())
						.isEqualTo("https://" + Region.getRegion(Regions.SA_EAST_1)
								.getServiceEndpoint("cloudformation"));
	}

	@Test
	public void parseInternal_withCustomRegionProvider_shouldConfigureDefaultClientWithCustomRegionReturnedByProvider()
			throws Exception {
		// Arrange
		DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

		// Act
		reader.loadBeanDefinitions(new ClassPathResource(
				getClass().getSimpleName() + "-custom-region-provider.xml", getClass()));

		// Assert
		AmazonCloudFormationClient amazonCloudFormation = registry
				.getBean(AmazonCloudFormationClient.class);
		assertThat(
				ReflectionTestUtils.getField(amazonCloudFormation, "endpoint").toString())
						.isEqualTo("https://" + Region.getRegion(Regions.AP_SOUTHEAST_2)
								.getServiceEndpoint("cloudformation"));
	}

	@Test
	public void resourceIdResolver_stackConfiguration_resourceIdResolverBeanExposed() {
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito
				.mock(AmazonCloudFormation.class);

		when(amazonCloudFormation.listStackResources(
				new ListStackResourcesRequest().withStackName("IntegrationTestStack")))
						.thenReturn(new ListStackResourcesResult()
								.withStackResourceSummaries(new StackResourceSummary()));

		applicationContext.load(new ClassPathResource(
				getClass().getSimpleName() + "-staticStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(
				getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);

		applicationContext.refresh();

		// Act
		ResourceIdResolver resourceIdResolver = applicationContext
				.getBean(ResourceIdResolver.class);

		// Assert
		assertThat(resourceIdResolver).isNotNull();
	}

	// @checkstyle:off
	@Test
	public void stackResourceRegistry_stackConfigurationWithStaticName_stackResourceRegistryBeanExposedUnderStaticStackName()
			throws Exception {
		// @checkstyle:on
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito
				.mock(AmazonCloudFormation.class);

		when(amazonCloudFormation.listStackResources(
				new ListStackResourcesRequest().withStackName("IntegrationTestStack")))
						.thenReturn(new ListStackResourcesResult()
								.withStackResourceSummaries(new StackResourceSummary()));

		applicationContext.load(new ClassPathResource(
				getClass().getSimpleName() + "-staticStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(
				getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);

		applicationContext.refresh();

		// Act
		StackResourceRegistry staticStackNameProviderBasedStackResourceRegistry = applicationContext
				.getBean("IntegrationTestStack", StackResourceRegistry.class);

		// Assert
		assertThat(staticStackNameProviderBasedStackResourceRegistry).isNotNull();
	}

	// @checkstyle:off
	@Test
	public void stackResourceRegistry_stackConfigurationWithoutStaticName_stackResourceRegistryBeanExposedUnderGeneratedName()
			throws Exception {
		// @checkstyle:on
		// Arrange
		HttpServer server = MetaDataServer.setupHttpServer();
		HttpContext httpContext = server.createContext("/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("foo"));

		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito
				.mock(AmazonCloudFormation.class);

		when(amazonCloudFormation.describeStackResources(
				new DescribeStackResourcesRequest().withPhysicalResourceId("foo")))
						.thenReturn(new DescribeStackResourcesResult().withStackResources(
								new StackResource().withStackName("test")));

		when(amazonCloudFormation.listStackResources(
				new ListStackResourcesRequest().withStackName("test")))
						.thenReturn(new ListStackResourcesResult()
								.withStackResourceSummaries(new StackResourceSummary()));

		applicationContext.load(new ClassPathResource(
				getClass().getSimpleName() + "-autoDetectStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(
				getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);

		applicationContext.refresh();

		// Act
		StackResourceRegistry autoDetectingStackNameProviderBasedStackResourceRegistry = applicationContext
				.getBean(
						"org.springframework.cloud.aws.core.env.stack.config.StackResourceRegistryFactoryBean#0",
						StackResourceRegistry.class);

		// Assert
		assertThat(autoDetectingStackNameProviderBasedStackResourceRegistry).isNotNull();

		server.removeContext(httpContext);
	}

	// @checkstyle:off
	@Test
	public void resourceIdResolverResolveToPhysicalResourceId_stackConfigurationWithStaticNameAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId() {
		// @checkstyle:on
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito
				.mock(AmazonCloudFormation.class);

		when(amazonCloudFormation.listStackResources(
				new ListStackResourcesRequest().withStackName("IntegrationTestStack")))
						.thenReturn(new ListStackResourcesResult()
								.withStackResourceSummaries(new StackResourceSummary()
										.withLogicalResourceId("EmptyBucket")
										.withPhysicalResourceId(
												"integrationteststack-emptybucket-foo")));

		applicationContext.load(new ClassPathResource(
				getClass().getSimpleName() + "-staticStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(
				getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);

		applicationContext.refresh();

		ResourceIdResolver resourceIdResolver = applicationContext
				.getBean(ResourceIdResolver.class);

		// Act
		String physicalResourceId = resourceIdResolver
				.resolveToPhysicalResourceId("EmptyBucket");

		// Assert
		assertThat(physicalResourceId).startsWith("integrationteststack-emptybucket-");
	}

	// @checkstyle:off
	@Test
	public void resourceIdResolverResolveToPhysicalResourceId_stackConfigurationWithoutStaticNameAndLogicalResourceIdOfExistingResourceProvided_returnsPhysicalResourceId()
			throws Exception {
		// @checkstyle:on
		// Arrange
		HttpServer server = MetaDataServer.setupHttpServer();
		HttpContext httpContext = server.createContext("/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("foo"));

		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito
				.mock(AmazonCloudFormation.class);
		when(amazonCloudFormation.describeStackResources(
				new DescribeStackResourcesRequest().withPhysicalResourceId("foo")))
						.thenReturn(new DescribeStackResourcesResult().withStackResources(
								new StackResource().withStackName("test")));

		when(amazonCloudFormation.listStackResources(
				new ListStackResourcesRequest().withStackName("test")))
						.thenReturn(new ListStackResourcesResult()
								.withStackResourceSummaries(new StackResourceSummary()
										.withLogicalResourceId("EmptyBucket")
										.withPhysicalResourceId(
												"integrationteststack-emptybucket-foo")));

		applicationContext.load(new ClassPathResource(
				getClass().getSimpleName() + "-autoDetectStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(
				getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);

		applicationContext.refresh();

		ResourceIdResolver resourceIdResolver = applicationContext
				.getBean(ResourceIdResolver.class);

		// Act
		String physicalResourceId = resourceIdResolver
				.resolveToPhysicalResourceId("EmptyBucket");

		// Assert
		assertThat(physicalResourceId).startsWith("integrationteststack-emptybucket-");

		server.removeContext(httpContext);
	}

	// @checkstyle:off
	@Test
	public void resourceIdResolverResolveToPhysicalResourceId_logicalResourceIdOfNonExistingResourceProvided_returnsLogicalResourceIdAsPhysicalResourceId() {
		// @checkstyle:on
		// Arrange
		GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext();
		AmazonCloudFormation amazonCloudFormation = Mockito
				.mock(AmazonCloudFormation.class);

		when(amazonCloudFormation.listStackResources(
				new ListStackResourcesRequest().withStackName("IntegrationTestStack")))
						.thenReturn(new ListStackResourcesResult()
								.withStackResourceSummaries(new StackResourceSummary()));

		applicationContext.load(new ClassPathResource(
				getClass().getSimpleName() + "-staticStackName.xml", getClass()));
		applicationContext.getBeanFactory().registerSingleton(
				getBeanName(AmazonCloudFormation.class.getName()), amazonCloudFormation);

		applicationContext.refresh();
		ResourceIdResolver resourceIdResolver = applicationContext
				.getBean(ResourceIdResolver.class);

		// Act
		String physicalResourceId = resourceIdResolver
				.resolveToPhysicalResourceId("nonExistingLogicalResourceId");

		// Assert
		assertThat(physicalResourceId).isEqualTo("nonExistingLogicalResourceId");
	}

	@After
	public void destroyMetaDataServer() throws Exception {
		MetaDataServer.shutdownHttpServer();

	}

}
