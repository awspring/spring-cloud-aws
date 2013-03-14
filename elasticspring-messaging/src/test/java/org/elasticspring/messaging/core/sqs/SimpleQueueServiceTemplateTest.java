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
import org.elasticspring.messaging.StringMessage;
import org.elasticspring.messaging.core.QueueingOperations;
import org.elasticspring.messaging.support.converter.MessageConverter;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;


/**
 * @author Agim Emruli
 * @since 1.0
 */
public class SimpleQueueServiceTemplateTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testConvertAndSendWithMinimalConfiguration() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		QueueingOperations messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("custom"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://customQueue"));

		messageTemplate.convertAndSend("custom", "message");

		Mockito.verify(amazonSQS, Mockito.times(1)).sendMessage(new SendMessageRequest("http://customQueue", "message"));
	}

	@Test
	public void testConvertAndSendSingleMessageDefaultDestination() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueingServiceTemplate messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
		messageTemplate.setDefaultDestinationName("test");
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("test"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue"));

		messageTemplate.convertAndSend("message");

		Mockito.verify(amazonSQS, Mockito.times(1)).sendMessage(new SendMessageRequest("http://testQueue", "message"));
	}

	@Test
	public void testConvertAndSendWithCustomMessageConverter() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);

		SimpleQueueingServiceTemplate template = new SimpleQueueingServiceTemplate(amazonSQS);

		MessageConverter messageConverter = Mockito.mock(MessageConverter.class);
		Mockito.when(messageConverter.toMessage(Mockito.anyString())).thenAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return new StringMessage(invocation.getArguments()[0].toString().toUpperCase());
			}
		});
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("test"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://customQueue"));

		template.setMessageConverter(messageConverter);
		template.convertAndSend("test", "message");

		Mockito.verify(amazonSQS, Mockito.times(1)).sendMessage(new SendMessageRequest("http://customQueue", "MESSAGE"));
	}

	@Test
	public void testConvertAndSendWithCustomDestinationResolver() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);

		SimpleQueueingServiceTemplate template = new SimpleQueueingServiceTemplate(amazonSQS);
		DestinationResolver destinationResolver = Mockito.mock(DestinationResolver.class);

		Mockito.when(destinationResolver.resolveDestinationName("test")).thenReturn("http://testQueue");

		template.setDestinationResolver(destinationResolver);
		template.convertAndSend("test", "message");

		Mockito.verify(amazonSQS, Mockito.times(1)).sendMessage(new SendMessageRequest("http://testQueue", "message"));
	}


	@Test
	public void testReceiveAndConvertWithDefaultDestination() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueingServiceTemplate messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
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
		SimpleQueueingServiceTemplate messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("custom"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://customQueue"));
		Message message = new Message().withBody("message").withReceiptHandle("r123");
		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://customQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		String result = (String) messageTemplate.receiveAndConvert("custom");
		Assert.assertEquals("message", result);
		Mockito.verify(amazonSQS, Mockito.times(1)).deleteMessage(new DeleteMessageRequest().withQueueUrl("http://customQueue").withReceiptHandle("r123"));
	}

	@Test
	public void testReceiveAndConvertWithCustomMessageConverter() throws Exception {

		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);

		MessageConverter messageConverter = Mockito.mock(MessageConverter.class);
		Mockito.when(messageConverter.fromMessage(Mockito.<org.elasticspring.messaging.Message<String>>anyObject())).thenAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return ((org.elasticspring.messaging.Message) invocation.getArguments()[0]).getPayload().toString().toUpperCase();
			}
		});

		SimpleQueueingServiceTemplate messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
		messageTemplate.setMessageConverter(messageConverter);

		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("custom"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://customQueue"));
		Message message = new Message().withBody("message").withReceiptHandle("r123");
		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://customQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		String result = (String) messageTemplate.receiveAndConvert("custom");
		Assert.assertEquals("MESSAGE", result);

		Mockito.verify(amazonSQS, Mockito.times(1)).deleteMessage(new DeleteMessageRequest().withQueueUrl("http://customQueue").withReceiptHandle("r123"));
	}

	@Test
	public void testReceiveAndConvertWithCustomDestinationResolver() throws Exception {

		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueingServiceTemplate messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
		DestinationResolver destinationResolver = Mockito.mock(DestinationResolver.class);
		Mockito.when(destinationResolver.resolveDestinationName("test")).thenReturn("http://customQueue");

		Message message = new Message().withBody("message").withReceiptHandle("r123");

		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://customQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		messageTemplate.setDestinationResolver(destinationResolver);
		String result = (String) messageTemplate.receiveAndConvert("test");
		Assert.assertEquals("message", result);
		Mockito.verify(amazonSQS, Mockito.times(1)).deleteMessage(new DeleteMessageRequest().withQueueUrl("http://customQueue").withReceiptHandle("r123"));
	}

	@Test
	public void testReceiveAndConvertWithTypeAndDefaultDestination() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueingServiceTemplate messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
		messageTemplate.setDefaultDestinationName("test");
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("test"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://testQueue"));
		Message message = new Message().withBody("message").withReceiptHandle("r123");
		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://testQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		String result = messageTemplate.receiveAndConvert(String.class);
		Assert.assertEquals("message", result);

		Mockito.verify(amazonSQS, Mockito.times(1)).deleteMessage(new DeleteMessageRequest().withQueueUrl("http://testQueue").withReceiptHandle("r123"));
	}

	@Test
	public void testReceiveAndConvertWithTypeAndCustomDestination() throws Exception {
		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueingServiceTemplate messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("custom"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://customQueue"));
		Message message = new Message().withBody("message").withReceiptHandle("r123");
		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://customQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		String result = messageTemplate.receiveAndConvert("custom", String.class);
		Assert.assertEquals("message", result);
		Mockito.verify(amazonSQS, Mockito.times(1)).deleteMessage(new DeleteMessageRequest().withQueueUrl("http://customQueue").withReceiptHandle("r123"));
	}

	@Test
	public void testNoTypeGiven() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("expectedType must not be null");

		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueingServiceTemplate messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
		messageTemplate.setDefaultDestinationName("test");

		messageTemplate.receiveAndConvert((Class<Object>) null);
	}

	@Test
	public void testWrongTypeGiven() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("result is not of expected type:" + BigDecimal.class.getName());

		AmazonSQS amazonSQS = Mockito.mock(AmazonSQS.class);
		SimpleQueueingServiceTemplate messageTemplate = new SimpleQueueingServiceTemplate(amazonSQS);
		Mockito.when(amazonSQS.getQueueUrl(new GetQueueUrlRequest("custom"))).thenReturn(new GetQueueUrlResult().withQueueUrl("http://customQueue"));
		Message message = new Message().withBody("message").withReceiptHandle("r123");
		Mockito.when(amazonSQS.receiveMessage(new ReceiveMessageRequest("http://customQueue").withMaxNumberOfMessages(1))).thenReturn(new ReceiveMessageResult().withMessages(message));

		messageTemplate.receiveAndConvert("custom", BigDecimal.class);
	}
}
