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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Utility methods to handle {@link SmartLifecycle} hooks.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class LifecycleHandler {

	private static final LifecycleHandler INSTANCE = new LifecycleHandler();

	public static LifecycleHandler get() {
		return INSTANCE;
	}

	private final SimpleAsyncTaskExecutor executor;

	private boolean parallelLifecycle = true;

	private LifecycleHandler() {
		executor = new SimpleAsyncTaskExecutor();
		executor.setThreadNamePrefix("lifecycle-thread-");
	}

	public void setParallelLifecycle(boolean parallelLifecycle) {
		this.parallelLifecycle = parallelLifecycle;
	}

	/**
	 * Execute the provided action if the provided objects are {@link SmartLifecycle} instances.
	 * @param action the action.
	 * @param objects the objects.
	 */
	public void manageLifecycle(Consumer<SmartLifecycle> action, Object... objects) {
		Arrays.stream(objects).forEach(object -> {
			if (object instanceof SmartLifecycle) {
				action.accept((SmartLifecycle) object);
			}
			else if (object instanceof Collection) {
				if (this.parallelLifecycle) {
					CompletableFuture.allOf(((Collection<?>) object).stream()
							.map(obj -> CompletableFuture.runAsync(() -> manageLifecycle(action, obj), this.executor))
							.toArray(CompletableFuture[]::new)).join();
				}
				else {
					((Collection<?>) object).forEach(obj -> manageLifecycle(action, obj));
				}
			}
		});
	}

	/**
	 * Starts the provided objects that are a {@link SmartLifecycle} instance.
	 * @param objects the objects.
	 */
	public void start(Object... objects) {
		manageLifecycle(SmartLifecycle::start, objects);
	}

	/**
	 * Starts the provided objects that are a {@link SmartLifecycle} instance.
	 * @param objects the objects.
	 */
	public void stop(Object... objects) {
		manageLifecycle(SmartLifecycle::stop, objects);
	}

	public boolean isRunning(Object object) {
		if (object instanceof SmartLifecycle) {
			return ((SmartLifecycle) object).isRunning();
		}
		return true;
	}

	public void dispose(Object destroyable) {
		if (destroyable instanceof DisposableBean) {
			try {
				((DisposableBean) destroyable).destroy();
			}
			catch (Exception e) {
				throw new IllegalStateException("Error destroying disposable " + destroyable);
			}
		}
	}

}
