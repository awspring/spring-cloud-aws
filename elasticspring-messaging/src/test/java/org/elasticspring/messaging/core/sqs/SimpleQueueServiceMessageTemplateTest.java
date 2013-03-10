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
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import org.elasticspring.messaging.core.MessageOperations;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;


/**
 *
 */
public class SimpleQueueServiceMessageTemplateTest {

	@Test
	public void testConvertAndSendSingleMessage() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueServiceMessageTemplate messageTemplate = new SimpleQueueServiceMessageTemplate(amazonSQS);
		messageTemplate.setDefaultDestinationName("test");
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("test"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue"));
		Mockito.when(amazonSQS.sendMessage(new SendMessageRequest("http://testQueue", "message"))).thenReturn(new SendMessageResult().withMessageId("123"));

		messageTemplate.convertAndSend("message");

	}

	@Test
	public void testConvertAndSendWithCustomDestination() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		MessageOperations messageTemplate = new SimpleQueueServiceMessageTemplate(amazonSQS);
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("custom"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://customQueue"));
		Mockito.when(amazonSQS.sendMessage(new SendMessageRequest("http://customQueue", "message"))).thenReturn(new SendMessageResult().withMessageId("123"));

		messageTemplate.convertAndSend("custom", "message");

	}

	@Test
	public void testReceiveAndConvert() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueServiceMessageTemplate messageTemplate = new SimpleQueueServiceMessageTemplate(amazonSQS);
		messageTemplate.setDefaultDestinationName("test");
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("test"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue"));
		Message message = new Message().withBody("message").withReceiptHandle("r123");
		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://testQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		String result = (String) messageTemplate.receiveAndConvert();
		Assert.assertEquals("message", result);

		Mockito.verify(amazonSQS, Mockito.times(1)).deleteMessage(new DeleteMessageRequest().withQueueUrl("http://testQueue").withReceiptHandle("r123"));
	}

	@Test
	public void testReceiveAndConvertWithCustomDestination() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueServiceMessageTemplate messageTemplate = new SimpleQueueServiceMessageTemplate(amazonSQS);
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("custom"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://customQueue"));
		Message message = new Message().withBody("message").withReceiptHandle("r123");
		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://customQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		messageTemplate.receiveAndConvert("custom");

		Mockito.verify(amazonSQS, Mockito.times(1)).deleteMessage(new DeleteMessageRequest().withQueueUrl("http://customQueue").withReceiptHandle("r123"));
	}

}
