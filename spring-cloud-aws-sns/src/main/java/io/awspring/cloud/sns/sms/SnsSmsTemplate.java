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

import static io.awspring.cloud.sns.core.MessageConverters.initMessageConverter;

import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.DestinationResolvingMessageSendingOperations;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Helper class that simplifies synchronous sending of sms messages to Mobile Phones or Platforms such as GCM and APNS.
 * The only mandatory fields are {@link SnsClient}.
 *
 * @author Matej Nedic
 * @since 3.0
 */
public class SnsSmsTemplate extends AbstractMessageSendingTemplate<SnsSmsTopicChannel>
		implements DestinationResolvingMessageSendingOperations<SnsSmsTopicChannel> {

	private final SnsClient snsClient;

	public SnsSmsTemplate(SnsClient snsClient, @Nullable MessageConverter messageConverter) {
		Assert.notNull(snsClient, "SnsClient must not be null");
		this.snsClient = snsClient;

		if (messageConverter != null) {
			this.setMessageConverter(initMessageConverter(messageConverter));
		}
	}

	@Override
	public void send(String destination, Message<?> message) throws MessagingException {
		doSend(resolveMessageChannelByTopicName(destination), message);
	}

	@Override
	public <T> void convertAndSend(String destination, T payload) throws MessagingException {
		this.convertAndSend(destination, payload, null, null);
	}

	@Override
	public <T> void convertAndSend(String destination, T payload, @Nullable Map<String, Object> headers)
			throws MessagingException {
		this.convertAndSend(destination, payload, headers, null);
	}

	@Override
	public <T> void convertAndSend(String destination, T payload, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException {
		this.convertAndSend(destination, payload, null, postProcessor);
	}

	@Override
	public <T> void convertAndSend(String destination, T payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException {
		convertAndSend(resolveMessageChannelByTopicName(destination), payload, headers, postProcessor);
	}

	/**
	 * Helper method for sending SMS to phone number or PlatformApplication.
	 *
	 * @param destination Number phone or Target Arn of PlatformApplication.
	 * @param notification Notification that contains message and MessageAttributes to be sent.
	 */
	public void sendNotification(String destination, SnsSmsNotification<?> notification) {
		this.convertAndSend(destination, notification.getPayload(), notification.getHeaders());
	}

	@Override
	protected void doSend(SnsSmsTopicChannel destination, Message<?> message) {
		destination.send(message);
	}

	private SnsSmsTopicChannel resolveMessageChannelByTopicName(String topicName) {
		if (topicName.startsWith("+")) {
			return new SnsSmsTopicChannel(this.snsClient, null, topicName);
		}
		return new SnsSmsTopicChannel(this.snsClient, topicName, null);
	}

}
