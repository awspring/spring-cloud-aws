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

import java.util.Map;

/**
 * Simple Interface that specifies what should Template for sending SNS SMS messages support.
 *
 * @author Matej Nedic
 */
public interface SnsSmsOperations {

	/**
	 * Sends message just with payload.
	 *
	 * @param destination can be Phone number or PlatformApplicationArn. Used to determine where to send message.
	 * @param payload message that you want to send.
	 */
	void sendMessage(String destination, Object payload);

	/**
	 * Sends message with payload and settings for sending. Example {@link SnsSmsHeaders} defaultSenderId.
	 *
	 * @param destination can be Phone number or PlatformApplicationArn. Used to determine where to send message.
	 * @param payload message that you want to send.
	 * @param headers will be transformed to {@link software.amazon.awssdk.services.sns.model.MessageAttributeValue} Map
	 *     which is used to set specific settings for sending SMS.
	 */
	void sendMessage(String destination, Object payload, Map<String, Object> headers);
}
