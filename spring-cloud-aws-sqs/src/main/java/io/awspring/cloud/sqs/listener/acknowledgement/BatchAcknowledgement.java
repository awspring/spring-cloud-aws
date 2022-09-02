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
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * Enables acknowledging messages for {@link io.awspring.cloud.sqs.listener.ListenerMode#BATCH}. Either the entire batch
 * or a partial batch can be acknowledged.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 * @see io.awspring.cloud.sqs.listener.ListenerMode
 */
public interface BatchAcknowledgement<T> {

	/**
	 * Acknowledge all messages from the batch.
	 */
	void acknowledge();

	/**
	 * Asynchronously acknowledge all messages from the batch.
	 */
	CompletableFuture<Void> acknowledgeAsync();

	/**
	 * Acknowledge the provided messages.
	 */
	void acknowledge(Collection<Message<T>> messagesToAcknowledge);

	/**
	 * Asynchronously acknowledge the provided messages.
	 */
	CompletableFuture<Void> acknowledgeAsync(Collection<Message<T>> messagesToAcknowledge);

}
