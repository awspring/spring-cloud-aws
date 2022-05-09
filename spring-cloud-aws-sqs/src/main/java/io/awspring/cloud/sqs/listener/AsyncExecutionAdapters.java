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

import io.awspring.cloud.sqs.CompletableFutures;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Utility class for adapting blocking processes to asynchronous components,
 * including error handling for the method calls.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class AsyncExecutionAdapters {

	private AsyncExecutionAdapters() {
	}

	/**
	 * Executes the provided blocking process and returns a void completed future.
	 * @param blockingProcess the blocking process.
	 * @return the completed future.
	 */
	public static CompletableFuture<Void> adaptFromBlocking(Runnable blockingProcess) {
		try {
			blockingProcess.run();
			return CompletableFuture.completedFuture(null);
		}
		catch (Exception e) {
			return CompletableFutures.failedFuture(e);
		}
	}

	/**
	 * Executes the provided blocking process and returns a completed future with the process' result.
	 * @param blockingProcess the blocking process.
	 * @return the completed future with the process' result.
	 */
	public static <T> CompletableFuture<T> adaptFromBlocking(Supplier<T> blockingProcess) {
		try {
			return CompletableFuture.completedFuture(blockingProcess.get());
		}
		catch (Exception e) {
			return CompletableFutures.failedFuture(e);
		}
	}
}
