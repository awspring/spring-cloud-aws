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

package org.springframework.cloud.aws.autoconfigure.context;

import java.lang.reflect.Field;
import java.util.Collections;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.aws.context.support.env.AwsCloudEnvironmentCheckUtils;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.env.stack.StackResourceRegistry;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextStackAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@Before
	public void restContextInstanceDataCondition() throws IllegalAccessException {
		Field field = ReflectionUtils.findField(AwsCloudEnvironmentCheckUtils.class,
				"isCloudEnvironment");
		assertThat(field).isNotNull();
		ReflectionUtils.makeAccessible(field);
		field.set(null, null);
	}

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void stackRegistry_autoConfigurationEnabled_returnsAutoConfiguredStackRegistry()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(AutoConfigurationStackRegistryTestConfiguration.class);
		this.context.register(ContextStackAutoConfiguration.class);
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext httpContext = httpServer.createContext(
				"/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("test"));

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(StackResourceRegistry.class)).isNotNull();

		httpServer.removeContext(httpContext);
		MetaDataServer.shutdownHttpServer();
	}

	@Test
	public void stackRegistry_manualConfigurationEnabled_returnsAutoConfiguredStackRegistry()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ManualConfigurationStackRegistryTestConfiguration.class);
		this.context.register(ContextStackAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.stack.name:manualStackName")
				.applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(StackResourceRegistry.class)).isNotNull();
	}

	@Test
	public void resourceIdResolver_withoutAnyStackConfiguration_availableAsConfiguredBean()
			throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextStackAutoConfiguration.class);
		TestPropertyValues.of("cloud.aws.stack.auto:false").applyTo(this.context);
		// Act
		this.context.refresh();

		// Assert
		assertThat(this.context.getBean(ResourceIdResolver.class)).isNotNull();
		assertThat(this.context.getBeansOfType(StackResourceRegistry.class).isEmpty())
				.isTrue();
	}

	@Configuration
	static class AutoConfigurationStackRegistryTestConfiguration {

		@Bean
		public AmazonCloudFormation amazonCloudFormation() {
			AmazonCloudFormation amazonCloudFormation = Mockito
					.mock(AmazonCloudFormation.class);
			Mockito.when(amazonCloudFormation.describeStackResources(
					new DescribeStackResourcesRequest().withPhysicalResourceId("test")))
					.thenReturn(new DescribeStackResourcesResult().withStackResources(
							new StackResource().withStackName("testStack")));
			Mockito.when(amazonCloudFormation.listStackResources(
					new ListStackResourcesRequest().withStackName("testStack")))
					.thenReturn(new ListStackResourcesResult()
							.withStackResourceSummaries(Collections.emptyList()));
			return amazonCloudFormation;
		}

	}

	@Configuration
	static class ManualConfigurationStackRegistryTestConfiguration {

		@Bean
		public AmazonCloudFormation amazonCloudFormation() {
			AmazonCloudFormation amazonCloudFormation = Mockito
					.mock(AmazonCloudFormation.class);
			Mockito.when(amazonCloudFormation.listStackResources(
					new ListStackResourcesRequest().withStackName("manualStackName")))
					.thenReturn(new ListStackResourcesResult()
							.withStackResourceSummaries(Collections.emptyList()));
			return amazonCloudFormation;
		}

	}

}
