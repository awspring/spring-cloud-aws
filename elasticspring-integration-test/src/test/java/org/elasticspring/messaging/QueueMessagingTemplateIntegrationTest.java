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

package org.elasticspring.messaging;

import org.elasticspring.messaging.core.QueueMessagingTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Agim Emruli
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class QueueMessagingTemplateIntegrationTest {

	@Resource(name = "stringMessage")
	private QueueMessagingTemplate stringQueueingOperations;

	@Resource(name = "objectMessage")
	private QueueMessagingTemplate objectQueueingOperations;

	@Resource(name = "jsonMessage")
	private QueueMessagingTemplate jsonQueueingOperations;

	@Test
	public void testSendAndReceiveStringMessage() throws Exception {
		String messageContent = "testMessage";
		this.stringQueueingOperations.convertAndSend("StringQueue", messageContent);
		String receivedMessage = this.stringQueueingOperations.receiveAndConvert("StringQueue", String.class);
		assertEquals(messageContent, receivedMessage);
	}

	@Test
	public void testSendAndReceiveObjectMessage() throws Exception {
		List<String> payload = Collections.singletonList("myString");
		this.objectQueueingOperations.convertAndSend("StreamQueue", payload);

		List<String> result = this.objectQueueingOperations.receiveAndConvert("StreamQueue", StringList.class);
		assertEquals("myString", result.get(0));
	}

	@Test
	public void testSendAndReceiveJsonMessage() throws Exception {
		this.jsonQueueingOperations.convertAndSend("JsonQueue", "myString");

		String result = this.jsonQueueingOperations.receiveAndConvert("JsonQueue", String.class);
		assertEquals("myString", result);
	}

	interface StringList extends List<String> {

	}

}
