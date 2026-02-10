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
package io.awspring.cloud.sns.core.batch;

import java.util.Collection;

/**
 * Result of a batch publish operation to SNS.
 * <p>
 * Contains both successful results and errors from the batch operation. Use {@link #hasErrors()} or
 * {@link #isFullySuccessful()} to check the overall status of the batch operation.
 *
 * @param results Collection of successful message results
 * @param errors Collection of failed message errors
 * @author Matej Nedic
 * @since 4.1.0
 */
public record BatchResult(Collection<SnsResult> results, Collection<SnsError> errors) {

	/**
	 * Represents a successfully published message in a batch operation.
	 *
	 * @param messageId The unique identifier assigned by SNS to the published message
	 * @param batchId The identifier used in the batch request to correlate the result
	 * @param sequenceNumber Sequence number of message sent. Only for FIFO.
	 */
	public record SnsResult(String messageId, String batchId, String sequenceNumber){}
	
	/**
	 * Represents a failed message in a batch operation.
	 *
	 * @param batchId The identifier used in the batch request to correlate the error
	 * @param code The error code indicating the type of failure
	 * @param message A human-readable description of the error
	 * @param senderFault Indicates whether the error was caused by the sender (true) or the service (false)
	 */
	public record SnsError(String batchId, String code, String message, boolean senderFault){}
	
	/**
	 * Checks if any messages in the batch failed to publish.
	 *
	 * @return true if there are any errors, false otherwise
	 */
	public boolean hasErrors() {
		return !errors.isEmpty();
	}
	
	/**
	 * Checks if all messages in the batch were successfully published.
	 *
	 * @return true if there are no errors, false otherwise
	 */
	public boolean isFullySuccessful() {
		return errors.isEmpty();
	}
}
