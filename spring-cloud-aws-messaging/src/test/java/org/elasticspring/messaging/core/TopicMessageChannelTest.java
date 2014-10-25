/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.core;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * @author Alain Sahli
 */
public class TopicMessageChannelTest {

	@Test
	public void sendMessage_validTextMessageAndSubject_returnsTrue() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);

		Message<String> stringMessage = MessageBuilder.withPayload("Message content").setHeader(TopicMessageChannel.NOTIFICATION_SUBJECT_HEADER, "Subject").build();
		MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(amazonSns, only()).publish(new PublishRequest("topicArn",
				"Message content", "Subject"));
		assertTrue(sent);
	}

	@Test
	public void sendMessage_validTextMessageWithoutSubject_returnsTrue() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);

		Message<String> stringMessage = MessageBuilder.withPayload("Message content").build();
		MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

		// Act
		boolean sent = messageChannel.send(stringMessage);

		// Assert
		verify(amazonSns, only()).publish(new PublishRequest("topicArn",
				"Message content", null));
		assertTrue(sent);
	}

	@Test
	public void sendMessage_validTextMessageAndTimeout_timeoutIsIgnored() throws Exception {
		// Arrange
		AmazonSNS amazonSns = mock(AmazonSNS.class);

		Message<String> stringMessage = MessageBuilder.withPayload("Message content").build();
		MessageChannel messageChannel = new TopicMessageChannel(amazonSns, "topicArn");

		// Act
		boolean sent = messageChannel.send(stringMessage, 10);

		// Assert
		verify(amazonSns, only()).publish(new PublishRequest("topicArn",
				"Message content", null));
		assertTrue(sent);
	}
}
