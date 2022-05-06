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
package io.awspring.cloud.messaging.support.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class DefaultListenerContainerRegistry implements MessageListenerContainerRegistry {

	private static final Logger logger = LoggerFactory.getLogger(DefaultListenerContainerRegistry.class);

	private final List<MessageListenerContainer<?>> listenerContainers = new ArrayList<>();

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running = false;

	@Override
	public void registerListenerContainer(MessageListenerContainer<?> listenerContainer) {
		logger.debug("Registering listener container {}", listenerContainer);
		this.listenerContainers.add(listenerContainer);
	}

	@Override
	public Collection<MessageListenerContainer<?>> retrieveListenerContainers() {
		return Collections.unmodifiableList(this.listenerContainers);
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			logger.debug("Starting registry {}", this);
			this.running = true;
			this.listenerContainers.forEach(MessageListenerContainer::start);
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			logger.debug("Stopping registry {}", this);
			this.running = false;
			this.listenerContainers.forEach(MessageListenerContainer::stop);
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
