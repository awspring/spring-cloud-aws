/*
 * Copyright 2010-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.core.sqs;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.junit.Test;
import org.mockito.Mockito;


/**
 *
 */
public class SimpleQueueServiceMessageTemplateTest {

	@Test
	public void testConvertAndSendSingleMessage() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueServiceMessageTemplate messageTemplate = new SimpleQueueServiceMessageTemplate(amazonSQS, "test");
		Mockito.when(amazonSQS.createQueue(new CreateQueueRequest("test"))).thenReturn(new CreateQueueResult().withQueueUrl("http://testQueue"));
		Mockito.when(amazonSQS.sendMessage(new SendMessageRequest("http://testQueue", "message"))).thenReturn(new SendMessageResult().withMessageId("123"));

		messageTemplate.convertAndSend("message");

	}

	@Test
	public void testConvertAndSendWithCustomDestination() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueServiceMessageTemplate messageTemplate = new SimpleQueueServiceMessageTemplate(amazonSQS, "test");
		Mockito.when(amazonSQS.createQueue(new CreateQueueRequest("custom"))).thenReturn(new CreateQueueResult().withQueueUrl("http://customQueue"));
		Mockito.when(amazonSQS.sendMessage(new SendMessageRequest("http://customQueue", "message"))).thenReturn(new SendMessageResult().withMessageId("123"));

		messageTemplate.convertAndSend("custom", "message");

	}

	@Test
	public void testReceiveAndConvert() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueServiceMessageTemplate messageTemplate = new SimpleQueueServiceMessageTemplate(amazonSQS, "test");
		Mockito.when(amazonSQS.createQueue(new CreateQueueRequest("test"))).thenReturn(new CreateQueueResult().withQueueUrl("http://testQueue"));
		Message message = new Message().withBody("message").withReceiptHandle("r123");
		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://testQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		messageTemplate.receiveAndConvert();

		Mockito.verify(amazonSQS, Mockito.times(1)).deleteMessage(new DeleteMessageRequest().withQueueUrl("http://testQueue").withReceiptHandle("r123"));
	}

	@Test
	public void testReceiveAndConvertWithCustomDestination() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueServiceMessageTemplate messageTemplate = new SimpleQueueServiceMessageTemplate(amazonSQS, "test");
		Mockito.when(amazonSQS.createQueue(new CreateQueueRequest("custom"))).thenReturn(new CreateQueueResult().withQueueUrl("http://customQueue"));
		Message message = new Message().withBody("message").withReceiptHandle("r123");
		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://customQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		messageTemplate.receiveAndConvert("custom");

		Mockito.verify(amazonSQS, Mockito.times(1)).deleteMessage(new DeleteMessageRequest().withQueueUrl("http://customQueue").withReceiptHandle("r123"));
	}

}
