/*
 * Copyright 2013-2020 the original author or authors.
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
 * @author Tomaz Fernandes
 * @since 3.0
 */
public class SqsHeaders {

	private SqsHeaders() {
	}

	/**
	 * SQS Headers prefix
	 */
	public static final String SQS_HEADER_PREFIX = "Sqs_";

	/**
	 * MessageAttributes prefix
	 */
	public static final String SQS_MA_HEADER_PREFIX = SQS_HEADER_PREFIX + "MA_";

	public static final String SQS_QUEUE_NAME_HEADER = SQS_HEADER_PREFIX + "QueueName";

	public static final String SQS_QUEUE_URL_HEADER = SQS_HEADER_PREFIX + "QueueUrl";

	public static final String SQS_RECEIPT_HANDLE_HEADER = SQS_HEADER_PREFIX + "ReceiptHandle";

	public static final String SQS_MESSAGE_ID_HEADER = SQS_HEADER_PREFIX + "MessageId";

	public static final String SQS_SOURCE_DATA_HEADER = SQS_HEADER_PREFIX + "SourceData";

	public static final String SQS_VISIBILITY_HEADER = SQS_HEADER_PREFIX + "Visibility";

	public static final String SQS_RECEIVED_AT_HEADER = SQS_HEADER_PREFIX + "ReceivedAt";

	public static final String SQS_ACKNOWLEDGMENT_HEADER = SQS_HEADER_PREFIX + "Acknowledgement";

	public static final String SQS_QUEUE_ATTRIBUTES_HEADER = SQS_HEADER_PREFIX + "QueueAttributes";

	public static final String SQS_DEFAULT_TYPE_HEADER = "JavaType";

	public static class MessageSystemAttribute {

		private MessageSystemAttribute(){
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
		public static final String SQS_APPROXIMATE_FIRST_RECEIVE_TIMESTAMP = SQS_MSA_HEADER_PREFIX + "ApproximateFirstReceiveTimestamp";

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
