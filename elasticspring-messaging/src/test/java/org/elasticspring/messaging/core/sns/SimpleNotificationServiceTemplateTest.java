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

package org.elasticspring.messaging.core.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.Topic;
import org.elasticspring.messaging.StringMessage;
import org.elasticspring.messaging.support.converter.MessageConverter;
import org.elasticspring.messaging.support.destination.DestinationResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Agim Emruli
 * @since 1.0
 */
public class SimpleNotificationServiceTemplateTest {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testConvertAndSendWithMinimalConfiguration() throws Exception {
		AmazonSNS amazonSNS = Mockito.mock(AmazonSNS.class);

		SimpleNotificationServiceTemplate template = new SimpleNotificationServiceTemplate(amazonSNS);
		Mockito.when(amazonSNS.listTopics(new ListTopicsRequest(null))).
				thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn("arn:aws:sns:us-east-1:123456789012:test")));
		template.convertAndSend("test", "hello");
		Mockito.verify(amazonSNS, Mockito.times(1)).publish(new PublishRequest("arn:aws:sns:us-east-1:123456789012:test", "hello"));
	}

	@Test
	public void testConvertAndSendWithDefaultDestinationName() throws Exception {
		AmazonSNS amazonSNS = Mockito.mock(AmazonSNS.class);

		SimpleNotificationServiceTemplate template = new SimpleNotificationServiceTemplate(amazonSNS);
		template.setDefaultDestinationName("test");
		Mockito.when(amazonSNS.listTopics(new ListTopicsRequest(null))).
				thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn("arn:aws:sns:us-east-1:123456789012:test")));
		template.convertAndSend("hello");
		Mockito.verify(amazonSNS, Mockito.times(1)).publish(new PublishRequest("arn:aws:sns:us-east-1:123456789012:test", "hello"));
	}

	@Test
	public void testConvertAndSendWithSubjectAndDefaultDestinationName() throws Exception {
		AmazonSNS amazonSNS = Mockito.mock(AmazonSNS.class);

		SimpleNotificationServiceTemplate template = new SimpleNotificationServiceTemplate(amazonSNS);
		template.setDefaultDestinationName("test");
		Mockito.when(amazonSNS.listTopics(new ListTopicsRequest(null))).
				thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn("arn:aws:sns:us-east-1:123456789012:test")));
		template.convertAndSendWithSubject("hello", "world");
		Mockito.verify(amazonSNS, Mockito.times(1)).publish(new PublishRequest("arn:aws:sns:us-east-1:123456789012:test", "hello", "world"));
	}

	@Test
	public void testConvertAndSendWithSubject() throws Exception {
		AmazonSNS amazonSNS = Mockito.mock(AmazonSNS.class);

		SimpleNotificationServiceTemplate template = new SimpleNotificationServiceTemplate(amazonSNS);
		Mockito.when(amazonSNS.listTopics(new ListTopicsRequest(null))).
				thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn("arn:aws:sns:us-east-1:123456789012:test")));
		template.convertAndSendWithSubject("test", "hello", "world");
		Mockito.verify(amazonSNS, Mockito.times(1)).publish(new PublishRequest("arn:aws:sns:us-east-1:123456789012:test", "hello", "world"));
	}

	@Test
	public void testCustomMessageConverter() throws Exception {
		AmazonSNS amazonSNS = Mockito.mock(AmazonSNS.class);
		MessageConverter messageConverter = Mockito.mock(MessageConverter.class);
		Mockito.when(messageConverter.toMessage(Mockito.anyString())).thenAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return new StringMessage(invocation.getArguments()[0].toString().toUpperCase());
			}
		});

		Mockito.when(amazonSNS.listTopics(new ListTopicsRequest(null))).
				thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn("arn:aws:sns:us-east-1:123456789012:test")));

		SimpleNotificationServiceTemplate template = new SimpleNotificationServiceTemplate(amazonSNS);
		template.setMessageConverter(messageConverter);

		template.convertAndSend("test", "messageContent");

		Mockito.verify(amazonSNS, Mockito.times(1)).publish(new PublishRequest("arn:aws:sns:us-east-1:123456789012:test", "MESSAGECONTENT"));

	}

	@Test
	public void testCustomMessageConverterWithSubject() throws Exception {
		AmazonSNS amazonSNS = Mockito.mock(AmazonSNS.class);
		MessageConverter messageConverter = Mockito.mock(MessageConverter.class);
		Mockito.when(messageConverter.toMessage(Mockito.anyString())).thenAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return new StringMessage(invocation.getArguments()[0].toString().toUpperCase());
			}
		});

		Mockito.when(amazonSNS.listTopics(new ListTopicsRequest(null))).
				thenReturn(new ListTopicsResult().withTopics(new Topic().withTopicArn("arn:aws:sns:us-east-1:123456789012:test")));

		SimpleNotificationServiceTemplate template = new SimpleNotificationServiceTemplate(amazonSNS);
		template.setMessageConverter(messageConverter);

		template.convertAndSendWithSubject("test", "messageContent", "subject");

		Mockito.verify(amazonSNS, Mockito.times(1)).publish(new PublishRequest("arn:aws:sns:us-east-1:123456789012:test", "MESSAGECONTENT", "subject"));
	}

	@Test
	public void testCustomDestinationResolver() throws Exception {
		AmazonSNS amazonSNS = Mockito.mock(AmazonSNS.class);
		DestinationResolver destinationResolver = Mockito.mock(DestinationResolver.class);

		Mockito.when(destinationResolver.resolveDestinationName("test")).thenReturn("arn:sns:test");

		SimpleNotificationServiceTemplate template = new SimpleNotificationServiceTemplate(amazonSNS);
		template.setDestinationResolver(destinationResolver);
		template.convertAndSend("test", "message");
		Mockito.verify(amazonSNS, Mockito.times(1)).publish(new PublishRequest("arn:sns:test", "message"));
	}

	@Test
	public void testAmazonSnsIsNull() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("amazonSNS must not be null");

		//noinspection ResultOfObjectAllocationIgnored
		new SimpleNotificationServiceTemplate(null);
	}

	@Test
	public void testNoDestinationWithNoDefaultDestination() throws Exception {
		this.expectedException.expect(IllegalStateException.class);
		this.expectedException.expectMessage("No default destination name configured for this template.");

		SimpleNotificationServiceTemplate template = new SimpleNotificationServiceTemplate(Mockito.mock(AmazonSNS.class));
		template.convertAndSend("message");
	}
}