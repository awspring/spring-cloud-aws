/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.core.task;

import java.util.Collections;
import java.util.List;

import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;

/**
 * Suppressing {@link java.util.concurrent.ExecutorService} implementation that ignores
 * {@link #shutdownNow()} calls which are made by the Amazon Webservice clients. If these
 * clients receive an externally managed
 * {@link org.springframework.core.task.TaskExecutor} this implementation suppresses the
 * calls to avoid exception during application shutdown.
 *
 * @author Agim Emruli
 * @since 1.0
 */
public class ShutdownSuppressingExecutorServiceAdapter extends ExecutorServiceAdapter {

	/**
	 * Create a new SuppressingExecutorServiceAdapter, using the given target executor.
	 * @param taskExecutor the target executor to delegate to, typically an externally
	 * managed one
	 */
	public ShutdownSuppressingExecutorServiceAdapter(TaskExecutor taskExecutor) {
		super(taskExecutor);
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public List<Runnable> shutdownNow() {
		return Collections.emptyList();
	}

}
