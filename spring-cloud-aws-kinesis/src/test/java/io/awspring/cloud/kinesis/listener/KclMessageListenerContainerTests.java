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
package io.awspring.cloud.kinesis.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.coordinator.Scheduler;

class KclMessageListenerContainerTests {

	@Test
	void schedulerFailureClearsActiveLifecycleState() {
		KclMessageListenerContainer container = container();
		Scheduler scheduler = mock(Scheduler.class);
		ExecutorService executor = mock(ExecutorService.class);
		doThrow(new IllegalStateException("boom")).when(scheduler).run();
		ReflectionTestUtils.setField(container, "scheduler", scheduler);
		ReflectionTestUtils.setField(container, "executorService", executor);
		ReflectionTestUtils.setField(container, "running", true);

		ReflectionTestUtils.invokeMethod(container, "runScheduler", scheduler);

		assertThat(container.isRunning()).isFalse();
		assertThat(ReflectionTestUtils.getField(container, "scheduler")).isNull();
		assertThat(ReflectionTestUtils.getField(container, "executorService")).isNull();
		verify(executor).shutdown();
	}

	@Test
	void staleSchedulerCannotClearReplacementLifecycleState() {
		KclMessageListenerContainer container = container();
		Scheduler staleScheduler = mock(Scheduler.class);
		Scheduler activeScheduler = mock(Scheduler.class);
		ExecutorService executor = mock(ExecutorService.class);
		doThrow(new IllegalStateException("boom")).when(staleScheduler).run();
		ReflectionTestUtils.setField(container, "scheduler", activeScheduler);
		ReflectionTestUtils.setField(container, "executorService", executor);
		ReflectionTestUtils.setField(container, "running", true);

		ReflectionTestUtils.invokeMethod(container, "runScheduler", staleScheduler);

		assertThat(container.isRunning()).isTrue();
		assertThat(ReflectionTestUtils.getField(container, "scheduler")).isSameAs(activeScheduler);
		assertThat(ReflectionTestUtils.getField(container, "executorService")).isSameAs(executor);
		verify(executor, never()).shutdown();
	}

	private KclMessageListenerContainer container() {
		return new KclMessageListenerContainer(mock(KinesisAsyncClient.class), mock(DynamoDbAsyncClient.class),
				mock(CloudWatchAsyncClient.class), "stream", "application", KclContainerOptions.builder().build());
	}

}
