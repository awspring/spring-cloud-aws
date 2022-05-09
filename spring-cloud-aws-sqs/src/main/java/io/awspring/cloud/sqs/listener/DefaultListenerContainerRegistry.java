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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import io.awspring.cloud.sqs.ConfigUtils;
import io.awspring.cloud.sqs.LifecycleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link MessageListenerContainerRegistry} implementation that registers the {@link MessageListenerContainer} instances
 * and automatically manage their lifecycle.
 *
 * This bean can be autowired and used to lookup container instances at runtime as well as manually manage their
 * lifecycle.
 *
 * Supports starting and stopping the container instances sequentially or in parallel.
 *
 * Note that only containers created via {@link io.awspring.cloud.sqs.annotation.SqsListener} annotations are registered
 * automatically.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class DefaultListenerContainerRegistry implements MessageListenerContainerRegistry {

	private static final Logger logger = LoggerFactory.getLogger(DefaultListenerContainerRegistry.class);

	private final Collection<MessageListenerContainer<?>> listenerContainers = new ArrayList<>();

	private boolean isParallelLifecycleManagement = true;

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running = false;

	@Override
	public void registerListenerContainer(MessageListenerContainer<?> listenerContainer) {
		logger.debug("Registering listener container {}", listenerContainer);
		Assert.state(getContainerById(listenerContainer.getId()) == null,
				() -> "Already registered container with id " + listenerContainer.getId());
		this.listenerContainers.add(listenerContainer);
	}

	@Override
	public Collection<MessageListenerContainer<?>> getListenerContainers() {
		return Collections.unmodifiableCollection(this.listenerContainers);
	}

	@Nullable
	@Override
	public MessageListenerContainer<?> getContainerById(String id) {
		Assert.notNull(id, "id cannot be null.");
		return this.listenerContainers.stream().filter(container -> container.getId().equals(id)).findFirst()
				.orElse(null);
	}

	/**
	 * Set to false if {@link org.springframework.context.SmartLifecycle} management should be made sequentially rather
	 * than in parallel.
	 * @param parallelLifecycleManagement the value.
	 */
	public void setParallelLifecycleManagement(boolean parallelLifecycleManagement) {
		this.isParallelLifecycleManagement = parallelLifecycleManagement;
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			logger.debug("Starting registry {}", this);
			this.running = true;
			if (this.isParallelLifecycleManagement) {
				this.listenerContainers.parallelStream().forEach(MessageListenerContainer::start);
			}
			else {
				this.listenerContainers.forEach(MessageListenerContainer::start);
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			logger.debug("Stopping registry {}", this);
			this.running = false;
			if (this.isParallelLifecycleManagement) {
				this.listenerContainers.parallelStream().forEach(MessageListenerContainer::stop);
			}
			else {
				this.listenerContainers.forEach(MessageListenerContainer::stop);
			}
		}
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
