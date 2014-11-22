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

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Test;
import org.springframework.cloud.aws.context.MetaDataServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
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
		this.context.register(ContextInstanceDataConfiguration.class);

		//Act
		this.context.refresh();

		//Assert
		assertTrue(this.context.getBeanFactoryPostProcessors().isEmpty());
	}

	@Test
	public void propertySource_cloudEnvironment_propertySourceConfigured() throws Exception {
		//Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		httpServer.createContext("/latest/meta-data/instance-id", new MetaDataServer.HttpResponseWriterHandler("test"));


		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextInstanceDataConfiguration.class);

		//Act
		this.context.refresh();

		//Assert
		assertEquals("test", this.context.getEnvironment().getProperty("instance-id"));
	}

	@Test
	public void propertySource_enableInstanceData_propertySourceConfigured() throws Exception {
		//Arrange
		HttpServer httpServer = MetaDataServer.setupHttpServer();
		httpServer.createContext("/latest/meta-data/instance-id", new MetaDataServer.HttpResponseWriterHandler("test"));

		//Act
		this.context = new AnnotationConfigApplicationContext(ApplicationConfiguration.class);

		//Assert
		assertEquals("test", this.context.getEnvironment().getProperty("instance-id"));
	}

	@Configuration
	@EnableInstanceData
	public static class ApplicationConfiguration {

	}
}