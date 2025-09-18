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
package io.awspring.cloud.sns.core;

import io.awspring.cloud.sns.handlers.NotificationStatus;
import io.awspring.cloud.sns.integration.SnsInboundChannelAdapter;
import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * SNS specific headers that can be applied to Spring Messaging {@link Message}.
 *
 * @author Matej Nedic
 * @author Artem Bilan
 * @since 3.0.0
 */
public final class SnsHeaders {

	/**
	 * SNS Headers prefix to be used by all headers added by the framework.
	 */
	public static final String SNS_HEADER_PREFIX = "Sns_";

	/**
	 * Notification subject. The value of this header is set to {@link PublishRequest#subject()}.
	 */
	public static final String NOTIFICATION_SUBJECT_HEADER = "notification-subject";

	/**
	 * Message group id for SNS message (applies only to FIFO topic). The value of this header is set to
	 * {@link PublishRequest#messageGroupId()}}.
	 */
	public static final String MESSAGE_GROUP_ID_HEADER = "message-group-id";

	/**
	 * Message Deduplication id for SNS message. The value of this header is set to
	 * {@link PublishRequest#messageDeduplicationId()}}}.
	 */
	public static final String MESSAGE_DEDUPLICATION_ID_HEADER = "message-deduplication-id";

	/**
	 * Topic ARN header where the message has been published. The value of this header is set from
	 * {@link PublishRequest#topicArn()}.
	 */
	public static final String TOPIC_HEADER = SNS_HEADER_PREFIX + "topicArn";

	/**
	 * Message id header as a unique identifier assigned to the published message, or from a received message. The value
	 * of this header is set from {@link PublishResponse#messageId()}.
	 */
	public static final String MESSAGE_ID_HEADER = SNS_HEADER_PREFIX + "messageId";

	/**
	 * The {@link NotificationStatus} header for manual confirmation on reception. The value of this header is set from
	 * {@link SnsInboundChannelAdapter}.
	 */
	public static final String NOTIFICATION_STATUS_HEADER = SNS_HEADER_PREFIX + "notificationStatus";

	/**
	 * The {@value SNS_MESSAGE_TYPE_HEADER} header for the received SNS message type.
	 */
	public static final String SNS_MESSAGE_TYPE_HEADER = SNS_HEADER_PREFIX + "messageType";

	private SnsHeaders() {

	}

}
