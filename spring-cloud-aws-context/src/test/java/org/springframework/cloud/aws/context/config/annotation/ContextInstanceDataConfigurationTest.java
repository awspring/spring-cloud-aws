/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.cloud.aws.context.config.annotation;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.aws.context.MetaDataServer;
import org.springframework.cloud.aws.context.support.env.AwsCloudEnvironmentCheckUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ContextInstanceDataConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
		MetaDataServer.shutdownHttpServer();
	}

	@Test
	public void propertySource_nonCloudEnvironment_noBeanConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ApplicationConfiguration.class);

		//Act
		this.context.refresh();

		//Assert
		assertTrue(this.context.getBeanFactoryPostProcessors().isEmpty());
	}

	@Test
	public void propertySource_enableInstanceData_propertySourceConfigured() throws Exception {
		//Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext httpContext = httpServer.createContext("/latest/meta-data/instance-id", new MetaDataServer.HttpResponseWriterHandler("test"));

		//Act
		this.context = new AnnotationConfigApplicationContext(ApplicationConfiguration.class);

		//Assert
		assertEquals("test", this.context.getEnvironment().getProperty("instance-id"));
		httpServer.removeContext(httpContext);
	}

	@Test
	public void propertySource_enableInstanceDataWithCustomAttributeSeparator_propertySourceConfiguredAndUsesCustomAttributeSeparator() throws Exception {
		//Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext httpContext = httpServer.createContext("/latest/user-data", new MetaDataServer.HttpResponseWriterHandler("a:b/c:d"));

		//Act
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithCustomAttributeSeparator.class);

		//Assert
		assertEquals("b", this.context.getEnvironment().getProperty("a"));
		assertEquals("d", this.context.getEnvironment().getProperty("c"));

		httpServer.removeContext(httpContext);
	}

	@Test
	public void propertySource_enableInstanceDataWithCustomValueSeparator_propertySourceConfiguredAndUsesCustomValueSeparator() throws Exception {
		//Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		HttpContext httpContext = httpServer.createContext("/latest/user-data", new MetaDataServer.HttpResponseWriterHandler("a=b;c=d"));

		//Act
		this.context = new AnnotationConfigApplicationContext(ApplicationConfigurationWithCustomValueSeparator.class);

		//Assert
		assertEquals("b", this.context.getEnvironment().getProperty("a"));
		assertEquals("d", this.context.getEnvironment().getProperty("c"));

		httpServer.removeContext(httpContext);
	}

	@Configuration
	@EnableContextInstanceData
	public static class ApplicationConfiguration {

	}

	@Configuration
	@EnableContextInstanceData(attributeSeparator = "/")
	public static class ApplicationConfigurationWithCustomAttributeSeparator {

	}

	@Configuration
	@EnableContextInstanceData(valueSeparator = "=")
	public static class ApplicationConfigurationWithCustomValueSeparator {

	}

	@Before
	public void restContextInstanceDataCondition() throws IllegalAccessException {
		Field field = ReflectionUtils.findField(AwsCloudEnvironmentCheckUtils.class, "isCloudEnvironment");
		assertNotNull(field);
		ReflectionUtils.makeAccessible(field);
		field.set(null, null);
	}
}
