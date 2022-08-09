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

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *
 * Utility methods, including some adapted from other projects.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class CompletableFutures {

	private CompletableFutures() {
	}

	/**
	 * Create an exceptionally completed {@link CompletableFuture}.
	 * @param t the throwable.
	 * @param <T> the future type.
	 * @return the completable future instance.
	 */
	public static <T> CompletableFuture<T> failedFuture(Throwable t) {
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeExceptionally(t);
		return future;
	}

	/**
	 * Compose the provided future with a function that returns another completable future that is executed
	 * exceptionally.
	 * @param future the future to compose with.
	 * @param composingFunction the function for handling the exception.
	 * @param <T> the future type.
	 * @return the completable future.
	 */
	// @formatter:off
	public static <T> CompletableFuture<T> exceptionallyCompose(CompletableFuture<T> future,
			Function<Throwable, ? extends CompletableFuture<T>> composingFunction) {
		return future.thenApply(CompletableFuture::completedFuture)
			.exceptionally(composingFunction)
			.thenCompose(Function.identity());
	}
	// @formatter:on

	/**
	 * Compose the provided future with a function to handle the result, taking a value, a throwable and providing a
	 * completable future as a result.
	 * @param future the future to compose with.
	 * @param composingFunction the composing function.
	 * @param <T> the future type.
	 * @param <U> the result type of the composing function.
	 * @return the completable future.
	 */
	public static <T, U> CompletableFuture<U> handleCompose(CompletableFuture<T> future,
			BiFunction<? super T, Throwable, ? extends CompletableFuture<U>> composingFunction) {
		return future.handle(composingFunction).thenCompose(Function.identity());
	}

}
