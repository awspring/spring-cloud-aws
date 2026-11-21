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
package io.awspring.cloud.kinesis.config;

/**
 * @author Matej Nedic
 * @since 4.3.0
 */
public final class KclBeanNames {

	private KclBeanNames() {
	}

	public static final String CONTAINER_REGISTRY_BEAN_NAME = "io.awspring.cloud.kinesis.internalContainerRegistry";

	public static final String KCL_LISTENER_ANNOTATION_BEAN_POST_PROCESSOR_BEAN_NAME = "io.awspring.cloud.kinesis.internalKclListenerAnnotationBeanPostProcessor";

	/**
	 * Name of the {@link MessageListenerContainerFactory} bean used by {@code @KclListener} methods that do not set a
	 * {@code factory}. The auto-configured factory is registered under this name.
	 */
	public static final String DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "defaultKclListenerContainerFactory";

}
