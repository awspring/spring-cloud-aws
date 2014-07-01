/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.support;

import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;

import java.util.Collections;
import java.util.List;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class SuppressingExecutorServiceAdapter extends ExecutorServiceAdapter {

	/**
	 * Create a new SuppressingExecutorServiceAdapter, using the given target executor.
	 *
	 * @param taskExecutor
	 * 		the target executor to delegate to
	 */
	public SuppressingExecutorServiceAdapter(TaskExecutor taskExecutor) {
		super(taskExecutor);
	}

	@Override
	public List<Runnable> shutdownNow() {
		return Collections.emptyList();
	}
}
