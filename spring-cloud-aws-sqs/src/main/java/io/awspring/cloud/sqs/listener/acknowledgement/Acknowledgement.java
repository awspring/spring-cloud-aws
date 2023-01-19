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

import io.awspring.cloud.sqs.MessageHeaderUtils;
import io.awspring.cloud.sqs.MessagingHeaders;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.springframework.messaging.Message;

/**
 * Interface representing a message acknowledgement. For this interface to be used as a listener method parameter,
 * {@link io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode#MANUAL} has to be set.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public interface Acknowledgement {

	/**
	 * Acknowledge the message.
	 */
	void acknowledge();

	/**
	 * Asynchronously acknowledge the message.
	 */
	CompletableFuture<Void> acknowledgeAsync();

	/**
	 * Acknowledge the provided message.
	 * @param message the message.
	 */
	static void acknowledge(Message<?> message) {
		acknowledgeAsync(message).join();
	}

	/**
	 * Acknowledge the provided message asynchronously.
	 * @param message the message.
	 */
	@SuppressWarnings("unchecked")
	static CompletableFuture<Void> acknowledgeAsync(Message<?> message) {
		return MessageHeaderUtils
				.getHeader(message, MessagingHeaders.ACKNOWLEDGMENT_CALLBACK_HEADER, AcknowledgementCallback.class)
				.onAcknowledge(message);
	}

	/**
	 * Acknowledge the provided messages.
	 * @param messages the messages.
	 */
	static <T> void acknowledge(Collection<Message<T>> messages) {
		acknowledgeAsync(messages).join();
	}

	/**
	 * Acknowledge the provided messages asynchronously.
	 * @param messages the messages.
	 */
	@SuppressWarnings("unchecked")
	static <T> CompletableFuture<Void> acknowledgeAsync(Collection<Message<T>> messages) {
		return !messages.isEmpty() ? MessageHeaderUtils.getHeader(messages.iterator().next(),
				MessagingHeaders.ACKNOWLEDGMENT_CALLBACK_HEADER, AcknowledgementCallback.class).onAcknowledge(messages)
				: CompletableFuture.completedFuture(null);
	}

}
