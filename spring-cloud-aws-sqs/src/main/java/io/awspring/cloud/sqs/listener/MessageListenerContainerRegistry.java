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

import java.util.Collection;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;

/**
 * Interface for registering and looking up containers at startup and runtime.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see DefaultListenerContainerRegistry
 */
public interface MessageListenerContainerRegistry extends SmartLifecycle {

	/**
	 * Register a {@link MessageListenerContainer} instance with this registry.
	 * @param listenerContainer the instance.
	 */
	void registerListenerContainer(MessageListenerContainer<?> listenerContainer);

	/**
	 * Return the {@link MessageListenerContainer} instances registered within this registry.
	 * @return the container instances.
	 */
	Collection<MessageListenerContainer<?>> getListenerContainers();

	/**
	 * Return the {@link MessageListenerContainer} instance registered within this registry with the provided id, or
	 * null if none.
	 * @param id the id.
	 * @return the container instance.
	 */
	@Nullable
	MessageListenerContainer<?> getContainerById(String id);

}
