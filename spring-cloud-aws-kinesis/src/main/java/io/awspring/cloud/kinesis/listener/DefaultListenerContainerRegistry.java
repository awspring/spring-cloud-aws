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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * @author Matej Nedic
 * @since 4.2.0
 */
public class DefaultListenerContainerRegistry implements MessageListenerContainerRegistry, SmartLifecycle {

	private final Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running;

	private boolean parallelLifecycle = true;

	private final TaskExecutor taskExecutor = createTaskExecutor();

	@Override
	public void registerListenerContainer(MessageListenerContainer container) {
		Assert.notNull(container, "container must not be null");
		String id = container.getId();
		Assert.state(!this.containers.containsKey(id), () -> "Already registered container with id " + id);
		this.containers.put(id, container);
	}

	@Override
	public Collection<MessageListenerContainer> getListenerContainers() {
		return Collections.unmodifiableCollection(this.containers.values());
	}

	@Override
	@Nullable
	public MessageListenerContainer getContainerById(String id) {
		Assert.notNull(id, "id cannot be null.");
		return this.containers.get(id);
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				return;
			}
			this.running = true;
			List<MessageListenerContainer> containersToStart = this.containers.values().stream()
					.filter(SmartLifecycle::isAutoStartup).toList();
			manageLifecycle(MessageListenerContainer::start, containersToStart);
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				return;
			}
			this.running = false;
			manageLifecycle(MessageListenerContainer::stop, this.containers.values());
		}
	}

	private void manageLifecycle(Consumer<MessageListenerContainer> action,
			Collection<MessageListenerContainer> containers) {
		if (this.parallelLifecycle) {
			CompletableFuture.allOf(containers.stream()
					.map(container -> CompletableFuture.runAsync(() -> action.accept(container), this.taskExecutor))
					.toArray(CompletableFuture[]::new)).join();
		}
		else {
			containers.forEach(action);
		}
	}

	public void setParallelLifecycle(boolean parallelLifecycle) {
		this.parallelLifecycle = parallelLifecycle;
	}

	private static TaskExecutor createTaskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setThreadNamePrefix("kinesis-lifecycle-thread-");
		return executor;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
