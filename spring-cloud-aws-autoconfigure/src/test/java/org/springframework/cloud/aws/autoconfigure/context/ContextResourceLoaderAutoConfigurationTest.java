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

package org.springframework.cloud.aws.autoconfigure.context;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.aws.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContextResourceLoaderAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() throws Exception {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createResourceLoader_withCustomTaskExecutorSettings_executorConfigured() throws Exception {
		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextResourceLoaderAutoConfiguration.class);
		this.context.register(ApplicationBean.class);

		EnvironmentTestUtils.addEnvironment(this.context, "cloud.aws.loader.corePoolSize:10",
				"cloud.aws.loader.maxPoolSize:20",
				"cloud.aws.loader.queueCapacity:0");

		//Act
		this.context.refresh();

		//Assert
		PathMatchingSimpleStorageResourcePatternResolver resourceLoader = this.context.getBean(ApplicationBean.class).getResourceLoader();
		assertNotNull(resourceLoader);

		SimpleStorageResourceLoader simpleStorageResourceLoader = (SimpleStorageResourceLoader) ReflectionTestUtils.getField(resourceLoader, "simpleStorageResourceLoader");
		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) ReflectionTestUtils.getField(simpleStorageResourceLoader, "taskExecutor");
		assertNotNull(taskExecutor);

		assertEquals(10, taskExecutor.getCorePoolSize());
		assertEquals(20, taskExecutor.getMaxPoolSize());
		assertEquals(0, ReflectionTestUtils.getField(taskExecutor, "queueCapacity"));
	}

	@Test
	public void createResourceLoader_withoutExecutorSettings_executorConfigured() throws Exception {

		//Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextResourceLoaderAutoConfiguration.class);
		this.context.register(ApplicationBean.class);

		//Act
		this.context.refresh();

		//Assert
		PathMatchingSimpleStorageResourcePatternResolver resourceLoader = this.context.getBean(ApplicationBean.class).getResourceLoader();
		assertNotNull(resourceLoader);

		SimpleStorageResourceLoader simpleStorageResourceLoader = (SimpleStorageResourceLoader) ReflectionTestUtils.getField(resourceLoader, "simpleStorageResourceLoader");
		SyncTaskExecutor taskExecutor = (SyncTaskExecutor) ReflectionTestUtils.getField(simpleStorageResourceLoader, "taskExecutor");
		assertNotNull(taskExecutor);
	}


	static class ApplicationBean {

		@Autowired
		private ResourceLoader resourceLoader;

		public PathMatchingSimpleStorageResourcePatternResolver getResourceLoader() {
			return (PathMatchingSimpleStorageResourcePatternResolver) this.resourceLoader;
		}
	}
}
