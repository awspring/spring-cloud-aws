/*
 * Copyright 2013-2024 the original author or authors.
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
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.operations.*;
import io.awspring.cloud.sqs.support.converter.AbstractMessagingMessageConverter;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StopWatch;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * @author Tomaz Fernandes
 * @author Dongha Kim
 */
@SpringBootTest
public class SqsTemplateIntegrationTests extends BaseSqsIntegrationTest {

	private static final String SENDS_AND_RECEIVES_MESSAGE_QUEUE_NAME = "send-receive-message-queue";

	private static final String SENDS_AND_RECEIVES_RECORD_QUEUE_NAME = "send-receive-record-queue";

	private static final String SENDS_AND_RECEIVES_RECORD_WITH_DELAY_QUEUE_NAME = "send-receive-record-delay-queue";

	private static final String SENDS_AND_RECEIVES_WITH_HEADERS_QUEUE_NAME = "send-receive-with-headers-queue";

	private static final String SENDS_AND_RECEIVES_MANUAL_ACK_QUEUE_NAME = "send-receive-manual-ack-record-queue";

	private static final String SENDS_AND_RECEIVES_BATCH_QUEUE_NAME = "send-receive-batch-queue";

	private static final String RETURNS_ON_PARTIAL_BATCH_QUEUE_NAME = "returns-on-partial-batch-queue";

	private static final String THROWS_ON_PARTIAL_BATCH_QUEUE_NAME = "returns-on-partial-batch-queue";

	private static final String SENDS_AND_RECEIVES_BATCH_FIFO_QUEUE_NAME = "send-receive-batch-fifo-queue.fifo";

	private static final String RECORD_WITHOUT_TYPE_HEADER_QUEUE_NAME = "record-without-type-header-queue";

	private static final String EMPTY_QUEUE_NAME = "empty-message-queue";

	private static final String SENDS_AND_RECEIVES_MESSAGE_FIFO_QUEUE_NAME = "send-receive-message-queue.fifo";

	private static final String HANDLES_CONTENT_DEDUPLICATION_QUEUE_NAME = "handles-content-deduplication-queue.fifo";

	private static final String SENDS_AND_RECEIVES_JSON_MESSAGE_QUEUE_NAME = "send-receive-json-message-queue";

	@Autowired
	private SqsAsyncClient asyncClient;

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(createQueue(client, SENDS_AND_RECEIVES_MESSAGE_QUEUE_NAME),
				createQueue(client, SENDS_AND_RECEIVES_RECORD_QUEUE_NAME),
				createQueue(client, SENDS_AND_RECEIVES_RECORD_WITH_DELAY_QUEUE_NAME),
				createQueue(client, SENDS_AND_RECEIVES_BATCH_QUEUE_NAME),
				createQueue(client, SENDS_AND_RECEIVES_WITH_HEADERS_QUEUE_NAME),
				createQueue(client, RECORD_WITHOUT_TYPE_HEADER_QUEUE_NAME),
				createQueue(client, RETURNS_ON_PARTIAL_BATCH_QUEUE_NAME),
				createQueue(client, THROWS_ON_PARTIAL_BATCH_QUEUE_NAME),
				createQueue(client, SENDS_AND_RECEIVES_JSON_MESSAGE_QUEUE_NAME),
				createQueue(client, SENDS_AND_RECEIVES_MANUAL_ACK_QUEUE_NAME), createQueue(client, EMPTY_QUEUE_NAME),
				createFifoQueue(client, SENDS_AND_RECEIVES_MESSAGE_FIFO_QUEUE_NAME),
				createFifoQueue(client, SENDS_AND_RECEIVES_BATCH_FIFO_QUEUE_NAME),
				createFifoQueue(client, HANDLES_CONTENT_DEDUPLICATION_QUEUE_NAME,
						Map.of(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "true")))
				.join();
	}

	@Test
	void shouldSendAndReceiveStringMessage() {
		SqsTemplate template = SqsTemplate.newTemplate(this.asyncClient);
		String testBody = "Hello world!";
		SendResult<Object> result = template
				.send(to -> to.queue(SENDS_AND_RECEIVES_MESSAGE_QUEUE_NAME).payload(testBody));
		assertThat(result).isNotNull();
		Optional<Message<?>> receivedMessage = template
				.receive(from -> from.queue(SENDS_AND_RECEIVES_MESSAGE_QUEUE_NAME));
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testBody);
	}

	@Test
	void shouldSendAndReceiveRecordMessageAndAcknowledge() {
		SqsTemplate template = SqsTemplate.newTemplate(this.asyncClient);
		SampleRecord testRecord = new SampleRecord("Hello world!",
				"From shouldSendAndReceiveRecordMessageAndAcknowledge!");
		SendResult<SampleRecord> result = template.send(SENDS_AND_RECEIVES_RECORD_QUEUE_NAME, testRecord);
		assertThat(result).isNotNull();
		Optional<Message<SampleRecord>> receivedMessage = template
				.receive(from -> from.queue(SENDS_AND_RECEIVES_RECORD_QUEUE_NAME), SampleRecord.class);
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testRecord);
		Optional<Message<SampleRecord>> receivedMessage2 = template.receive(
				from -> from.queue(SENDS_AND_RECEIVES_RECORD_QUEUE_NAME).pollTimeout(Duration.ofSeconds(1)),
				SampleRecord.class);
		assertThat(receivedMessage2).isEmpty();
	}

	@Test
	void shouldSendMessageWithDelay() {
		SqsOperations template = SqsTemplate.newSyncTemplate(this.asyncClient);
		SampleRecord testRecord = new SampleRecord("Hello world!", "From shouldSendMessageWithDelay!");
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		int delaySeconds = 1;
		SendResult<SampleRecord> result = template.send(to -> to.queue(SENDS_AND_RECEIVES_RECORD_WITH_DELAY_QUEUE_NAME)
				.delaySeconds(delaySeconds).payload(testRecord));
		Optional<Message<SampleRecord>> receivedMessage = template
				.receive(from -> from.queue(SENDS_AND_RECEIVES_RECORD_WITH_DELAY_QUEUE_NAME), SampleRecord.class);
		stopWatch.stop();
		assertThat(stopWatch.getTotalTimeSeconds()).isGreaterThanOrEqualTo(1.0);
		assertThat(result.message().getHeaders().get(SqsHeaders.SQS_DELAY_HEADER)).isEqualTo(delaySeconds);
	}

	@Test
	void shouldSendAndReceiveMessageWithHeaders() {
		SqsOperations template = SqsTemplate.newTemplate(this.asyncClient);
		SampleRecord testRecord = new SampleRecord("Hello world!", "From shouldSendAndReceiveMessageWithHeaders!");
		String myCustomHeader = "MyCustomHeader";
		String myCustomValue = "MyCustomValue";
		String myCustomHeader2 = "MyCustomHeader2";
		String myCustomValue2 = "MyCustomValue2";
		String myCustomHeader3 = "MyCustomHeader";
		String myCustomValue3 = "MyCustomValue";
		template.send(to -> to.queue(SENDS_AND_RECEIVES_WITH_HEADERS_QUEUE_NAME).payload(testRecord)
				.header(myCustomHeader, myCustomValue).headers(Map.of(myCustomHeader2, myCustomValue2)));
		Optional<Message<SampleRecord>> receivedMessage = template
				.receive(from -> from.queue(SENDS_AND_RECEIVES_WITH_HEADERS_QUEUE_NAME)
						.additionalHeaders(Map.of(myCustomHeader3, myCustomValue3)), SampleRecord.class);
		assertThat(receivedMessage).isPresent().get().extracting(Message::getHeaders)
				.asInstanceOf(InstanceOfAssertFactories.MAP)
				.containsKeys(myCustomHeader, myCustomHeader2, myCustomHeader3)
				.containsValues(myCustomValue, myCustomValue2, myCustomValue3);
	}

	@Test
	void shouldSendAndReceiveWithManualAcknowledgement() {
		SqsTemplate template = SqsTemplate.builder().sqsAsyncClient(this.asyncClient)
				.configure(options -> options.acknowledgementMode(TemplateAcknowledgementMode.MANUAL)
						.defaultQueue(SENDS_AND_RECEIVES_MANUAL_ACK_QUEUE_NAME))
				.build();
		SampleRecord testRecord = new SampleRecord("Hello world!",
				"From shouldSendAndReceiveWithManualAcknowledgement!");
		template.send(to -> to.payload(testRecord));
		Optional<Message<SampleRecord>> receivedMessage = template
				.receive(from -> from.visibilityTimeout(Duration.ofSeconds(1)), SampleRecord.class);
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testRecord);
		Optional<Message<SampleRecord>> receivedMessage2 = template
				.receive(from -> from.visibilityTimeout(Duration.ofSeconds(1)), SampleRecord.class);
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testRecord);
		Message<SampleRecord> message = receivedMessage2.get();
		Acknowledgement.acknowledge(message);
		Optional<Message<SampleRecord>> receivedMessage3 = template
				.receive(from -> from.pollTimeout(Duration.ofSeconds(1)), SampleRecord.class);
		assertThat(receivedMessage3).isEmpty();
	}

	@Test
	void shouldSendAndReceiveJsonString() {
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(this.asyncClient)
				.configureDefaultConverter(AbstractMessagingMessageConverter::doNotSendPayloadTypeHeader)
				.buildSyncTemplate();
		String jsonString = """
				{
					"propertyOne": "hello",
					"propertyTwo": "sqs!"
				}
				""";
		SampleRecord expectedPayload = new SampleRecord("hello", "sqs!");
		SendResult<Object> result = template.send(to -> to.queue(SENDS_AND_RECEIVES_JSON_MESSAGE_QUEUE_NAME)
				.payload(jsonString).header(MessageHeaders.CONTENT_TYPE, "application/json"));
		assertThat(result).isNotNull();
		Optional<Message<SampleRecord>> receivedMessage = template
				.receive(from -> from.queue(SENDS_AND_RECEIVES_JSON_MESSAGE_QUEUE_NAME), SampleRecord.class);
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(expectedPayload);
	}

	@Test
	void shouldSendAndReceiveBatch() {
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(this.asyncClient)
				.configure(options -> options.acknowledgementMode(TemplateAcknowledgementMode.MANUAL))
				.buildSyncTemplate();
		List<Message<SampleRecord>> messagesToSend = IntStream.range(0, 5)
				.mapToObj(index -> new SampleRecord("Hello world - " + index, "From shouldSendAndReceiveBatch!"))
				.map(record -> MessageBuilder.withPayload(record).build()).toList();
		SendResult.Batch<SampleRecord> response = template.sendMany(SENDS_AND_RECEIVES_BATCH_QUEUE_NAME,
				messagesToSend);
		Collection<Message<SampleRecord>> receivedMessages = template
				.receiveMany(from -> from.queue(SENDS_AND_RECEIVES_BATCH_QUEUE_NAME).pollTimeout(Duration.ofSeconds(10))
						.maxNumberOfMessages(10).visibilityTimeout(Duration.ofSeconds(1)), SampleRecord.class);
		assertThat(receivedMessages.stream().map(Message::getPayload).toList()).hasSize(5)
				.containsExactlyElementsOf(messagesToSend.stream().map(Message::getPayload).toList());
		Acknowledgement.acknowledge(receivedMessages);
		Collection<Message<SampleRecord>> noMessages = template.receiveMany(
				from -> from.queue(SENDS_AND_RECEIVES_BATCH_QUEUE_NAME).pollTimeout(Duration.ofSeconds(2)),
				SampleRecord.class);
		assertThat(noMessages).isEmpty();
	}

	@Test
	void shouldSendAndReceiveMessageFifo() {
		String testBody = "Hello world!";
		SqsOperations template = SqsTemplate.newTemplate(this.asyncClient);
		SendResult<Object> result = template
				.send(to -> to.queue(SENDS_AND_RECEIVES_MESSAGE_FIFO_QUEUE_NAME).payload(testBody));
		Optional<Message<?>> receivedMessage = template
				.receive(from -> from.queue(SENDS_AND_RECEIVES_MESSAGE_FIFO_QUEUE_NAME));
		assertThat(receivedMessage).isPresent().get().isInstanceOfSatisfying(Message.class, message -> {
			assertThat(message.getPayload()).isEqualTo(testBody);
			assertThat(result.additionalInformation().get(SqsTemplateParameters.SEQUENCE_NUMBER_PARAMETER_NAME))
					.isEqualTo(message.getHeaders().get(SqsHeaders.MessageSystemAttributes.SQS_SEQUENCE_NUMBER));
			assertThat(message.getHeaders().get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER))
					.isEqualTo(result.message().getHeaders()
							.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER));
			assertThat(message.getHeaders().get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER))
					.isEqualTo(result.message().getHeaders()
							.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER));
			assertThat(result.messageId()).isEqualTo(message.getHeaders().getId());
		});

	}

	@Test
	void shouldHandleContentBasedDeduplication() {
		String testBody = "Hello world!";
		SqsOperations template = SqsTemplate.builder().sqsAsyncClient(this.asyncClient).build();
		SendResult<Object> result = template
				.send(to -> to.queue(HANDLES_CONTENT_DEDUPLICATION_QUEUE_NAME).payload(testBody));
		Optional<Message<?>> receivedMessage = template
				.receive(from -> from.queue(HANDLES_CONTENT_DEDUPLICATION_QUEUE_NAME));
		assertThat(result.message().getHeaders()
				.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER)).isNull();
		assertThat(receivedMessage).isPresent().get().isInstanceOfSatisfying(Message.class, message -> {
			assertThat(message.getPayload()).isEqualTo(testBody);
			assertThat(result.additionalInformation().get(SqsTemplateParameters.SEQUENCE_NUMBER_PARAMETER_NAME))
					.isEqualTo(message.getHeaders().get(SqsHeaders.MessageSystemAttributes.SQS_SEQUENCE_NUMBER));
			assertThat(message.getHeaders().get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER))
					.isEqualTo(result.message().getHeaders()
							.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER));
			assertThat(message.getHeaders().get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER))
					.isNotNull();
			assertThat(result.messageId()).isEqualTo(message.getHeaders().getId());
		});
	}

	@Test
	void shouldSendAndReceiveBatchFifo() {
		int batchSize = 5;
		SqsTemplate template = SqsTemplate.newTemplate(this.asyncClient);
		List<Message<SampleRecord>> messagesToSend = IntStream.range(0, batchSize)
				.mapToObj(index -> new SampleRecord("Hello world - " + index, "From shouldSendAndReceiveBatchFifo!"))
				.map(record -> MessageBuilder.withPayload(record).build()).toList();
		SendResult.Batch<SampleRecord> batchSendResult = template.sendMany(SENDS_AND_RECEIVES_BATCH_FIFO_QUEUE_NAME,
				messagesToSend);
		List<SendResult<SampleRecord>> successful = new ArrayList<>(batchSendResult.successful());
		assertThat(batchSendResult.failed()).isEmpty();
		assertThat(successful).hasSize(batchSize);
		IntStream.range(0, batchSize).forEach(index -> {
			SendResult<SampleRecord> result = successful.get(index);
			Message<SampleRecord> originalMessage = messagesToSend.get(index);
			assertThat(result.endpoint()).isEqualTo(SENDS_AND_RECEIVES_BATCH_FIFO_QUEUE_NAME);
			assertThat(result.message().getPayload()).isEqualTo(originalMessage.getPayload());
			assertThat(result.message().getHeaders().getId()).isEqualTo(originalMessage.getHeaders().getId());
			assertThat(result.message().getHeaders()).containsKeys(
					SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER,
					SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER);
			assertThat(result.additionalInformation().get(SqsTemplateParameters.SEQUENCE_NUMBER_PARAMETER_NAME))
					.isNotNull();
		});
		List<Message<SampleRecord>> receivedMessages = new ArrayList<>(
				template.receiveMany(from -> from.queue(SENDS_AND_RECEIVES_BATCH_FIFO_QUEUE_NAME)
						.pollTimeout(Duration.ofSeconds(10)).maxNumberOfMessages(10), SampleRecord.class));
		IntStream.range(0, batchSize).forEach(index -> {
			SendResult<SampleRecord> result = successful.get(index);
			Message<SampleRecord> originalMessage = messagesToSend.get(index);
			Message<SampleRecord> receivedMessage = receivedMessages.get(index);
			assertThat(receivedMessage.getPayload()).isEqualTo(originalMessage.getPayload());
			assertThat(receivedMessage.getHeaders().getId()).isEqualTo(result.messageId());
			assertThat(
					result.message().getHeaders().get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER))
					.isEqualTo(receivedMessage.getHeaders()
							.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER));
			assertThat(result.message().getHeaders()
					.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER))
					.isEqualTo(receivedMessage.getHeaders()
							.get(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER));
			assertThat(result.additionalInformation().get(SqsTemplateParameters.SEQUENCE_NUMBER_PARAMETER_NAME))
					.isEqualTo(
							receivedMessage.getHeaders().get(SqsHeaders.MessageSystemAttributes.SQS_SEQUENCE_NUMBER));
		});

	}

	@Test
	void shouldSendAndReceiveRecordMessageWithoutPayloadInfoHeader() {
		SqsTemplate template = SqsTemplate.builder().sqsAsyncClient(this.asyncClient)
				.configureDefaultConverter(AbstractMessagingMessageConverter::doNotSendPayloadTypeHeader).build();
		SampleRecord testRecord = new SampleRecord("Hello world!",
				"From shouldSendAndReceiveRecordMessageWithoutPayloadInfoHeader!");
		SendResult<SampleRecord> result = template.send(RECORD_WITHOUT_TYPE_HEADER_QUEUE_NAME, testRecord);
		assertThat(result).isNotNull();
		Optional<Message<SampleRecord>> receivedMessage = template
				.receive(from -> from.queue(RECORD_WITHOUT_TYPE_HEADER_QUEUE_NAME), SampleRecord.class);
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testRecord);
	}

	@Test
	void shouldReceiveEmptyMessage() {
		Optional<Message<?>> receivedMessage = SqsTemplate.newTemplate(this.asyncClient)
				.receive(from -> from.queue(EMPTY_QUEUE_NAME).pollTimeout(Duration.ofSeconds(1)));
		assertThat(receivedMessage).isEmpty();
	}

	private record SampleRecord(String propertyOne, String propertyTwo) {
	}

	@Configuration
	static class SQSConfiguration {

		@Bean
		SqsAsyncClient client() {
			return createAsyncClient();
		}
	}

}
