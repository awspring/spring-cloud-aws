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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * BatchVisibility interface that can be injected as parameter into a listener method. The purpose of this interface is
 * to provide a way for the listener methods to extend the visibility timeout of the messages being currently processed.
 *
 * @author Clement Denis
 * @since 3.3
 */
public interface BatchVisibility<T> {

	/**
	 * Asynchronously changes all messages visibility to the provided value. // * @param seconds number of seconds to
	 * set the visibility of all messages to.
	 * @return a completable future.
	 */
	CompletableFuture<Void> changeToAsync(int seconds);

	/**
	 * Changes all messages visibility to the provided value.
	 * @param seconds number of seconds to set the visibility of all messages to.
	 */
	default void changeTo(int seconds) {
		changeToAsync(seconds).join();
	}

	/**
	 * Asynchronously changes the provided messages visibility to the provided value. // * @param seconds number of
	 * seconds to set the visibility of provided messages to.
	 * @return a completable future.
	 */
	CompletableFuture<Void> changeToAsync(Collection<Message<T>> messages, int seconds);

	/**
	 * Changes the provided messages visibility to the provided value.
	 * @param seconds number of seconds to set the visibility of the provided messages to.
	 */
	default void changeTo(Collection<Message<T>> messages, int seconds) {
		changeToAsync(messages, seconds).join();
	}

}
