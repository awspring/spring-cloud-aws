/*
 * Copyright 2013-2025 the original author or authors.
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
package io.awspring.cloud.sqs.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsAsyncOperations;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * @author Artem Bilan
 *
 * @since 4.0
 */
@SpringJUnitConfig
@DirtiesContext
class SqsMessageHandlerTests extends BaseSqsIntegrationTest {

	static SqsAsyncClient AMAZON_SQS;

	@Autowired
	MessageChannel sqsSendChannel;

	@Autowired
	MessageChannel sqsSendBatchChannel;

	@Autowired
	QueueChannel outputChannel;

	@Autowired
	SqsMessageHandler sqsMessageHandler;

	@BeforeAll
	static void setup() {
		AMAZON_SQS = createAsyncClient();
	}

	@Test
	void sqsMessageHandler() {
		Message<String> message = MessageBuilder.withPayload("message1").build();

		this.sqsSendChannel.send(message);

		ReceiveMessageResponse receiveMessageResponse = AMAZON_SQS.getQueueUrl(request -> request.queueName("queue1"))
				.thenCompose(response -> AMAZON_SQS
						.receiveMessage(request -> request.queueUrl(response.queueUrl()).waitTimeSeconds(10)))
				.join();

		assertThat(receiveMessageResponse.hasMessages()).isTrue();
		assertThat(receiveMessageResponse.messages().get(0).body()).isEqualTo("message1");

		Message<String> message2 = MessageBuilder.withPayload("message2")
				.setHeader(SqsHeaders.SQS_QUEUE_NAME_HEADER, "queue2").setHeader("testHeader", "testValue").build();
		this.sqsMessageHandler.setQueueExpression(
				new FunctionExpression<Message<?>>(m -> m.getHeaders().get(SqsHeaders.SQS_QUEUE_NAME_HEADER)));

		this.sqsSendChannel.send(message2);

		receiveMessageResponse = AMAZON_SQS.getQueueUrl(request -> request.queueName("queue2"))
				.thenCompose(response -> AMAZON_SQS.receiveMessage(request -> request.queueUrl(response.queueUrl())
						.messageAttributeNames(QueueAttributeName.ALL.toString()).waitTimeSeconds(10)))
				.join();

		assertThat(receiveMessageResponse.hasMessages()).isTrue();
		software.amazon.awssdk.services.sqs.model.Message sqsMessage = receiveMessageResponse.messages().get(0);
		assertThat(sqsMessage.body()).isEqualTo("message2");

		Map<String, MessageAttributeValue> messageAttributes = sqsMessage.messageAttributes();

		assertThat(messageAttributes).doesNotContainKeys(MessageHeaders.ID, MessageHeaders.TIMESTAMP)
				.containsKey("testHeader");
		assertThat(messageAttributes.get("testHeader").stringValue()).isEqualTo("testValue");
	}

	@Test
	void sqsBatchMessageHandler() {
		GenericMessage<?> message = new GenericMessage<>(
				List.of(new GenericMessage<>("batchItem1"), new GenericMessage<>("batchItem2")));

		this.sqsSendBatchChannel.send(message);

		Message<?> receive = this.outputChannel.receive(10000);
		assertThat(receive).isNotNull().extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.type(SendResult.Batch.class))
				.extracting(SendResult.Batch::successful).asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(2);
	}

	@Configuration
	@EnableIntegration
	static class ContextConfiguration {

		@Bean
		SqsAsyncOperations sqsAsyncOperations() {
			return SqsTemplate.newTemplate(AMAZON_SQS);
		}

		@Bean
		@ServiceActivator(inputChannel = "sqsSendChannel")
		MessageHandler sqsMessageHandler(SqsAsyncOperations sqsAsyncOperations) {
			SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsyncOperations);
			sqsMessageHandler.setQueue("queue1");
			return sqsMessageHandler;
		}

		@Bean
		@ServiceActivator(inputChannel = "sqsSendBatchChannel", outputChannel = "outputChannel")
		MessageHandler sqsBatchMessageHandler(SqsAsyncOperations sqsAsyncOperations) {
			SqsMessageHandler sqsMessageHandler = new SqsMessageHandler(sqsAsyncOperations);
			sqsMessageHandler.setQueue("queue3");
			sqsMessageHandler.setAsync(true);
			return sqsMessageHandler;
		}

		@Bean
		QueueChannel outputChannel() {
			return new QueueChannel();
		}

	}

}
