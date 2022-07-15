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
 * @author Matej Nedic
 * @since 3.0.0
 */
public class SnsSmsTemplate implements SnsSmsOperations {

	private final SnsClient snsClient;
	private final MessageAttributeConverter messageAttributeConverter;

	public SnsSmsTemplate(SnsClient snsClient) {
		this(snsClient, new DefaultMessageAttributeConverter());
	}

	public SnsSmsTemplate(SnsClient snsClient, MessageAttributeConverter messageAttributeConverter) {
		this.snsClient = snsClient;
		this.messageAttributeConverter = messageAttributeConverter;
	}

	@Override
	public void send(String phoneNumber, String message) {
		send(phoneNumber, message, null);
	}

	@Override
	public void send(String phoneNumber, String message, @Nullable SmsMessageAttributes attributes) {
		PublishRequest.Builder publishRequest = PublishRequest.builder().phoneNumber(phoneNumber).message(message)
				.messageAttributes(this.messageAttributeConverter.convert(attributes));
		populatePublishRequest(publishRequest, attributes);
		this.snsClient.publish(publishRequest.build());
	}

	@Override
	public void sendToTopicArn(String topicArn, String message) {
		sendToTopicArn(topicArn, message, null);
	}

	@Override
	public void sendToTopicArn(String topicArn, String message, @Nullable SmsMessageAttributes attributes) {
		PublishRequest.Builder publishRequest = PublishRequest.builder().topicArn(topicArn).message(message)
				.messageAttributes(this.messageAttributeConverter.convert(attributes));
		populatePublishRequest(publishRequest, attributes);
		this.snsClient.publish(publishRequest.build());

	}

	@Override
	public void sendToTargetArn(String targetArn, String message) {
		sendToTargetArn(targetArn, message, null);
	}

	@Override
	public void sendToTargetArn(String targetArn, String message, @Nullable SmsMessageAttributes attributes) {
		PublishRequest.Builder publishRequest = PublishRequest.builder().targetArn(targetArn).message(message)
				.messageAttributes(this.messageAttributeConverter.convert(attributes));
		populatePublishRequest(publishRequest, attributes);
		this.snsClient.publish(publishRequest.build());
	}

	private void populatePublishRequest(PublishRequest.Builder builder, SmsMessageAttributes attributes) {
		if (attributes != null) {
			if (attributes.getDeduplicationId() != null) {
				builder.messageDeduplicationId(attributes.getDeduplicationId());
			}
			if (attributes.getMessageStructure() != null) {
				builder.messageStructure(attributes.getMessageStructure());
			}
			if (attributes.getMessageGroupId() != null) {
				builder.messageGroupId(attributes.getMessageGroupId());
			}
		}
	}
}
