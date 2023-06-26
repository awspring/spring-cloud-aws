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

import io.awspring.cloud.sqs.LifecycleHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link MessageListenerContainerRegistry} implementation that registers the {@link MessageListenerContainer} instances
 * and manage their lifecycle.
 *
 * This bean can be autowired and used to lookup container instances at runtime, which can be useful to e.g. manually
 * manage their lifecycle.
 *
 * A {@link LifecycleHandler} is used to manage the containers' lifecycle.
 *
 * Only containers created via {@link io.awspring.cloud.sqs.annotation.SqsListener} annotations are registered by the
 * framework.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class DefaultListenerContainerRegistry implements MessageListenerContainerRegistry {

	private static final Logger logger = LoggerFactory.getLogger(DefaultListenerContainerRegistry.class);

	private final Map<String, MessageListenerContainer<?>> listenerContainers = new ConcurrentHashMap<>();

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running = false;

	private int phase = MessageListenerContainer.DEFAULT_PHASE;

	@Override
	public void registerListenerContainer(MessageListenerContainer<?> listenerContainer) {
		Assert.notNull(listenerContainer, "listenerContainer cannot be null");
		Assert.isTrue(getContainerById(listenerContainer.getId()) == null,
				() -> "Already registered container with id " + listenerContainer.getId());
		logger.debug("Registering listener container {}", listenerContainer.getId());
		this.listenerContainers.put(listenerContainer.getId(), listenerContainer);
	}

	@Override
	public Collection<MessageListenerContainer<?>> getListenerContainers() {
		return Collections.unmodifiableCollection(this.listenerContainers.values());
	}

	@Nullable
	@Override
	public MessageListenerContainer<?> getContainerById(String id) {
		Assert.notNull(id, "id cannot be null.");
		return this.listenerContainers.get(id);
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			logger.debug("Starting {}", getClass().getSimpleName());
			List<MessageListenerContainer<?>> containersToStart = this.listenerContainers.values().stream()
					.filter(SmartLifecycle::isAutoStartup).collect(Collectors.toList());
			LifecycleHandler.get().start(containersToStart);
			this.running = true;
			logger.debug("{} started", getClass().getSimpleName());
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			logger.debug("Stopping {}", getClass().getSimpleName());
			this.running = false;
			LifecycleHandler.get().stop(this.listenerContainers.values());
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return phase;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}
}
