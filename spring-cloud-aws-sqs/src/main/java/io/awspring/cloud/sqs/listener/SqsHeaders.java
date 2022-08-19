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
package io.awspring.cloud.sqs.listener;

/**
 * The {@link org.springframework.messaging.MessageHeaders} names used for {@link org.springframework.messaging.Message}
 * instances created from SQS messages. Can be used to retrieve headers from messages either through
 * {@link org.springframework.messaging.MessageHeaders#get} or
 * {@link org.springframework.messaging.handler.annotation.Header} parameter annotations.
 * @author Tomaz Fernandes
 * @since 3.0
 * @see io.awspring.cloud.sqs.support.converter.SqsHeaderMapper
 */
public class SqsHeaders {

	private SqsHeaders() {
	}

	/**
	 * SQS Headers prefix to be used by all headers added by the framework.
	 */
	public static final String SQS_HEADER_PREFIX = "Sqs_";

	/**
	 * MessageAttributes prefix to be used by all headers mapped from SQS Message Attributes.
	 */
	public static final String SQS_MA_HEADER_PREFIX = SQS_HEADER_PREFIX + "MA_";

	/**
	 * Header for the queue name.
	 */
	public static final String SQS_QUEUE_NAME_HEADER = SQS_HEADER_PREFIX + "QueueName";

	/**
	 * Header for the queue url.
	 */
	public static final String SQS_QUEUE_URL_HEADER = SQS_HEADER_PREFIX + "QueueUrl";

	/**
	 * Header for the SQS Message's receipt handle.
	 */
	public static final String SQS_RECEIPT_HANDLE_HEADER = SQS_HEADER_PREFIX + "ReceiptHandle";

	/**
	 * Header for the SQS Message's id.
	 */
	public static final String SQS_MESSAGE_ID_HEADER = SQS_HEADER_PREFIX + "MessageId";

	/**
	 * Header for the original SQS {@link software.amazon.awssdk.services.sqs.model.Message}.
	 */
	public static final String SQS_SOURCE_DATA_HEADER = SQS_HEADER_PREFIX + "SourceData";

	/**
	 * Header for the {@link Visibility} object for this message.
	 */
	public static final String SQS_VISIBILITY_HEADER = SQS_HEADER_PREFIX + "Visibility";

	/**
	 * Header for the received at attribute.
	 */
	public static final String SQS_RECEIVED_AT_HEADER = SQS_HEADER_PREFIX + "ReceivedAt";

	/**
	 * Header for a {@link io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementCallback} for this message.
	 */
	public static final String SQS_ACKNOWLEDGMENT_CALLBACK_HEADER = SQS_HEADER_PREFIX + "Acknowledgement";

	/**
	 * Header for the {@link QueueAttributes} for this message.
	 */
	public static final String SQS_QUEUE_ATTRIBUTES_HEADER = SQS_HEADER_PREFIX + "QueueAttributes";

	/**
	 * Header containing the FQCN of the {@link Class} that the message's payload should be deserialized to.
	 */
	public static final String SQS_DEFAULT_TYPE_HEADER = "JavaType";

	public static class MessageSystemAttribute {

		private MessageSystemAttribute() {
		}

		/**
		 * MessageSystemAttributes prefix
		 */
		public static final String SQS_MSA_HEADER_PREFIX = SQS_HEADER_PREFIX + "MSA_";

		/**
		 * Group id header in a SQS message.
		 */
		public static final String SQS_MESSAGE_GROUP_ID_HEADER = SQS_MSA_HEADER_PREFIX + "MessageGroupId";

		/**
		 * Deduplication header in a SQS message.
		 */
		public static final String SQS_DEDUPLICATION_ID_HEADER = SQS_MSA_HEADER_PREFIX + "MessageDeduplicationId";

		/**
		 * ApproximateFirstReceiveTimestamp header in a SQS message.
		 */
		public static final String SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP = SQS_MSA_HEADER_PREFIX
				+ "ApproximateFirstReceiveTimestamp";

		/**
		 * ApproximateReceiveCount header in a SQS message.
		 */
		public static final String SQS_APPROXIMATE_RECEIVE_COUNT = SQS_MSA_HEADER_PREFIX + "ApproximateReceiveCount";

		/**
		 * SentTimestamp header in a SQS message.
		 */
		public static final String SQS_SENT_TIMESTAMP = SQS_MSA_HEADER_PREFIX + "SentTimestamp";

		/**
		 * SenderId header in a SQS message.
		 */
		public static final String SQS_SENDER_ID = SQS_MSA_HEADER_PREFIX + "SenderId";

		/**
		 * SenderId header in a SQS message.
		 */
		public static final String SQS_SEQUENCE_NUMBER = SQS_MSA_HEADER_PREFIX + "SequenceNumber";

		/**
		 * SenderId header in a SQS message.
		 */
		public static final String SQS_AWS_TRACE_HEADER = SQS_MSA_HEADER_PREFIX + "AWSTraceHeader";

	}

}
