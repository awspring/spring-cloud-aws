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
 * @author Matej Nedic
 * @since 3.0.0
 */
public interface SnsSmsOperations {
	void send(String phoneNumber, String message, @Nullable SmsMessageAttributes attributes);

	default void send(String phoneNumber, String message) {
		send(phoneNumber, message, null);
	}

	void sendToTopicArn(String topicArn, String message, @Nullable SmsMessageAttributes attributes);

	default void sendToTopicArn(String topicArn, String message) {
		sendToTopicArn(topicArn, message, null);
	}
}
