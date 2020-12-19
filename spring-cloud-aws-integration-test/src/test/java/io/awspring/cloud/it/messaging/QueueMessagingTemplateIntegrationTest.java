/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.aws.it.messaging;

import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.cloud.aws.it.AWSIntegration;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 */
@ExtendWith(SpringExtension.class)
@AWSIntegration
abstract class QueueMessagingTemplateIntegrationTest {

	private static final String JSON_QUEUE_NAME = "JsonQueue";

	private static final String STRING_QUEUE_NAME = "StringQueue";

	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Resource(name = "defaultQueueMessagingTemplate")
	private QueueMessagingTemplate defaultQueueMessagingTemplate;

	@SuppressWarnings("SpringJavaAutowiringInspection")
	@Resource(name = "queueMessagingTemplateWithCustomConverter")
	private QueueMessagingTemplate messagingTemplateWithCustomConverter;

	@Test
	void sendAndReceive_stringMessageWithProvidedDestination_shouldUseTheProvidedDestination() throws Exception {
		// Arrange
		String messageContent = "testMessage";

		// Act
		this.defaultQueueMessagingTemplate.convertAndSend(STRING_QUEUE_NAME, messageContent);
		String receivedMessage = this.defaultQueueMessagingTemplate.receiveAndConvert(STRING_QUEUE_NAME, String.class);

		// Assert
		assertThat(receivedMessage).isEqualTo(messageContent);
	}

	@Test
	void sendAndReceive_ObjectMessageWithDefaultDestination_shouldUseTheStreamQueue() throws Exception {
		// Arrange
		List<String> payload = Collections.singletonList("myString");

		// Act
		this.messagingTemplateWithCustomConverter.convertAndSend(payload);
		List<String> result = this.messagingTemplateWithCustomConverter.receiveAndConvert(StringList.class);

		// Assert
		assertThat(result.get(0)).isEqualTo("myString");
	}

	@Test
	void sendAndReceive_JsonMessageWithDefaultDestination_shouldUseTheJsonQueue() throws Exception {
		// Arrange
		DummyObject payload = new DummyObject("Hello", 100);

		// Act
		this.defaultQueueMessagingTemplate.convertAndSend(payload);
		DummyObject result = this.defaultQueueMessagingTemplate.receiveAndConvert(DummyObject.class);

		// Assert
		assertThat(result.getValue()).isEqualTo("Hello");
		assertThat(result.getAnotherValue()).isEqualTo(100);
	}

	@Test
	void convertAndSend_aStringWithJsonConverter_shouldSerializeAndDeserializeCorrectly() throws Exception {
		// Act
		this.defaultQueueMessagingTemplate.convertAndSend(JSON_QUEUE_NAME, "A String");

		// Assert
		String result = this.defaultQueueMessagingTemplate.receiveAndConvert(JSON_QUEUE_NAME, String.class);
		assertThat(result).isEqualTo("A String");
	}

	private interface StringList extends List<String> {

	}

	private static final class DummyObject {

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

}
