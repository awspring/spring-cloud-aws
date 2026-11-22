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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Holds the listener containers and manages their lifecycle as a single {@link SmartLifecycle} bean.
 * <p>
 * The containers are not Spring beans themselves, so their {@link MessageListenerContainer#getPhase() phases} are
 * honoured here: the registry starts the containers in ascending phase order and stops them in descending order, with
 * the containers of one phase started or stopped in parallel (see {@link #setParallelLifecycle(boolean)}). Towards the
 * rest of the application the registry runs in a single phase: the one set through {@link #setPhase(int)}, or otherwise
 * the highest phase among its containers, so the listeners start after and stop before the beans of every phase a
 * container asks to follow.
 *
 * @author Matej Nedic
 * @since 4.3.0
 */
public class DefaultListenerContainerRegistry implements MessageListenerContainerRegistry, SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(DefaultListenerContainerRegistry.class);

	private final Map<String, MessageListenerContainer> containers = new ConcurrentHashMap<>();

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running;

	private boolean parallelLifecycle = true;

	@Nullable
	private Integer phase;

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
			List<MessageListenerContainer> containersToStart = this.containers.values().stream()
					.filter(SmartLifecycle::isAutoStartup).toList();
			try {
				byPhase(containersToStart, Comparator.naturalOrder())
						.forEach(group -> manageLifecycle(MessageListenerContainer::start, group));
			}
			catch (RuntimeException ex) {
				// Leave nothing half-started: the registry reports not running, so a
				// later stop() would skip the
				// containers that did start and their KCL workers would keep holding
				// leases.
				stopStartedContainers();
				throw unwrap(ex);
			}
			this.running = true;
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				return;
			}
			this.running = false;
			byPhase(this.containers.values(), Comparator.reverseOrder())
					.forEach(group -> manageLifecycle(MessageListenerContainer::stop, group));
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

	private void stopStartedContainers() {
		List<MessageListenerContainer> started = this.containers.values().stream()
				.filter(MessageListenerContainer::isRunning).toList();
		try {
			byPhase(started, Comparator.reverseOrder())
					.forEach(group -> manageLifecycle(MessageListenerContainer::stop, group));
		}
		catch (RuntimeException stopFailure) {
			logger.warn("Failed to stop containers after a container failed to start", stopFailure);
		}
	}

	/**
	 * Unwraps the {@link CompletionException} the parallel lifecycle wraps container failures in.
	 */
	private static RuntimeException unwrap(RuntimeException ex) {
		if (ex instanceof CompletionException && ex.getCause() instanceof RuntimeException cause) {
			return cause;
		}
		return ex;
	}

	/**
	 * Groups the containers by phase, ordering the groups with the given comparator.
	 */
	private static Collection<List<MessageListenerContainer>> byPhase(Collection<MessageListenerContainer> containers,
			Comparator<Integer> phaseOrder) {
		Map<Integer, List<MessageListenerContainer>> groups = new TreeMap<>(phaseOrder);
		containers.forEach(
				container -> groups.computeIfAbsent(container.getPhase(), phase -> new ArrayList<>()).add(container));
		return groups.values();
	}

	public void setParallelLifecycle(boolean parallelLifecycle) {
		this.parallelLifecycle = parallelLifecycle;
	}

	/**
	 * Set the phase the registry, and so all its containers, start and stop in relative to the rest of the application.
	 * When unset, the registry uses the highest phase among its containers, or {@link SmartLifecycle#DEFAULT_PHASE}
	 * when it has none.
	 * @param phase the phase.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public int getPhase() {
		if (this.phase != null) {
			return this.phase;
		}
		return this.containers.values().stream().mapToInt(MessageListenerContainer::getPhase).max()
				.orElse(DEFAULT_PHASE);
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
