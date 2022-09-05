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
package io.awspring.cloud.sqs.listener.acknowledgement;

import java.util.Collection;
import org.springframework.messaging.Message;

/**
 * Provides actions to be executed after a message acknowledgement completes with either success or failure.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see AbstractOrderingAcknowledgementProcessor
 */
public interface AcknowledgementResultCallback<T> {

	/**
	 * Execute an action after the messages are successfully acknowledged.
	 * @param messages the messages.
	 */
	default void onSuccess(Collection<Message<T>> messages) {
	}

	/**
	 * Execute an action if message acknowledgement fails.
	 * @param messages the messages.
	 * @param t the error thrown by the acknowledgement.
	 */
	default void onFailure(Collection<Message<T>> messages, Throwable t) {
	}

}
