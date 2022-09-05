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
package io.awspring.cloud.sns.sms.core;

import io.awspring.cloud.sns.sms.attributes.SmsMessageAttributes;
import org.springframework.lang.Nullable;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Helper class that simplifies synchronous sending of SMS to SNS topic or Phone Number. The only mandatory field is
 * {@link SnsClient}.
 * @author Matej Nedic
 * @since 3.0.0
 */
public class SnsSmsTemplate implements SnsSmsOperations {

	private final SnsClient snsClient;

	public SnsSmsTemplate(SnsClient snsClient) {
		this.snsClient = snsClient;
	}

	@Override
	public void send(String phoneNumber, String message, @Nullable SmsMessageAttributes attributes) {
		PublishRequest.Builder publishRequest = PublishRequest.builder().phoneNumber(phoneNumber).message(message);
		if (attributes != null) {
			publishRequest.messageAttributes(attributes.convertAndPopulate());
		}
		this.snsClient.publish(publishRequest.build());
	}

	@Override
	public void sendToTopicArn(String topicArn, String message, @Nullable SmsMessageAttributes attributes) {
		PublishRequest.Builder publishRequest = PublishRequest.builder().topicArn(topicArn).message(message);
		if (attributes != null) {
			publishRequest.messageAttributes(attributes.convertAndPopulate());
		}
		this.snsClient.publish(publishRequest.build());
	}

}
