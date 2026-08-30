/*
 * Copyright 2013-2026 the original author or authors.
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

package io.awspring.cloud.kinesis.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.mock.env.MockEnvironment;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

class KinesisVirtualThreadsTest {

	@Test
	void kinesisAdapterVirtualThreadsEnabled() {
		GenericApplicationContext context = new GenericApplicationContext();
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.threads.virtual.enabled", "true");
		context.setEnvironment(env);
		context.refresh();

		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		KinesisMessageDrivenChannelAdapter adapter = new KinesisMessageDrivenChannelAdapter(client, "stream1");
		adapter.setApplicationContext(context);
		adapter.setBeanFactory(context.getBeanFactory());
		adapter.afterPropertiesSet();

		Object consumerExecutor = TestUtils.getPropertyValue(adapter, "consumerExecutor");
		Object dispatcherExecutor = TestUtils.getPropertyValue(adapter, "dispatcherExecutor");

		assertThat(consumerExecutor).isNotNull();
		assertThat(dispatcherExecutor).isNotNull();

		ExecutorService consumer = (ExecutorService) consumerExecutor;
		ExecutorService dispatcher = (ExecutorService) dispatcherExecutor;

		try {
			consumer.submit(() -> {
				assertThat(Thread.currentThread().isVirtual()).isTrue();
			}).get();
			dispatcher.submit(() -> {
				assertThat(Thread.currentThread().isVirtual()).isTrue();
			}).get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void kinesisAdapterVirtualThreadsDisabled() {
		GenericApplicationContext context = new GenericApplicationContext();
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.threads.virtual.enabled", "false");
		context.setEnvironment(env);
		context.refresh();

		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		KinesisMessageDrivenChannelAdapter adapter = new KinesisMessageDrivenChannelAdapter(client, "stream1");
		adapter.setApplicationContext(context);
		adapter.setBeanFactory(context.getBeanFactory());
		adapter.afterPropertiesSet();

		Object consumerExecutor = TestUtils.getPropertyValue(adapter, "consumerExecutor");
		Object dispatcherExecutor = TestUtils.getPropertyValue(adapter, "dispatcherExecutor");

		assertThat(consumerExecutor).isNotNull();
		assertThat(dispatcherExecutor).isNotNull();

		ExecutorService consumer = (ExecutorService) consumerExecutor;
		ExecutorService dispatcher = (ExecutorService) dispatcherExecutor;

		try {
			consumer.submit(() -> {
				assertThat(Thread.currentThread().isVirtual()).isFalse();
			}).get();
			dispatcher.submit(() -> {
				assertThat(Thread.currentThread().isVirtual()).isFalse();
			}).get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void kclAdapterVirtualThreadsEnabled() {
		GenericApplicationContext context = new GenericApplicationContext();
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.threads.virtual.enabled", "true");
		context.setEnvironment(env);
		context.refresh();

		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient cloudWatch = mock(software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient.class);
		software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient dynamoDb = mock(software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient.class);
		KclMessageDrivenChannelAdapter adapter = new KclMessageDrivenChannelAdapter(client, cloudWatch, dynamoDb, "stream1");
		adapter.setApplicationContext(context);
		adapter.setBeanFactory(context.getBeanFactory());
		adapter.afterPropertiesSet();

		Object executor = TestUtils.getPropertyValue(adapter, "executor");
		assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);
		SimpleAsyncTaskExecutor sate = (SimpleAsyncTaskExecutor) executor;
		try {
			sate.submit(() -> {
				assertThat(Thread.currentThread().isVirtual()).isTrue();
			}).get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void kclAdapterVirtualThreadsDisabled() {
		GenericApplicationContext context = new GenericApplicationContext();
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.threads.virtual.enabled", "false");
		context.setEnvironment(env);
		context.refresh();

		KinesisAsyncClient client = mock(KinesisAsyncClient.class);
		software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient cloudWatch = mock(software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient.class);
		software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient dynamoDb = mock(software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient.class);
		KclMessageDrivenChannelAdapter adapter = new KclMessageDrivenChannelAdapter(client, cloudWatch, dynamoDb, "stream1");
		adapter.setApplicationContext(context);
		adapter.setBeanFactory(context.getBeanFactory());
		adapter.afterPropertiesSet();

		Object executor = TestUtils.getPropertyValue(adapter, "executor");
		assertThat(executor).isInstanceOf(SimpleAsyncTaskExecutor.class);
		SimpleAsyncTaskExecutor sate = (SimpleAsyncTaskExecutor) executor;
		try {
			sate.submit(() -> {
				assertThat(Thread.currentThread().isVirtual()).isFalse();
			}).get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
