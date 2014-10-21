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

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * @author Alain Sahli
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class QueueMessagingTemplateIntegrationTest {

	public static final String JSON_QUEUE_NAME = "JsonQueue";
	public static final String STREAM_QUEUE_NAME = "StreamQueue";
	public static final String STRING_QUEUE_NAME = "StringQueue";

	@Resource(name = "stringMessage")
	private QueueMessagingTemplate stringQueueingOperations;

	@Resource(name = "objectMessage")
	private QueueMessagingTemplate objectQueueingOperations;

	@Resource(name = "jsonMessage")
	private QueueMessagingTemplate jsonQueueingOperations;

	@Test
	public void testSendAndReceiveStringMessage() throws Exception {
		String messageContent = "testMessage";
		this.stringQueueingOperations.convertAndSend(STRING_QUEUE_NAME, messageContent);
		String receivedMessage = this.stringQueueingOperations.receiveAndConvert(STRING_QUEUE_NAME, String.class);
		assertEquals(messageContent, receivedMessage);
	}

	@Test
	public void testSendAndReceiveObjectMessage() throws Exception {
		List<String> payload = Collections.singletonList("myString");
		this.objectQueueingOperations.convertAndSend(STREAM_QUEUE_NAME, payload);

		List<String> result = this.objectQueueingOperations.receiveAndConvert(STREAM_QUEUE_NAME, StringList.class);
		assertEquals("myString", result.get(0));
	}

	@Test
	public void testSendAndReceiveJsonMessage() throws Exception {
		DummyObject payload = new DummyObject("Hello", 100);
		this.jsonQueueingOperations.convertAndSend(JSON_QUEUE_NAME, payload);

		DummyObject result = this.jsonQueueingOperations.receiveAndConvert(JSON_QUEUE_NAME, DummyObject.class);
		assertEquals("Hello", result.getValue());
		assertEquals(100, result.getAnotherValue());
	}

	@Test
	public void convertAndSend_aStringWithJsonConverter_shouldSerializeAndDeserializeCorrectly() throws Exception {
		// Act
		this.jsonQueueingOperations.convertAndSend(JSON_QUEUE_NAME, "A String");

		// Assert
		String result = this.jsonQueueingOperations.receiveAndConvert(JSON_QUEUE_NAME, String.class);
		assertEquals("A String", result);
	}

	private static class DummyObject {
		private final String value;
		private final int anotherValue;

		private DummyObject(@JsonProperty("value") String value, @JsonProperty("anotherValue") int anotherValue) {
			this.value = value;
			this.anotherValue = anotherValue;
		}

		public int getAnotherValue() {
			return this.anotherValue;
		}

		public String getValue() {
			return this.value;
		}
	}

	interface StringList extends List<String> {

	}

}
