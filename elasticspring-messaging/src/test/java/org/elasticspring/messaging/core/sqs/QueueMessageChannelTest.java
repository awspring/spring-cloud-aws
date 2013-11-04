/*
 * Copyright 2013 the original author or authors.
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

package org.elasticspring.messaging.core.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class QueueMessageChannelTest {

	@Test
	public void testSendMessage() throws Exception {
		// Arrange
		AmazonSQS amazonSqs = Mockito.mock(AmazonSQS.class);

		Message<String> stringMessage = MessageBuilder.withPayload("message content").build();
		MessageChannel messageChannel = new QueueMessageChannel(amazonSqs, "http://testQueue");


		// Act
		messageChannel.send(stringMessage);

		// Assert
		Mockito.verify(amazonSqs, Mockito.only()).
				sendMessage(new SendMessageRequest("http://testQueue", "message content").withDelaySeconds(0));

	}
}
