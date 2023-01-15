package io.awspring.cloud.sqs.integration;

import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsOperations;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.operations.TemplateAcknowledgementMode;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tomaz Fernandes
 * @since 3.0
 */
@SpringBootTest
public class SqsTemplateIntegrationTests extends BaseSqsIntegrationTest {

	private static final String SENDS_AND_RECEIVES_MESSAGE_QUEUE_NAME = "send-receive-message-queue";

	private static final String SENDS_AND_RECEIVES_RECORD_QUEUE_NAME = "send-receive-record-queue";

	private static final String SENDS_AND_RECEIVES_WITH_HEADERS_QUEUE_NAME = "send-receive-with-headers-queue";

	private static final String SENDS_AND_RECEIVES_MANUAL_ACK_QUEUE_NAME = "send-receive-manual-ack-record-queue";

	private static final String SENDS_AND_RECEIVES_BATCH_QUEUE_NAME = "send-receive-batch-queue";

	private static final String SENDS_AND_RECEIVES_BATCH_FIFO_QUEUE_NAME = "send-receive-batch-fifo-queue.fifo";

	private static final String RECORD_WITHOUT_TYPE_HEADER_QUEUE_NAME = "record-without-type-header-queue";

	private static final String EMPTY_QUEUE_NAME = "empty-message-queue";

	private static final String SENDS_AND_RECEIVES_MESSAGE_FIFO_QUEUE_NAME = "send-receive-message-queue.fifo";

	@Autowired
	private SqsAsyncClient asyncClient;

	@BeforeAll
	static void beforeTests() {
		SqsAsyncClient client = createAsyncClient();
		CompletableFuture.allOf(
			createQueue(client, SENDS_AND_RECEIVES_MESSAGE_QUEUE_NAME),
			createQueue(client, SENDS_AND_RECEIVES_RECORD_QUEUE_NAME),
			createQueue(client, SENDS_AND_RECEIVES_BATCH_QUEUE_NAME),
			createQueue(client, SENDS_AND_RECEIVES_WITH_HEADERS_QUEUE_NAME),
			createQueue(client, RECORD_WITHOUT_TYPE_HEADER_QUEUE_NAME),
			createQueue(client, SENDS_AND_RECEIVES_MANUAL_ACK_QUEUE_NAME),
			createQueue(client, EMPTY_QUEUE_NAME),
			createFifoQueue(client, SENDS_AND_RECEIVES_MESSAGE_FIFO_QUEUE_NAME),
			createFifoQueue(client, SENDS_AND_RECEIVES_BATCH_FIFO_QUEUE_NAME))
			.join();
	}

	@Test
	void shouldSendAndReceiveStringMessage() {
		SqsTemplate<Object> template = SqsTemplate
			.newTemplate(this.asyncClient);
		String testBody = "Hello world!";
		SendResult<Object> result = template.send(to -> to
			.queue(SENDS_AND_RECEIVES_MESSAGE_QUEUE_NAME)
			.payload(testBody));
		assertThat(result).isNotNull();
		Optional<Message<Object>> receivedMessage = template.receive(from -> from
			.queue(SENDS_AND_RECEIVES_MESSAGE_QUEUE_NAME));
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testBody);
	}

	@Test
	void shouldSendAndReceiveRecordMessageAndAcknowledge() {
		SqsTemplate<SampleRecord> template = SqsTemplate.newTemplate(this.asyncClient);
		SampleRecord testRecord = new SampleRecord("Hello world!", "From SQS!");
		SendResult<SampleRecord> result = template.send(SENDS_AND_RECEIVES_RECORD_QUEUE_NAME, testRecord);
		assertThat(result).isNotNull();
		Optional<Message<SampleRecord>> receivedMessage = template.receive(from -> from
			.queue(SENDS_AND_RECEIVES_RECORD_QUEUE_NAME));
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testRecord);
		Optional<Message<SampleRecord>> receivedMessage2 = template.receive(from -> from
			.queue(SENDS_AND_RECEIVES_RECORD_QUEUE_NAME).pollTimeout(Duration.ofSeconds(1)));
		assertThat(receivedMessage2).isEmpty();
	}

	@Test
	void shouldSendAndReceiveMessageWithHeaders() {
		SqsTemplate<SampleRecord> template = SqsTemplate.newTemplate(this.asyncClient);
		SampleRecord testRecord = new SampleRecord("Hello world!", "From SQS!");
		String myCustomHeader = "MyCustomHeader";
		String myCustomValue = "MyCustomValue";
		String myCustomHeader2 = "MyCustomHeader2";
		String myCustomValue2 = "MyCustomValue2";
		template.send(to -> to
			.queue(SENDS_AND_RECEIVES_WITH_HEADERS_QUEUE_NAME)
			.payload(testRecord)
			.header(myCustomHeader, myCustomValue)
			.headers(Map.of(myCustomHeader2, myCustomValue2))
		);
		Optional<Message<SampleRecord>> receivedMessage = template.receive(from -> from
			.queue(SENDS_AND_RECEIVES_WITH_HEADERS_QUEUE_NAME));
		assertThat(receivedMessage).isPresent().get().extracting(Message::getHeaders)
			.asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsKeys(myCustomHeader, myCustomHeader2)
			.containsValues(myCustomValue, myCustomValue2);
	}

	@Test
	void shouldSendAndReceiveWithManualAcknowledgement() {
		SqsTemplate<SampleRecord> template = SqsTemplate.<SampleRecord>builder()
			.sqsAsyncClient(this.asyncClient)
			.configure(options -> options.acknowledgementMode(TemplateAcknowledgementMode.DO_NOT_ACKNOWLEDGE)
				.endpointName(SENDS_AND_RECEIVES_MANUAL_ACK_QUEUE_NAME))
			.build();
		SampleRecord testRecord = new SampleRecord("Hello world!", "From SQS!");
		template.send(to -> to.payload(testRecord));
		Optional<Message<SampleRecord>> receivedMessage = template.receive(from -> from
			.visibilityTimeout(Duration.ofSeconds(1)));

		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testRecord);

		Optional<Message<SampleRecord>> receivedMessage2 = template.receive(from -> from
			.visibilityTimeout(Duration.ofSeconds(1)));
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testRecord);
		Message<SampleRecord> message = receivedMessage2.get();
		Acknowledgement.acknowledge(message);
		Optional<Message<SampleRecord>> receivedMessage3 = template.receive(from -> from
			.pollTimeout(Duration.ofSeconds(1)));
		assertThat(receivedMessage3).isEmpty();
	}

	@Test
	void shouldSendAndReceiveBatch() {
		SqsTemplate<SampleRecord> template = SqsTemplate.newTemplate(this.asyncClient);
		List<Message<SampleRecord>> messagesToSend = IntStream
			.range(0, 5)
			.mapToObj(index -> new SampleRecord("Hello world - " + index, "From SQS!"))
			.map(record -> MessageBuilder.withPayload(record).build())
			.toList();
		SendResult.Batch<SampleRecord> response = template.sendMany(SENDS_AND_RECEIVES_BATCH_QUEUE_NAME, messagesToSend);
		Collection<SampleRecord> receivedMessages = template
			.receiveMany(from -> from
				.queue(SENDS_AND_RECEIVES_BATCH_QUEUE_NAME)
				.pollTimeout(Duration.ofSeconds(10))
				.maxNumberOfMessages(10))
			.stream()
			.map(Message::getPayload)
			.toList();
		assertThat(receivedMessages)
			.hasSize(5)
			.containsExactlyElementsOf(messagesToSend.stream().map(Message::getPayload).toList());
	}

	@Test
	void shouldSendAndReceiveMessageFifo() {
		String testBody = "Hello world!";
		SqsOperations<Object> template = SqsTemplate.newTemplate(this.asyncClient);
		template.sendFifo(to -> to
			.queue(SENDS_AND_RECEIVES_MESSAGE_FIFO_QUEUE_NAME)
			.payload(testBody));
		Optional<Message<Object>> receivedMessage = template
			.receiveFifo(from -> from.queue(SENDS_AND_RECEIVES_MESSAGE_FIFO_QUEUE_NAME));
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testBody);
	}

	@Test
	void shouldSendAndReceiveBatchFifo() {
		SqsTemplate<SampleRecord> template = SqsTemplate.newTemplate(this.asyncClient);
		List<Message<SampleRecord>> messagesToSend = IntStream
			.range(0, 5)
			.mapToObj(index -> new SampleRecord("Hello world - " + index, "From SQS!"))
			.map(record -> MessageBuilder.withPayload(record).build())
			.toList();
		template.sendFifo(SENDS_AND_RECEIVES_BATCH_FIFO_QUEUE_NAME, messagesToSend);
		Collection<SampleRecord> receivedMessages = template
			.receiveManyFifo(from -> from
				.queue(SENDS_AND_RECEIVES_BATCH_FIFO_QUEUE_NAME)
				.pollTimeout(Duration.ofSeconds(10))
				.maxNumberOfMessages(10))
			.stream()
			.map(Message::getPayload)
			.toList();
		assertThat(receivedMessages)
			.hasSize(5)
			.containsExactlyElementsOf(messagesToSend.stream().map(Message::getPayload).toList());
	}

	@Test
	void shouldSendAndReceiveRecordMessageWithoutPayloadInfoHeader() {
		SqsTemplate<SampleRecord> template = SqsTemplate
			.<SampleRecord>builder()
			.sqsAsyncClient(this.asyncClient)
			.defaultMessageConverter(converter -> converter.setPayloadTypeHeaderValueFunction(msg -> null))
			.build();
		SampleRecord testRecord = new SampleRecord("Hello world!", "From SQS!");
		SendResult<SampleRecord> result = template.send(RECORD_WITHOUT_TYPE_HEADER_QUEUE_NAME, testRecord);
		assertThat(result).isNotNull();
		Optional<Message<SampleRecord>> receivedMessage = template.receive(from -> from
			.queue(RECORD_WITHOUT_TYPE_HEADER_QUEUE_NAME)
			.payloadClass(SampleRecord.class));
		assertThat(receivedMessage).isPresent().get().extracting(Message::getPayload).isEqualTo(testRecord);
	}

	@Test
	void shouldReceiveEmptyMessage() {
		Optional<Message<Object>> receivedMessage = SqsTemplate
			.newTemplate(this.asyncClient)
			.receive(from -> from.queue(EMPTY_QUEUE_NAME).pollTimeout(Duration.ofSeconds(1)));
		assertThat(receivedMessage).isEmpty();
	}

	private record SampleRecord(String propertyOne, String propertyTwo) {}

	@Configuration
	static class SQSConfiguration {

		@Bean
		SqsAsyncClient client() {
			return createAsyncClient();
		}
	}

}
