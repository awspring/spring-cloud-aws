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
package io.awspring.cloud.sqs.config;

import io.awspring.cloud.sqs.listener.MessageListenerContainer;

/**
 * Creates {@link MessageListenerContainer} instances for given {@link Endpoint} instances or endpoint names.
 *
 * @param <C> the {@link MessageListenerContainer} type.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
@FunctionalInterface
public interface MessageListenerContainerFactory<C extends MessageListenerContainer<?>> {

	/**
	 * Create a container instance for the given endpoint names.
	 * @param logicalEndpointNames the names.
	 * @return the container instance.
	 */
	C createContainer(String... logicalEndpointNames);

	/**
	 * Create a container instance for the given {@link Endpoint}.
	 * @param endpoint the endpoint.
	 * @return the container instance.
	 */
	default C createContainer(Endpoint endpoint) {
		throw new UnsupportedOperationException("This factory is not capable of processing Endpoint instances.");
	}

}
