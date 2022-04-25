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

import org.springframework.messaging.Message;

/**
 * SNS specific headers for SMS that can be applied to Spring Messaging {@link Message}. For all Message Attributes
 * please check: https://docs.aws.amazon.com/sns/latest/dg/sns-message-attributes.html#sns-attrib-mobile-reserved
 *
 * @author Matej Nedic
 * @since 3.0.0
 */
public class SnsSmsHeaders {

	/**
	 * Type of SMS which will be used when sending SMS. Currently AWS supports: - Promotional default Marketing and so
	 * on. (Lower cost) - Transactional Highest reliability used in for example: One time passcode, MFA auth and so on.
	 */
	public static final String DEFAULT_SMS_TYPE = "DefaultSMSType";

	/**
	 * Name that will be displayed to sender
	 */
	public static final String DEFAULT_SENDER_ID = "DefaultSenderID";

}
