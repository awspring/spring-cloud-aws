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
import java.util.Collection;
import org.springframework.lang.Nullable;

/**
 * Represents a messaging endpoint from which messages can be consumed by a {@link MessageListenerContainer}.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface Endpoint {

	/**
	 * The logical names for this endpoint.
	 * @return the logical names.
	 */
	Collection<String> getLogicalNames();

	/**
	 * The name of the factory bean that will process this endpoint.
	 * @return the factory bean name.
	 */
	String getListenerContainerFactoryName();

	/**
	 * An optional id for this endpoint.
	 * @return the endpoint id.
	 */
	@Nullable
	String getId();

	/**
	 * Set up the necessary attributes for the container to process this endpoint.
	 * @param container the container to be configured.
	 */
	void setupContainer(MessageListenerContainer<?> container);

}
