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
package io.awspring.cloud.sqs.listener;

import java.util.concurrent.CompletableFuture;

/**
 * Visibility interface that can be injected as parameter into a listener method. The purpose of this interface is to
 * provide a way for the listener methods to extend the visibility timeout of the message being currently processed.
 *
 * @author Szymon Dembek
 * @author Tomaz Fernandes
 * @since 1.3
 */
public interface Visibility {

	/**
	 * The maximum visibility timeout interval, which corresponds to the maximum SQS visibility timeout of 12 hours.
	 */
	int MAX_VISIBILITY_TIMEOUT_SECONDS = 43200;

	/**
	 * Asynchronously changes the message visibility to the provided value.
	 * @param seconds number of seconds to set the visibility of the message to.
	 * @return a completable future.
	 */
	CompletableFuture<Void> changeToAsync(int seconds);

	/**
	 * Changes the message visibility to the provided value.
	 * @param seconds number of seconds to set the visibility of the message to.
	 */
	default void changeTo(int seconds) {
		changeToAsync(seconds).join();
	}

}
