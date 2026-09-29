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
 * @since 4.2.0
 */
public final class KclBeanNames {

	private KclBeanNames() {
	}

	public static final String CONTAINER_REGISTRY_BEAN_NAME = "io.awspring.cloud.kinesis.internalContainerRegistry";

	public static final String KCL_LISTENER_ANNOTATION_BEAN_POST_PROCESSOR_BEAN_NAME = "io.awspring.cloud.kinesis.internalKclListenerAnnotationBeanPostProcessor";

}
