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

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageProtocolResolver;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextResourceLoaderAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createResourceLoader_withCustomTaskExecutorSettings_executorConfigured() {
		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextResourceLoaderAutoConfiguration.class);

		TestPropertyValues.of("cloud.aws.loader.corePoolSize:10",
				"cloud.aws.loader.maxPoolSize:20", "cloud.aws.loader.queueCapacity:0")
				.applyTo(this.context);

		// Act
		this.context.refresh();

		// Assert
		SimpleStorageProtocolResolver simpleStorageProtocolResolver = (SimpleStorageProtocolResolver) this.context
				.getProtocolResolvers().iterator().next();
		ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) ReflectionTestUtils
				.getField(simpleStorageProtocolResolver, "taskExecutor");
		assertThat(taskExecutor).isNotNull();

		assertThat(taskExecutor.getCorePoolSize()).isEqualTo(10);
		assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(20);
		assertThat(ReflectionTestUtils.getField(taskExecutor, "queueCapacity"))
				.isEqualTo(0);
	}

	@Test
	public void createResourceLoader_withoutExecutorSettings_executorConfigured() {

		// Arrange
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ContextResourceLoaderAutoConfiguration.class);

		// Act
		this.context.refresh();

		// Assert
		SimpleStorageProtocolResolver simpleStorageProtocolResolver = (SimpleStorageProtocolResolver) this.context
				.getProtocolResolvers().iterator().next();
		SyncTaskExecutor taskExecutor = (SyncTaskExecutor) ReflectionTestUtils
				.getField(simpleStorageProtocolResolver, "taskExecutor");
		assertThat(taskExecutor).isNotNull();
	}

}
