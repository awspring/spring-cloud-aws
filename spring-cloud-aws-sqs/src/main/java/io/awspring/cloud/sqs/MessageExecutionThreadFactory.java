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
package io.awspring.cloud.sqs;

import io.awspring.cloud.sqs.listener.ContainerOptions;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * {@link CustomizableThreadFactory} implementation for creating {@link MessageExecutionThread} instances. This should
 * be set to {@link org.springframework.core.task.TaskExecutor} instances provided by
 * {@link ContainerOptions#getComponentsTaskExecutor()} to avoid excessive thread hopping for blocking components.
 * @author Tomaz Fernandes
 * @since 3.0
 * @see MessageExecutionThread
 * @see io.awspring.cloud.sqs.listener.AsyncComponentAdapters
 */
public class MessageExecutionThreadFactory extends CustomizableThreadFactory {

	@Override
	public Thread createThread(Runnable runnable) {
		MessageExecutionThread thread = new MessageExecutionThread(getThreadGroup(), runnable, nextThreadName());
		thread.setDaemon(false);
		thread.setPriority(Thread.NORM_PRIORITY);
		return thread;
	}

}
