/*
 * Copyright 2013-2022 the original author or authors.
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
package io.awspring.cloud.sqs.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;

/**
 * Tests for {@link DefaultListenerContainerRegistry}.
 *
 * @author Tomaz Fernandes
 * @author Sharang Gupta
 */
@SuppressWarnings("unchecked")
class DefaultListenerContainerRegistryTests {

	@Test
	void shouldRegisterListenerContainer() {
		MessageListenerContainer<Object> container = mock(MessageListenerContainer.class);
		String id = "test-container-id";
		given(container.getId()).willReturn(id);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container);
		assertThat(registry.getPhase()).isEqualTo(MessageListenerContainer.DEFAULT_PHASE);
	}

	@Test
	void shouldGetListenerContainer() {
		MessageListenerContainer<Object> container = mock(MessageListenerContainer.class);
		String id = "test-container-id";
		given(container.getId()).willReturn(id);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.setPhase(2);
		registry.registerListenerContainer(container);
		MessageListenerContainer<?> containerFromRegistry = registry.getContainerById(id);
		assertThat(containerFromRegistry).isEqualTo(container);
		assertThat(registry.getPhase()).isEqualTo(2);
	}

	@Test
	void shouldGetAllListenerContainers() {
		MessageListenerContainer<Object> container1 = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> container2 = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> container3 = mock(MessageListenerContainer.class);
		String id1 = "test-container-id-1";
		String id2 = "test-container-id-2";
		String id3 = "test-container-id-3";
		given(container1.getId()).willReturn(id1);
		given(container2.getId()).willReturn(id2);
		given(container3.getId()).willReturn(id3);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container1);
		registry.registerListenerContainer(container2);
		registry.registerListenerContainer(container3);
		assertThat(registry.getListenerContainers()).containsExactlyInAnyOrder(container1, container2, container3);
	}

	@Test
	void shouldStartAndStopAllListenerContainers() {
		MessageListenerContainer<Object> container1 = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> container2 = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> container3 = mock(MessageListenerContainer.class);
		String id1 = "test-container-id-1";
		String id2 = "test-container-id-2";
		String id3 = "test-container-id-3";
		given(container1.getId()).willReturn(id1);
		given(container1.isAutoStartup()).willReturn(true);
		given(container2.getId()).willReturn(id2);
		given(container2.isAutoStartup()).willReturn(true);
		given(container3.getId()).willReturn(id3);
		given(container3.isAutoStartup()).willReturn(true);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container1);
		registry.registerListenerContainer(container2);
		registry.registerListenerContainer(container3);
		registry.start();
		assertThat(registry.isRunning()).isTrue();
		registry.stop();
		assertThat(registry.isRunning()).isFalse();
		then(container1).should().start();
		then(container1).should().stop();
		then(container2).should().start();
		then(container2).should().stop();
		then(container3).should().start();
		then(container3).should().stop();
	}

	@Test
	void shouldNotStartContainerWithAutoStartupFalse() {
		MessageListenerContainer<Object> container1 = mock(MessageListenerContainer.class);
		String id1 = "test-container-id-1";
		given(container1.getId()).willReturn(id1);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container1);
		registry.start();
		assertThat(registry.isRunning()).isTrue();
		registry.stop();
		assertThat(registry.isRunning()).isFalse();
		then(container1).should(times(0)).start();
		then(container1).should().stop();
	}

	@Test
	void shouldThrowIfIdAlreadyPresent() {
		MessageListenerContainer<Object> container = mock(MessageListenerContainer.class);
		String id = "test-container-id";
		given(container.getId()).willReturn(id);
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container);
		assertThatThrownBy(() -> registry.registerListenerContainer(container))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRemainRunningAfterContainerFailsToStart() {
		CountDownLatch healthyContainerUpSignal = new CountDownLatch(1);
		MessageListenerContainer<Object> healthyContainer = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> faultyContainer = mock(MessageListenerContainer.class);
		String healthyId = "healthy-test-container-id";
		String faultyId = "faulty-test-container-id";
		given(healthyContainer.getId()).willReturn(healthyId);
		given(healthyContainer.isAutoStartup()).willReturn(true);
		given(faultyContainer.getId()).willReturn(faultyId);
		given(faultyContainer.isAutoStartup()).willReturn(true);
		willAnswer(invocation -> {
			healthyContainerUpSignal.countDown();
			return null;
		}).given(healthyContainer).start();
		String containerFailedToStartMessage = "Container failed to start";
		willAnswer(invocation -> {
			// Wait until the healthy container has started before failing the faulty container.
			if (!healthyContainerUpSignal.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Test setup failed: healthy container start was not called");
			}
			throw new IllegalStateException(containerFailedToStartMessage);
		}).given(faultyContainer).start();
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(healthyContainer);
		registry.registerListenerContainer(faultyContainer);
		assertThatThrownBy(registry::start).isInstanceOf(CompletionException.class).cause()
				.isInstanceOf(RuntimeException.class).hasMessage(containerFailedToStartMessage);
		assertThat(registry.isRunning()).isTrue();
		registry.stop();
		then(healthyContainer).should(times(1)).stop();
		then(faultyContainer).should(times(1)).stop();
		assertThat(registry.isRunning()).isFalse();
	}

	@Test
	void shouldBeStoppedBySpringLifecycleProcessorWhenOneContainerFailsToStart() {
		CountDownLatch healthyContainerUpSignal = new CountDownLatch(1);
		MessageListenerContainer<Object> healthyContainer = mock(MessageListenerContainer.class);
		MessageListenerContainer<Object> faultyContainer = mock(MessageListenerContainer.class);
		String healthyId = "healthy-test-container-id";
		String faultyId = "faulty-test-container-id";
		given(healthyContainer.getId()).willReturn(healthyId);
		given(healthyContainer.isAutoStartup()).willReturn(true);
		given(faultyContainer.getId()).willReturn(faultyId);
		given(faultyContainer.isAutoStartup()).willReturn(true);
		willAnswer(invocation -> {
			healthyContainerUpSignal.countDown();
			return null;
		}).given(healthyContainer).start();
		String containerFailedToStartMessage = "Container failed to start";
		willAnswer(invocation -> {
			if (!healthyContainerUpSignal.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Test setup failed: healthy container start was not called");
			}
			throw new IllegalStateException(containerFailedToStartMessage);
		}).given(faultyContainer).start();
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(healthyContainer);
		registry.registerListenerContainer(faultyContainer);
		GenericApplicationContext context = new GenericApplicationContext();

		try (context) {
			context.registerBean(DefaultListenerContainerRegistry.class, () -> registry);
			assertThatThrownBy(context::refresh).isInstanceOf(ApplicationContextException.class)
					.hasMessageContaining(DefaultListenerContainerRegistry.class.getName()).cause()
					.isInstanceOf(CompletionException.class).cause().isInstanceOf(IllegalStateException.class)
					.hasMessage(containerFailedToStartMessage);

			then(healthyContainer).should(times(1)).stop();
			then(faultyContainer).should(times(1)).stop();
			assertThat(registry.isRunning()).isFalse();
		}
	}

	@Test
	void shouldStartContainersDuringLifecycleStartByDefault() {
		MessageListenerContainer<Object> container = autoStartupContainer("test-container-id");
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.registerListenerContainer(container);
		ContainerStartProbe probe = new ContainerStartProbe(container);

		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBean(DefaultListenerContainerRegistry.class, () -> registry);
			context.registerBean(ContainerStartProbe.class, () -> probe);
			context.refresh();

			assertThat(registry.isStartAfterContextRefresh()).isFalse();
			assertThat(probe.startedBeforeContextRefreshed).isTrue();
			then(container).should(times(1)).start();
		}
	}

	@Test
	void shouldStartContainersAfterContextRefreshWhenConfigured() {
		MessageListenerContainer<Object> container = autoStartupContainer("test-container-id");
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.setStartAfterContextRefresh(true);
		registry.registerListenerContainer(container);
		ContainerStartProbe probe = new ContainerStartProbe(container);

		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBean(DefaultListenerContainerRegistry.class, () -> registry);
			context.registerBean(ContainerStartProbe.class, () -> probe);
			context.refresh();

			assertThat(probe.startedBeforeContextRefreshed).isFalse();
			assertThat(registry.isRunning()).isTrue();
			then(container).should(times(1)).start();
		}
		then(container).should().stop();
	}

	@Test
	void shouldNotStartContainersBeforeContextRefreshWhenConfigured() {
		MessageListenerContainer<Object> container = autoStartupContainer("test-container-id");
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.setStartAfterContextRefresh(true);
		registry.registerListenerContainer(container);

		registry.start();

		assertThat(registry.isRunning()).isTrue();
		then(container).should(never()).start();
	}

	@Test
	void shouldNotStartContainerWithAutoStartupFalseAfterContextRefresh() {
		MessageListenerContainer<Object> container = mock(MessageListenerContainer.class);
		given(container.getId()).willReturn("test-container-id");
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.setStartAfterContextRefresh(true);
		registry.registerListenerContainer(container);

		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBean(DefaultListenerContainerRegistry.class, () -> registry);
			context.refresh();

			then(container).should(never()).start();
		}
	}

	@Test
	void shouldNotStartContainersWhenAnotherContextIsRefreshed() {
		MessageListenerContainer<Object> container = autoStartupContainer("test-container-id");
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.setStartAfterContextRefresh(true);
		registry.registerListenerContainer(container);

		try (GenericApplicationContext context = new GenericApplicationContext();
				GenericApplicationContext otherContext = new GenericApplicationContext()) {
			registry.setApplicationContext(context);
			registry.start();
			registry.onApplicationEvent(new ContextRefreshedEvent(otherContext));

			then(container).should(never()).start();
		}
	}

	@Test
	void shouldStartContainersOnRestartOnceContextIsRefreshed() {
		MessageListenerContainer<Object> container = autoStartupContainer("test-container-id");
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.setStartAfterContextRefresh(true);
		registry.registerListenerContainer(container);

		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBean(DefaultListenerContainerRegistry.class, () -> registry);
			context.refresh();
			registry.stop();
			registry.start();

			then(container).should(times(2)).start();
		}
	}

	@Test
	void shouldFailContextRefreshWhenContainerFailsToStartAfterContextRefresh() {
		MessageListenerContainer<Object> container = autoStartupContainer("test-container-id");
		String containerFailedToStartMessage = "Container failed to start";
		willAnswer(invocation -> {
			throw new IllegalStateException(containerFailedToStartMessage);
		}).given(container).start();
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.setStartAfterContextRefresh(true);
		registry.registerListenerContainer(container);

		try (GenericApplicationContext context = new GenericApplicationContext()) {
			context.registerBean(DefaultListenerContainerRegistry.class, () -> registry);
			assertThatThrownBy(context::refresh).hasRootCauseInstanceOf(IllegalStateException.class)
					.hasRootCauseMessage(containerFailedToStartMessage);

			then(container).should(times(1)).stop();
			assertThat(registry.isRunning()).isFalse();
		}
	}

	private MessageListenerContainer<Object> autoStartupContainer(String id) {
		MessageListenerContainer<Object> container = mock(MessageListenerContainer.class);
		given(container.getId()).willReturn(id);
		given(container.isAutoStartup()).willReturn(true);
		return container;
	}

	/**
	 * Records whether the container was already started when the first {@link ContextRefreshedEvent} listener runs.
	 */
	private static class ContainerStartProbe implements ApplicationListener<ContextRefreshedEvent>, Ordered {

		private final AtomicBoolean started = new AtomicBoolean();

		private volatile boolean startedBeforeContextRefreshed;

		ContainerStartProbe(MessageListenerContainer<Object> container) {
			willAnswer(invocation -> {
				this.started.set(true);
				return null;
			}).given(container).start();
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			this.startedBeforeContextRefreshed = this.started.get();
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

	}
}
