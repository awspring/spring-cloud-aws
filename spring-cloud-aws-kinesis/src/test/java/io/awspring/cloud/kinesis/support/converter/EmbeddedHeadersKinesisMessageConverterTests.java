/*
 * Copyright 2013-2026 the original author or authors.
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
package io.awspring.cloud.kinesis.support.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

class EmbeddedHeadersKinesisMessageConverterTests {

	private final KinesisMessagingMessageConverter delegate = new KinesisMessagingMessageConverter();

	private final EmbeddedHeadersKinesisMessageConverter converter = new EmbeddedHeadersKinesisMessageConverter(
			this.delegate);

	@Test
	@DisplayName("a record written with EmbeddedJsonHeadersMessageMapper is decoded into payload and headers")
	void decodesEmbeddedHeaders() {
		EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper();
		Message<byte[]> produced = MessageBuilder.withPayload("hello".getBytes(StandardCharsets.UTF_8))
				.setHeader("traceparent", "00-trace-span-01").setHeader("my-header", "my-value").build();
		byte[] embedded = mapper.fromMessage(produced);

		Message<byte[]> message = this.converter.toMessagingMessage(record("pk-1", "seq-1", embedded), "shard-1",
				"my-stream");

		assertThat(message.getPayload()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
		assertThat(message.getHeaders().get("traceparent")).isEqualTo("00-trace-span-01");
		assertThat(message.getHeaders().get("my-header")).isEqualTo("my-value");
	}

	@Test
	@DisplayName("Kinesis headers from the delegate win over embedded headers of the same name")
	void delegateHeadersWinOverEmbedded() {
		EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper();
		Message<byte[]> produced = MessageBuilder.withPayload("hello".getBytes(StandardCharsets.UTF_8))
				.setHeader(KinesisMessageHeaders.STREAM_NAME, "embedded-stream").build();
		byte[] embedded = mapper.fromMessage(produced);

		Message<byte[]> message = this.converter.toMessagingMessage(record("pk-1", "seq-1", embedded), "shard-1",
				"my-stream");

		assertThat(message.getHeaders().get(KinesisMessageHeaders.STREAM_NAME)).isEqualTo("my-stream");
	}

	@Test
	@DisplayName("a record without the envelope passes through unchanged")
	void plainRecordPassesThrough() {
		byte[] plain = "not an envelope".getBytes(StandardCharsets.UTF_8);

		Message<byte[]> message = this.converter.toMessagingMessage(record("pk-2", "seq-2", plain), "shard-2",
				"my-stream");

		assertThat(message.getPayload()).isEqualTo(plain);
		assertThat(message.getHeaders().get(KinesisMessageHeaders.PARTITION_KEY)).isEqualTo("pk-2");
		assertThat(message.getHeaders().get(KinesisMessageHeaders.STREAM_NAME)).isEqualTo("my-stream");
	}

	@Test
	@DisplayName("fromMessagingMessage embeds the message headers and the result round-trips through the mapper")
	void embedsHeadersOnWrite() {
		Message<String> message = MessageBuilder.withPayload("hello").setHeader("traceparent", "00-trace-span-01")
				.build();

		byte[] written = this.converter.fromMessagingMessage(message);

		Message<?> decoded = new EmbeddedJsonHeadersMessageMapper().toMessage(written, null);
		assertThat(decoded.getPayload()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
		assertThat(decoded.getHeaders().get("traceparent")).isEqualTo("00-trace-span-01");
	}

	@Test
	@DisplayName("the original id and timestamp are not embedded in the envelope")
	void doesNotEmbedIdAndTimestamp() {
		Message<String> message = MessageBuilder.withPayload("hello").setHeader("my-header", "my-value").build();
		String originalId = message.getHeaders().getId().toString();

		byte[] written = this.converter.fromMessagingMessage(message);

		String envelope = new String(written, StandardCharsets.UTF_8);
		assertThat(envelope).contains("my-header");
		assertThat(envelope).doesNotContain(originalId);
		assertThat(envelope).doesNotContain(MessageHeaders.TIMESTAMP);
	}

	@Test
	@DisplayName("a message written by the converter is read back with the same payload and headers")
	void roundTrip() {
		Message<String> message = MessageBuilder.withPayload("payload").setHeader("my-header", "my-value").build();

		byte[] written = this.converter.fromMessagingMessage(message);
		Message<byte[]> read = this.converter.toMessagingMessage(record("pk-3", "seq-3", written), "shard-3",
				"my-stream");

		assertThat(read.getPayload()).isEqualTo("payload".getBytes(StandardCharsets.UTF_8));
		assertThat(read.getHeaders().get("my-header")).isEqualTo("my-value");
	}

	private KinesisClientRecord record(String partitionKey, String sequenceNumber, byte[] body) {
		return KinesisClientRecord.builder().partitionKey(partitionKey).sequenceNumber(sequenceNumber)
				.subSequenceNumber(0L).approximateArrivalTimestamp(Instant.ofEpochMilli(1000L))
				.data(ByteBuffer.wrap(body)).build();
	}

}
