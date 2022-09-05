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
package io.awspring.cloud.sns.sms;

import org.springframework.lang.Nullable;

/**
 * High level SNS operations for sending SMS.
 *
 * Read more in the official AWS documentation:
 *
 * <ul>
 * <li><a href="https://docs.amazonaws.cn/en_us/sns/latest/dg/sns-mobile-phone-number-as-subscriber.html">Mobile text
 * messaging (SMS)</a></li>
 * <li><a href="https://docs.amazonaws.cn/en_us/sns/latest/dg/sms_publish-to-phone.html">Publishing to a mobile
 * phone</a></li>
 * <li><a href="https://docs.amazonaws.cn/en_us/sns/latest/dg/sms_publish-to-topic.html">Publishing to a topic</a></li>
 * </ul>
 *
 * @author Matej Nedic
 * @since 3.0.0
 */
public interface SnsSmsOperations {
	/**
	 * Sends SMS directly to a phone number without a need for a phone number to be subscribed to an SNS topic.
	 *
	 * @param phoneNumber - phone number to receive a message in E.164 format (for example +14155552671)
	 * @param message - content of the SMS
	 * @param attributes - the message attributes
	 */
	void send(String phoneNumber, String message, @Nullable SmsMessageAttributes attributes);

	/**
	 * Sends SMS directly to a phone number without a need for a phone number to be subscribed to an SNS topic.
	 *
	 * @param phoneNumber - phone number to receive a message in E.164 format (for example +14155552671)
	 * @param message - content of the SMS
	 */
	default void send(String phoneNumber, String message) {
		send(phoneNumber, message, null);
	}

	/**
	 * Sends SMS to a topic to which phone numbers are subscribed.
	 *
	 * @param topicArn - the topic ARN
	 * @param message - content of the SMS
	 * @param attributes - the message attributes
	 */
	void sendToTopicArn(String topicArn, String message, @Nullable SmsMessageAttributes attributes);

	/**
	 * Sends SMS to a topic to which phone numbers are subscribed.
	 *
	 * @param topicArn - the topic ARN
	 * @param message - content of the SMS
	 */
	default void sendToTopicArn(String topicArn, String message) {
		sendToTopicArn(topicArn, message, null);
	}
}
