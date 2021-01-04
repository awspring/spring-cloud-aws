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

package io.awspring.cloud.context.config.annotation;

import java.util.Collections;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.ListStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import io.awspring.cloud.context.MetaDataServer;
import io.awspring.cloud.core.env.stack.StackResourceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ContextStackConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@AfterEach
	void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}

		MetaDataServer.shutdownHttpServer();
	}

	@Test
	void stackRegistry_noStackNameConfigured_returnsAutoConfiguredStackRegistry() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfigurationWithEmptyStackName.class);
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext httpContext = httpServer.createContext("/latest/meta-data/instance-id",
				new MetaDataServer.HttpResponseWriterHandler("test"));

		// Act
		this.context.refresh();

		// Assert
		StackResourceRegistry stackResourceRegistry = this.context.getBean(StackResourceRegistry.class);
		assertThat(stackResourceRegistry).isNotNull();
		assertThat(stackResourceRegistry.getStackName()).isEqualTo("testStack");

		httpServer.removeContext(httpContext);
	}

	@Test
	void stackRegistry_stackNameConfigured_returnsConfiguredStackRegistryForName() throws Exception {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ManualConfigurationStackRegistryTestConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		StackResourceRegistry stackResourceRegistry = this.context.getBean(StackResourceRegistry.class);
		assertThat(stackResourceRegistry).isNotNull();
		assertThat(stackResourceRegistry.getStackName()).isEqualTo("manualStackName");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableStackConfiguration
	static class ApplicationConfigurationWithEmptyStackName {

		@Bean
		AmazonCloudFormation amazonCloudFormation() {
			AmazonCloudFormation amazonCloudFormation = Mockito.mock(AmazonCloudFormation.class);
			Mockito.when(amazonCloudFormation
					.describeStackResources(new DescribeStackResourcesRequest().withPhysicalResourceId("test")))
					.thenReturn(new DescribeStackResourcesResult()
							.withStackResources(new StackResource().withStackName("testStack")));
			Mockito.when(
					amazonCloudFormation.listStackResources(new ListStackResourcesRequest().withStackName("testStack")))
					.thenReturn(new ListStackResourcesResult().withStackResourceSummaries(Collections.emptyList()));
			return amazonCloudFormation;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableStackConfiguration(stackName = "manualStackName")
	static class ManualConfigurationStackRegistryTestConfiguration {

		@Bean
		AmazonCloudFormation amazonCloudFormation() {
			AmazonCloudFormation amazonCloudFormation = Mockito.mock(AmazonCloudFormation.class);
			Mockito.when(amazonCloudFormation
					.listStackResources(new ListStackResourcesRequest().withStackName("manualStackName")))
					.thenReturn(new ListStackResourcesResult().withStackResourceSummaries(Collections.emptyList()));
			return amazonCloudFormation;
		}

	}

}
