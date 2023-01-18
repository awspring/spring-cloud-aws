/*
 * Copyright 2013-2023 the original author or authors.
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
package io.awspring.cloud.sqs.operations;

import org.springframework.lang.Nullable;

/**
 * Exception representing a partial or complete failure in sending a batch of messages to an endpoint.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SendBatchOperationFailedException extends MessagingOperationFailedException {

	private final SendResult.Batch<?> sendBatchResult;

	/**
	 * Create an instance with the provided arguments.
	 * @param msg the error message.
	 * @param endpoint the endpoint to which the messages were sent to.
	 * @param sendBatchResult the detailed result of the batch send attempt..
	 */
	public SendBatchOperationFailedException(String msg, String endpoint, SendResult.Batch<?> sendBatchResult) {
		this(msg, endpoint, sendBatchResult, null);
	}

	/**
	 * Create an instance with the provided arguments.
	 * @param msg the error message.
	 * @param endpoint the endpoint to which the messages were sent to.
	 * @param sendBatchResult the detailed result of the send message.
	 * @param cause the exception cause.
	 */
	public SendBatchOperationFailedException(String msg, String endpoint, SendResult.Batch<?> sendBatchResult,
			@Nullable Throwable cause) {
		super(msg, endpoint, sendBatchResult.failed().stream().map(SendResult.Failed::message).toList(), cause);
		this.sendBatchResult = sendBatchResult;
	}

	/**
	 * Get the detailed result of the batch send attempt.
	 * @return the result.
	 */
	public SendResult.Batch<?> getSendBatchResult() {
		return this.sendBatchResult;
	}

	/**
	 * Get the detailed result of the batch send attempt, casting the result to the provided payload type.
	 * @return the result.
	 */
	@SuppressWarnings("unchecked")
	public <T> SendResult.Batch<T> getSendBatchResult(Class<T> payloadClass) {
		return (SendResult.Batch<T>) this.sendBatchResult;
	}

}
