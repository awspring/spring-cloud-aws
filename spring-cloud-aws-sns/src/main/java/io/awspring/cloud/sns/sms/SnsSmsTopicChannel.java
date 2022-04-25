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

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import io.awspring.cloud.sns.core.HeaderConverter;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.AbstractMessageChannel;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Implementation of {@link AbstractMessageChannel} which is used for converting and sending SMS messages to phone
 * numbers or Platforms GCM and APNS {@link SnsClient} to SNS.
 *
 * @author Matej Nedic
 * @since 3.0
 */
public class SnsSmsTopicChannel extends AbstractMessageChannel {

	@Nullable
	private final String targetArn;
	@Nullable
	private final String phoneNumber;
	private HeaderConverter headerConverter = new HeaderConverter(JsonStringEncoder.getInstance(), this.logger);
	private final SnsClient snsClient;

	public SnsSmsTopicChannel(SnsClient snsClient, String targetArn, String phoneNumber) {
		this.targetArn = targetArn;
		this.phoneNumber = phoneNumber;
		this.snsClient = snsClient;
	}

	@Override
	protected boolean sendInternal(Message<?> message, long timeout) {
		PublishRequest.Builder publishRequestBuilder = PublishRequest.builder()
				.message(message.getPayload().toString());
		if (phoneNumber != null) {
			publishRequestBuilder.phoneNumber(phoneNumber);
		}
		else {
			publishRequestBuilder.targetArn(targetArn);
		}

		Map<String, MessageAttributeValue> messageAttributes = headerConverter.toSnsMessageAttributes(message);
		if (!messageAttributes.isEmpty()) {
			publishRequestBuilder.messageAttributes(messageAttributes);
		}

		this.snsClient.publish(publishRequestBuilder.build());
		return true;
	}
}
