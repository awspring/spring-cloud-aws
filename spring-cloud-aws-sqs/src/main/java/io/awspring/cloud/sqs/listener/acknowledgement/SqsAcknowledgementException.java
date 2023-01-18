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

import io.awspring.cloud.sqs.SqsException;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * {@link RuntimeException} that wraps an error thrown during acknowledgement execution.
 *
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsAcknowledgementException extends SqsException {

	private final String queueUrl;

	private final Collection<Message<?>> failedAcknowledgementMessages;

	private final Collection<Message<?>> successfullyAcknowledgedMessages;

	/**
	 * Contruct an instance with the given parameters.
	 * @param errorMessage the error message.
	 * @param failedAcknowledgementMessages the messages that failed to be acknowledged.
	 * @param queueUrl the url for the queue from which the messages were polled from.
	 * @param <T> the messages payload type.
	 */
	public <T> SqsAcknowledgementException(String errorMessage, Collection<Message<T>> successfullyAcknowledgedMessages,
										   Collection<Message<T>> failedAcknowledgementMessages, String queueUrl) {
		this(errorMessage, successfullyAcknowledgedMessages, failedAcknowledgementMessages, queueUrl, null);
	}

	/**
	 * Contruct an instance with the given parameters.
	 * @param errorMessage the error message.
	 * @param failedAcknowledgementMessages the messages that failed to be acknowledged.
	 * @param queueUrl the url for the queue from which the messages were polled from.
	 * @param cause the exception cause.
	 * @param <T> the messages payload type.
	 */
	public <T> SqsAcknowledgementException(String errorMessage, Collection<Message<T>> successfullyAcknowledgedMessages,
										   Collection<Message<T>> failedAcknowledgementMessages, String queueUrl, @Nullable Throwable cause) {
		super(errorMessage, cause);
		this.queueUrl = queueUrl;
		this.failedAcknowledgementMessages = failedAcknowledgementMessages.stream().map(msg -> (Message<?>) msg)
				.collect(Collectors.toList());
		this.successfullyAcknowledgedMessages = successfullyAcknowledgedMessages.stream().map(msg -> (Message<?>) msg)
			.collect(Collectors.toList());
	}

	/**
	 * Return the messages that failed to be acknowledged.
	 * @return the messages.
	 */
	public Collection<Message<?>> getFailedAcknowledgementMessages() {
		return this.failedAcknowledgementMessages;
	}

	/**
	 * Return the messages that failed to be acknowledged.
	 * @return the messages.
	 */
	public Collection<Message<?>> getSuccessfullyAcknowledgedMessages() {
		return this.successfullyAcknowledgedMessages;
	}

	/**
	 * Return the url for the queue from which the messages were polled from.
	 * @return the queue url.
	 */
	public String getQueueUrl() {
		return this.queueUrl;
	}

}
