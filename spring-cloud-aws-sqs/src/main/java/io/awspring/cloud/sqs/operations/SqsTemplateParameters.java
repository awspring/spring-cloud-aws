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

/**
 * SQS parameters added to {@link SendResult} objects as additional information. These are returned by the SQS endpoints
 * and stored as returned. See the AWS documentation for more information.
 *
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsTemplateParameters {

	/**
	 * Sequence number generated for SQS FIFO.
	 */
	public static final String SEQUENCE_NUMBER_PARAMETER_NAME = "sequenceNumber";

	/**
	 * Whether the messaging operation failed due to a problem with the request.
	 */
	public static final String SENDER_FAULT_PARAMETER_NAME = "senderFault";

	/**
	 * A code representing the error.
	 */
	public static final String ERROR_CODE_PARAMETER_NAME = "code";

	/**
	 * The raw provider message ID when it is not a valid UUID.
	 */
	public static final String RAW_MESSAGE_ID_PARAMETER_NAME = "rawMessageId";

}
