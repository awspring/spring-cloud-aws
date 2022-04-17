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

import org.springframework.messaging.Message;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * SNS specific headers that can be applied to Spring Messaging {@link Message}.
 *
 * @author Matej Nedic
 * @since 3.0.0
 */
public final class SnsHeaders {

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

	private SnsHeaders() {

	}

}
