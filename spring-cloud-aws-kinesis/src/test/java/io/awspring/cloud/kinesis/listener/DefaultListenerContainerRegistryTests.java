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

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultListenerContainerRegistry}.
 *
 * @author Matej Nedic
 * @since 4.2.0
 */
class DefaultListenerContainerRegistryTests {

	@Test
	@DisplayName("start() starts only auto-startup containers")
	void startsOnlyAutoStartupContainers() {
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		TestContainer autoStart = new TestContainer("auto", true);
		TestContainer manualStart = new TestContainer("manual", false);
		registry.registerListenerContainer(autoStart);
		registry.registerListenerContainer(manualStart);

		registry.start();

		assertThat(autoStart.isRunning()).isTrue();
		assertThat(manualStart.isRunning()).isFalse();
		assertThat(registry.isRunning()).isTrue();
	}

	@Test
	@DisplayName("start() is idempotent")
	void startIsIdempotent() {
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		TestContainer container = new TestContainer("c1", true);
		registry.registerListenerContainer(container);

		registry.start();
		registry.start();

		assertThat(container.startCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("start() runs containers in parallel")
	void startsInParallel() throws Exception {
		assertParallel(true);
	}

	@Test
	@DisplayName("stop() runs containers in parallel")
	void stopsInParallel() throws Exception {
		assertParallel(false);
	}

	@Test
	@DisplayName("sequential mode does not run containers concurrently")
	void sequentialModeIsNotParallel() {
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		registry.setParallelLifecycle(false);
		CyclicBarrier barrier = new CyclicBarrier(2);
		registry.registerListenerContainer(new BarrierContainer("c1", barrier));
		registry.registerListenerContainer(new BarrierContainer("c2", barrier));

		assertThat(registry.isRunning()).isFalse();
		Thread thread = new Thread(registry::start);
		thread.setDaemon(true);
		thread.start();
		try {
			thread.join(500);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		assertThat(thread.isAlive()).isTrue();
	}

	private void assertParallel(boolean testStart) throws Exception {
		DefaultListenerContainerRegistry registry = new DefaultListenerContainerRegistry();
		CyclicBarrier barrier = new CyclicBarrier(2);
		BarrierContainer c1 = new BarrierContainer("c1", barrier);
		BarrierContainer c2 = new BarrierContainer("c2", barrier);
		registry.registerListenerContainer(c1);
		registry.registerListenerContainer(c2);

		if (testStart) {
			registry.start();
			assertThat(c1.awaited()).isTrue();
			assertThat(c2.awaited()).isTrue();
		}
		else {
			registry.start();
			barrier.reset();
			c1.resetAwaited();
			c2.resetAwaited();
			registry.stop();
			assertThat(c1.awaited()).isTrue();
			assertThat(c2.awaited()).isTrue();
		}
	}

	private static class TestContainer implements MessageListenerContainer {

		private String id;

		private final boolean autoStartup;

		private volatile boolean running;

		private int startCount;

		TestContainer(String id, boolean autoStartup) {
			this.id = id;
			this.autoStartup = autoStartup;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public void setId(String id) {
			this.id = id;
		}

		@Override
		public boolean isAutoStartup() {
			return this.autoStartup;
		}

		@Override
		public void start() {
			this.startCount++;
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		int startCount() {
			return this.startCount;
		}
	}

	private static class BarrierContainer implements MessageListenerContainer {

		private String id;

		private final CyclicBarrier barrier;

		private volatile boolean running;

		private final AtomicBoolean awaited = new AtomicBoolean(false);

		BarrierContainer(String id, CyclicBarrier barrier) {
			this.id = id;
			this.barrier = barrier;
		}

		@Override
		public String getId() {
			return this.id;
		}

		@Override
		public void setId(String id) {
			this.id = id;
		}

		@Override
		public void start() {
			this.running = true;
			await();
		}

		@Override
		public void stop() {
			this.running = false;
			await();
		}

		private void await() {
			try {
				this.barrier.await(2, TimeUnit.SECONDS);
				this.awaited.set(true);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			catch (BrokenBarrierException | TimeoutException ignored) {
			}
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		boolean awaited() {
			return this.awaited.get();
		}

		void resetAwaited() {
			this.awaited.set(false);
		}
	}
}
