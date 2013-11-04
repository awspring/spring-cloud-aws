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

package org.elasticspring.messaging;

import org.elasticspring.support.TestStackEnvironment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.core.GenericMessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;


/**
 *
 */
@ContextConfiguration("SendMessageTest-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class SendMessageTest {


	@Resource(name = "stringMessage")
	private GenericMessagingTemplate stringQueueingOperations;

	@Resource(name = "objectMessage")
	private GenericMessagingTemplate objectQueueingOperations;

	@Resource(name = "jsonMessage")
	private GenericMessagingTemplate jsonQueueingOperations;

	@Autowired
	private DestinationResolver<MessageChannel> destinationResolver;

	@Autowired
	private TestStackEnvironment testStackEnvironment;

	@Before
	public void reconfigureDestinationResolver() throws Exception {
		this.stringQueueingOperations.setDestinationResolver(this.destinationResolver);
		this.objectQueueingOperations.setDestinationResolver(this.destinationResolver);
		this.jsonQueueingOperations.setDestinationResolver(this.destinationResolver);
	}

	@Test
	public void testSendAndReceiveStringMessage() throws Exception {
		String messageContent = "testMessage";
		String queueName = this.testStackEnvironment.getByLogicalId("StringQueue");
		this.stringQueueingOperations.convertAndSend(queueName, messageContent);
		Thread.sleep(5000);
		String receivedMessage = this.stringQueueingOperations.receiveAndConvert(queueName,String.class);
		Assert.assertEquals(messageContent, receivedMessage);
	}

	@Test
	public void testSendAndReceiveObjectMessage() throws Exception {
		List<String> payload = Collections.singletonList("myString");
		String queueName = this.testStackEnvironment.getByLogicalId("JsonQueue");
		this.objectQueueingOperations.convertAndSend(queueName, payload);

		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) this.objectQueueingOperations.receiveAndConvert(queueName,List.class);
		Assert.assertEquals("myString", result.get(0));
	}

	@Test
	public void testSendAndReceiveJsonMessage() throws Exception {
		String queueName = this.testStackEnvironment.getByLogicalId("StreamQueue");
		this.jsonQueueingOperations.convertAndSend(queueName, "myString");

		@SuppressWarnings("unchecked")
		String result = this.jsonQueueingOperations.receiveAndConvert(queueName,String.class);
		Assert.assertEquals("myString", result);
	}
}